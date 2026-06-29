package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.broker.AnalyzerTopics;
import ru.yandex.practicum.config.HubEventConsumerConfig;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.mapper.ActionMapper;
import ru.yandex.practicum.mapper.ConditionMapper;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.model.Sensor;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {
    private final HubEventConsumerConfig consumerConfig;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionMapper conditionMapper;
    private final ActionMapper actionMapper;

    private KafkaConsumer<String, HubEventAvro> consumer;

    @Override
    public void run() {
        log.info("Запуск HubEventProcessor");

        try {
            Properties properties = new Properties();
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    consumerConfig.getBootstrapServers());
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getHubEventConsumer().getKeyDeserializer());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    consumerConfig.getHubEventConsumer().getValueDeserializer());
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    consumerConfig.getHubEventConsumer().getAutoOffsetReset());
            properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                    consumerConfig.getHubEventConsumer().getGroupId());

            // Дополнительные настройки для надежности
            properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

            this.consumer = new KafkaConsumer<>(properties);
            log.info("Kafka Consumer создан с настройками: bootstrap-servers={}, group-id={}",
                    consumerConfig.getBootstrapServers(),
                    consumerConfig.getHubEventConsumer().getGroupId());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Получен сигнал завершения работы, пробуждаем consumer");
                consumer.wakeup();
            }));

            consumer.subscribe(List.of(AnalyzerTopics.TELEMETRY_HUBS_V1));
            log.info("Подписка на топик: {}", AnalyzerTopics.TELEMETRY_HUBS_V1);

            while (true) {
                try {
                    ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                    if (!records.isEmpty()) {
                        log.debug("Получено {} записей из топика", records.count());
                    }

                    for (ConsumerRecord<String, HubEventAvro> record : records) {
                        try {
                            log.info("Обработка события хаба: hubId={}, timestamp={}",
                                    record.value().getHubId(), record.timestamp());
                            handleEvent(record.value());

                            // Коммитим после успешной обработки каждой записи
                            consumer.commitSync();
                            log.debug("Коммит offset для записи: partition={}, offset={}",
                                    record.partition(), record.offset());
                        } catch (Exception e) {
                            log.error("Ошибка при обработке записи: partition={}, offset={}",
                                    record.partition(), record.offset(), e);
                            // В случае ошибки не коммитим, чтобы можно было переобработать
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
            log.info("Получен сигнал остановки, завершаем работу HubEventProcessor");
        } catch (Exception e) {
            log.error("Критическая ошибка в HubEventProcessor", e);
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

    private void handleEvent(HubEventAvro hubEvent) {
        String hubId = hubEvent.getHubId();
        log.debug("Обработка события для хаба: {}", hubId);

        try {
            switch (hubEvent.getPayload()) {
                case DeviceAddedEventAvro deviceAddedEvent -> {
                    log.info("Получено событие добавления устройства для хаба: {}", hubId);
                    handleDeviceAdded(hubId, deviceAddedEvent);
                }
                case DeviceRemovedEventAvro deviceRemovedEvent -> {
                    log.info("Получено событие удаления устройства для хаба: {}", hubId);
                    handleDeviceRemoved(hubId, deviceRemovedEvent);
                }
                case ScenarioAddedEventAvro scenarioAddedEvent -> {
                    log.info("Получено событие добавления сценария для хаба: {}", hubId);
                    handleScenarioAdded(hubId, scenarioAddedEvent);
                }
                case ScenarioRemovedEventAvro scenarioRemovedEvent -> {
                    log.info("Получено событие удаления сценария для хаба: {}", hubId);
                    handleScenarioRemoved(hubId, scenarioRemovedEvent);
                }
                case null -> log.error("Payload события хаба равен null: {}", hubEvent);
                default -> log.error("Получено событие хаба неизвестного типа: {}",
                        hubEvent.getPayload().getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события для хаба: {}", hubId, e);
            throw e;
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        log.debug("Проверка существования устройства с ID: {} в хабе: {}", event.getId(), hubId);

        if (sensorRepository.existsByIdInAndHubId(List.of(event.getId()), hubId)) {
            log.warn("Устройство с ID: {} уже зарегистрировано в хабе: {}", event.getId(), hubId);
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(event.getId());
        sensor.setHubId(hubId);

        try {
            sensorRepository.save(sensor);
            log.info("Устройство с ID: {} успешно зарегистрировано в хабе: {}", event.getId(), hubId);
        } catch (Exception e) {
            log.error("Ошибка при сохранении устройства с ID: {} в хабе: {}", event.getId(), hubId, e);
            throw e;
        }
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        log.debug("Проверка существования устройства с ID: {} в хабе: {}", event.getId(), hubId);

        if (!sensorRepository.existsByIdInAndHubId(List.of(event.getId()), hubId)) {
            log.warn("Устройство с ID: {} не найдено в хабе: {}", event.getId(), hubId);
            return;
        }

        try {
            sensorRepository.deleteById(event.getId());
            log.info("Устройство с ID: {} успешно удалено из хаба: {}", event.getId(), hubId);
        } catch (Exception e) {
            log.error("Ошибка при удалении устройства с ID: {} из хаба: {}", event.getId(), hubId, e);
            throw e;
        }
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        log.debug("Проверка существования сценария с именем: {} в хабе: {}", event.getName(), hubId);

        Optional<Scenario> existingScenario = scenarioRepository.findByHubIdAndName(hubId, event.getName());
        if (existingScenario.isPresent()) {
            log.warn("Сценарий с именем: {} уже зарегистрирован в хабе: {}", event.getName(), hubId);
            return;
        }

        try {
            Scenario scenario = new Scenario();
            scenario.setHubId(hubId);
            scenario.setName(event.getName());
            scenario.setConditions(
                    event.getConditions().stream()
                            .collect(Collectors.toMap(
                                    condition -> condition.getSensorId(),
                                    conditionMapper::fromAvro
                            ))
            );
            scenario.setActions(
                    event.getActions().stream()
                            .collect(Collectors.toMap(
                                    action -> action.getSensorId(),
                                    actionMapper::fromAvro
                            ))
            );

            scenarioRepository.save(scenario);
            log.info("Сценарий с именем: {} успешно зарегистрирован в хабе: {}. " +
                            "Количество условий: {}, количество действий: {}",
                    event.getName(), hubId,
                    scenario.getConditions().size(),
                    scenario.getActions().size());
        } catch (Exception e) {
            log.error("Ошибка при сохранении сценария с именем: {} в хабе: {}", event.getName(), hubId, e);
            throw e;
        }
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        log.debug("Поиск сценария с именем: {} в хабе: {}", event.getName(), hubId);

        Optional<Scenario> existingScenario = scenarioRepository.findByHubIdAndName(hubId, event.getName());
        if (existingScenario.isEmpty()) {
            log.warn("Сценарий с именем: {} не найден в хабе: {}", event.getName(), hubId);
            return;
        }

        try {
            scenarioRepository.deleteById(existingScenario.get().getId());
            log.info("Сценарий с именем: {} успешно удален из хаба: {}", event.getName(), hubId);
        } catch (Exception e) {
            log.error("Ошибка при удалении сценария с именем: {} из хаба: {}", event.getName(), hubId, e);
            throw e;
        }
    }
}