package ru.yandex.practicum.processor;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.broker.AnalyzerTopics;
import ru.yandex.practicum.config.SnapshotConsumerConfig;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.mapper.ActionMapper;
import ru.yandex.practicum.mapper.ConditionMapper;
import ru.yandex.practicum.model.Action;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import telemetry.service.collector.ActionTypeProto;        // Из hub_event.proto
import telemetry.service.collector.DeviceActionProto;      // Из hub_event.proto
import telemetry.service.hubrouter.DeviceActionRequest;    // Из device_action.proto
import telemetry.service.hubrouter.HubRouterControllerGrpc; // Из hub_router_controller.proto

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
public class SnapshotProcessor {
    private final SnapshotConsumerConfig consumerConfig;
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionMapper conditionMapper;
    private final ActionMapper actionMapper;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    private KafkaConsumer<String, SensorsSnapshotAvro> consumer;

    public SnapshotProcessor(
            SnapshotConsumerConfig consumerConfig,
            ScenarioRepository scenarioRepository,
            SensorRepository sensorRepository,
            ConditionMapper conditionMapper,
            ActionMapper actionMapper,
            @GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.consumerConfig = consumerConfig;
        this.scenarioRepository = scenarioRepository;
        this.sensorRepository = sensorRepository;
        this.conditionMapper = conditionMapper;
        this.actionMapper = actionMapper;
        this.hubRouterClient = hubRouterClient;
    }

