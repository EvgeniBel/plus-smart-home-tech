package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.mapper.ActionMapper;
import ru.yandex.practicum.mapper.ConditionMapper;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> hubEventConsumer;
    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionMapper conditionMapper;
    private final ActionMapper actionMapper;
    private final AtomicBoolean running = new AtomicBoolean(true);
    @Value("${kafka.topics.hubs:telemetry.hubs.v1}")
    private String hubsTopic;
    @Value("${app.analyzer.poll-timeout-hub:1000}")
    private long pollTimeout;

    @Override
    public void run() {
        log.info("🚀 Запуск HubEventProcessor...");

        try {
            hubEventConsumer.subscribe(java.util.List.of(hubsTopic));
            log.info("📡 Подписка на топик событий хаба: {}", hubsTopic);

            while (running.get()) {
                ConsumerRecords<String, HubEventAvro> records =
                        hubEventConsumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("📨 Получено {} событий хаба", records.count());

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        HubEventAvro event = record.value();
                        log.debug("📊 Обработка события хаба: hubId={}, type={}",
                                event.getHubId(), event.getPayload().getClass().getSimpleName());
                        handleEvent(event);
                    } catch (Exception e) {
                        log.error("❌ Ошибка обработки события хаба: offset={}", record.offset(), e);
                    }
                }

                hubEventConsumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Ошибка фиксации оффсетов: {}", offsets, exception);
                    }
                });
            }

        } catch (WakeupException e) {
            log.info("⏹️ Получен сигнал Wakeup");
        } catch (Exception e) {
            log.error("❌ Критическая ошибка", e);
        } finally {
            shutdown();
        }
    }

    private void handleEvent(HubEventAvro event) {
        String hubId = event.getHubId();
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro) {
            handleDeviceAdded(hubId, (DeviceAddedEventAvro) payload);
        } else if (payload instanceof DeviceRemovedEventAvro) {
            handleDeviceRemoved(hubId, (DeviceRemovedEventAvro) payload);
        } else if (payload instanceof ScenarioAddedEventAvro) {
            handleScenarioAdded(hubId, (ScenarioAddedEventAvro) payload);
        } else if (payload instanceof ScenarioRemovedEventAvro) {
            handleScenarioRemoved(hubId, (ScenarioRemovedEventAvro) payload);
        } else {
            log.warn("⚠️ Неизвестный тип события: {}", payload.getClass().getSimpleName());
        }
    }
    @Transactional
    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        String deviceId = event.getId();
        log.info("🆕 Добавление устройства: hubId={}, deviceId={}", hubId, deviceId);

        if (sensorRepository.existsById(deviceId)) {
            log.info("Устройство {} уже существует", deviceId);
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(deviceId);
        sensor.setHubId(hubId);
        sensorRepository.save(sensor);
        log.info("✅ Устройство {} добавлено в хаб {}", deviceId, hubId);
    }
    @Transactional
    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        String deviceId = event.getId();
        log.info("🗑️ Удаление устройства: hubId={}, deviceId={}", hubId, deviceId);

        Optional<Sensor> sensorOpt = sensorRepository.findByIdAndHubId(deviceId, hubId);
        sensorOpt.ifPresentOrElse(
                sensor -> {
                    sensorRepository.delete(sensor);
                    log.info("✅ Устройство {} удалено из хаба {}", deviceId, hubId);
                },
                () -> log.warn("⚠️ Устройство {} не найдено в хабе {}", deviceId, hubId)
        );
    }
    @Transactional
    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        String name = event.getName();
        log.info("📝 Добавление сценария: hubId={}, name={}", hubId, name);

        // Удаляем старый сценарий, если существует
        scenarioRepository.findByHubIdAndName(hubId, name)
                .ifPresent(scenarioRepository::delete);

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(name);

        // Добавляем условия
        if (event.getConditions() != null && !event.getConditions().isEmpty()) {
            for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
                Condition condition = conditionMapper.fromAvro(conditionAvro);
                if (condition == null) {
                    log.warn("⚠️ Пропущено условие для датчика {}", conditionAvro.getSensorId());
                    continue;
                }

                Sensor sensor = sensorRepository
                        .findByIdAndHubId(conditionAvro.getSensorId(), hubId)
                        .orElse(null);
                if (sensor == null) {
                    log.warn("⚠️ Датчик {} не найден в хабе {}", conditionAvro.getSensorId(), hubId);
                    continue;
                }

                ScenarioCondition sc = new ScenarioCondition();
                sc.setScenario(scenario);
                sc.setSensor(sensor);
                sc.setCondition(condition);
                scenario.getConditions().add(sc);
                log.debug("✅ Добавлено условие: датчик={}, тип={}",
                        sensor.getId(), condition.getType());
            }
        }

        // Добавляем действия
        if (event.getActions() != null && !event.getActions().isEmpty()) {
            for (DeviceActionAvro actionAvro : event.getActions()) {
                Action action = actionMapper.fromAvro(actionAvro);
                if (action == null) {
                    log.warn("⚠️ Пропущено действие для датчика {}", actionAvro.getSensorId());
                    continue;
                }

                Sensor sensor = sensorRepository
                        .findByIdAndHubId(actionAvro.getSensorId(), hubId)
                        .orElse(null);
                if (sensor == null) {
                    log.warn("⚠️ Датчик {} не найден в хабе {}", actionAvro.getSensorId(), hubId);
                    continue;
                }

                ScenarioAction sa = new ScenarioAction();
                sa.setScenario(scenario);
                sa.setSensor(sensor);
                sa.setAction(action);
                scenario.getActions().add(sa);
                log.debug("✅ Добавлено действие: датчик={}, тип={}",
                        sensor.getId(), action.getType());
            }
        }

        scenarioRepository.save(scenario);
        log.info("✅ Сценарий '{}' добавлен в хаб {}. Условий: {}, Действий: {}",
                name, hubId, scenario.getConditions().size(), scenario.getActions().size());
    }
    @Transactional
    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String name = event.getName();
        log.info("🗑️ Удаление сценария: hubId={}, name={}", hubId, name);

        Optional<Scenario> scenarioOpt = scenarioRepository.findByHubIdAndName(hubId, name);
        scenarioOpt.ifPresentOrElse(
                scenario -> {
                    scenarioRepository.delete(scenario);
                    log.info("✅ Сценарий {} удалён из хаба {}", name, hubId);
                },
                () -> log.warn("⚠️ Сценарий {} не найден в хабе {}", name, hubId)
        );
    }

    private void shutdown() {
        log.info("🔄 Завершение HubEventProcessor...");
        try {
            hubEventConsumer.commitSync();
            hubEventConsumer.close();
            log.info("✅ Консьюмер закрыт");
        } catch (Exception e) {
            log.error("Ошибка при закрытии консьюмера", e);
        }
    }

    public void stop() {
        running.set(false);
        hubEventConsumer.wakeup();
    }
}