    public void start() {
        log.info("Запуск SnapshotProcessor");

        try {
            Properties properties = new Properties();
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    consumerConfig.getBootstrapServers());
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getSnapshotConsumer().getKeyDeserializer());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getSnapshotConsumer().getValueDeserializer());
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    consumerConfig.getSnapshotConsumer().getAutoOffsetReset());
            properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                    consumerConfig.getSnapshotConsumer().getGroupId());
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

            this.consumer = new KafkaConsumer<>(properties);
            log.info("Kafka Consumer создан с настройками: bootstrap-servers={}, group-id={}",
                    consumerConfig.getBootstrapServers(),
                    consumerConfig.getSnapshotConsumer().getGroupId());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Получен сигнал завершения работы, пробуждаем consumer");
                consumer.wakeup();
            }));

            consumer.subscribe(List.of(AnalyzerTopics.TELEMETRY_SNAPSHOTS_V1));
            log.info("Подписка на топик: {}", AnalyzerTopics.TELEMETRY_SNAPSHOTS_V1);

            while (true) {
                try {
                    ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));

                    if (!records.isEmpty()) {
                        log.info("Получено {} снимков состояния датчиков", records.count());
                    }

                    for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                        try {
                            SensorsSnapshotAvro snapshot = record.value();
                            log.info("Обработка снимка для хаба: {}, количество датчиков: {}",
                                    snapshot.getHubId(),
                                    snapshot.getSensorsState() != null ? snapshot.getSensorsState().size() : 0);

                            processSnapshot(snapshot);

                            consumer.commitSync();
                            log.debug("Коммит offset для записи: partition={}, offset={}",
                                    record.partition(), record.offset());
                        } catch (Exception e) {
                            log.error("Ошибка при обработке записи: partition={}, offset={}",
                                    record.partition(), record.offset(), e);
                        }
                    }
                } catch (WakeupException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Ошибка при poll записей", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

        } catch (WakeupException ignored) {
            log.info("Получен сигнал остановки, завершаем работу SnapshotProcessor");
        } catch (Exception e) {
            log.error("Критическая ошибка в SnapshotProcessor", e);
        } finally {
            try {
                if (consumer != null) {
                    log.info("Фиксация смещений перед закрытием consumer");
                    consumer.commitSync();
                }
            } catch (Exception e) {
                log.error("Ошибка при фиксации смещений", e);
            } finally {
                if (consumer != null) {
                    log.info("Закрытие Kafka consumer");
                    consumer.close();
                }
            }
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        if (sensorsState == null || sensorsState.isEmpty()) {
            log.debug("Снимок для хаба {} не содержит данных датчиков", hubId);
            return;
        }

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        if (scenarios.isEmpty()) {
            log.debug("Для хаба {} не найдено сценариев", hubId);
            return;
        }

        log.debug("Для хаба {} найдено {} сценариев", hubId, scenarios.size());

        scenarios.stream()
                .filter(scenario -> !scenario.getConditions().isEmpty())
                .filter(scenario -> {
                    boolean matches = matchConditions(scenario.getConditions(), sensorsState);
                    if (matches) {
                        log.info("Все условия выполнены для сценария: {} в хабе: {}",
                                scenario.getName(), hubId);
                    }
                    return matches;
                })
                .forEach(scenario -> executeActions(snapshot, scenario));
    }

    private void executeActions(SensorsSnapshotAvro snapshot, Scenario scenario) {
        String hubId = snapshot.getHubId();
        Map<String, Action> actions = scenario.getActions();

        if (actions == null || actions.isEmpty()) {
            log.debug("Сценарий {} в хабе {} не содержит действий", scenario.getName(), hubId);
            return;
        }

        log.info("Выполнение {} действий для сценария: {} в хабе: {}",
                actions.size(), scenario.getName(), hubId);

        actions.forEach((sensorId, action) -> {
            try {
                log.debug("Выполняется действие для датчика {}: тип={}, значение={}",
                        sensorId, action.getType(), action.getValue());

                DeviceActionProto actionProto = DeviceActionProto.newBuilder()
                        .setSensorId(sensorId)
                        .setType(ActionTypeProto.valueOf(action.getType().toUpperCase()))
                        .setValue(action.getValue() != null ? action.getValue() : 0)
                        .build();

                Instant instant = Instant.now();
                Timestamp timestampProto = Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano())
                        .build();

                DeviceActionRequest grpcRequest = DeviceActionRequest.newBuilder()
                        .setHubId(hubId)
                        .setScenarioName(scenario.getName())
                        .setAction(actionProto)
                        .setTimestamp(timestampProto)
                        .build();

                hubRouterClient.handleDeviceAction(grpcRequest);
                log.info("gRPC команда успешно отправлена для датчика {} в хабе {}", sensorId, hubId);

            } catch (Exception e) {
                log.error("Не удалось отправить gRPC команду для датчика {} в хабе {}",
                        sensorId, hubId, e);
            }
        });
    }

    private boolean matchConditions(Map<String, Condition> scenarioConditions,
                                    Map<String, SensorStateAvro> sensorsState) {
        if (scenarioConditions == null || scenarioConditions.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Condition> entry : scenarioConditions.entrySet()) {
            String sensorId = entry.getKey();
            Condition condition = entry.getValue();

            SensorStateAvro sensorState = sensorsState.get(sensorId);
            if (sensorState == null || sensorState.getData() == null) {
                log.debug("Нет данных для датчика {} или данные отсутствуют", sensorId);
                return false;
            }

            try {
                ConditionTypeAvro conditionType = ConditionTypeAvro
                        .valueOf(condition.getType().toUpperCase());
                ConditionOperationAvro operation = ConditionOperationAvro
                        .valueOf(condition.getOperation().toUpperCase());

                Object actualValue = getSensorValue(sensorState.getData(), conditionType);
                if (actualValue == null) {
                    log.debug("Не удалось получить значение для датчика {} типа {}",
                            sensorId, conditionType);
                    return false;
                }

                boolean matches = checkOperation(actualValue, operation, condition.getValue());
                if (!matches) {
                    log.debug("Условие не выполнено для датчика {}: ожидалось {} {} {}",
                            sensorId, condition.getValue(), operation, actualValue);
                }
                return matches;

            } catch (IllegalArgumentException e) {
                log.error("Ошибка маппинга Condition из БД в Avro Enum. Type: {}, Operation: {}",
                        condition.getType(), condition.getOperation(), e);
                return false;
            }
        }
        return true;
    }

    private Object getSensorValue(Object avroUnionData, ConditionTypeAvro conditionType) {
        if (avroUnionData == null) {
            return null;
        }

        return switch (conditionType) {
            case TEMPERATURE -> {
                if (avroUnionData instanceof TemperatureSensorAvro t) {
                    yield t.getTemperatureC();
                }
                if (avroUnionData instanceof ClimateSensorAvro c) {
                    yield c.getTemperatureC();
                }
                yield null;
            }
            case HUMIDITY -> avroUnionData instanceof ClimateSensorAvro c ? c.getHumidity() : null;
            case CO2LEVEL -> avroUnionData instanceof ClimateSensorAvro c ? c.getCo2Level() : null;
            case LUMINOSITY -> avroUnionData instanceof LightSensorAvro l ? l.getLuminosity() : null;
            case MOTION -> avroUnionData instanceof MotionSensorAvro m ? m.getMotion() : null;
            case SWITCH -> avroUnionData instanceof SwitchSensorAvro s ? s.getState() : null;
        };
    }

    private boolean checkOperation(Object actual, ConditionOperationAvro operation, Integer expectedValue) {
        if (actual instanceof Boolean actualBool) {
            if (operation != ConditionOperationAvro.EQUALS) {
                log.warn("Для булевых условий поддерживается только операция EQUALS. Получено: {}", operation);
                return false;
            }
            boolean expectedBool = expectedValue != null && expectedValue != 0;
            return actualBool == expectedBool;
        }

        if (actual instanceof Integer actualInt) {
            if (expectedValue == null) {
                log.debug("Ожидаемое значение равно null");
                return false;
            }
            return switch (operation) {
                case EQUALS -> actualInt.equals(expectedValue);
                case GREATER_THAN -> actualInt > expectedValue;
                case LOWER_THAN -> actualInt < expectedValue;
            };
        }

        log.warn("Неизвестный тип данных для сравнения: {}", actual.getClass().getSimpleName());
        return false;
    }
}