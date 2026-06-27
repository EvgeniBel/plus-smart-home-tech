package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterClientService hubRouterService;

    @Transactional(readOnly = true)
    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        log.info("📊 Обработка снапшота для хаба: {}, датчиков: {}", hubId, sensorsState.size());
        log.debug("📊 Состояние датчиков: {}", sensorsState.keySet());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("🔍 Найдено сценариев для хаба {}: {}", hubId, scenarios.size());

        scenarios.stream()
                .filter(scenario -> {
                    log.debug("🔍 Проверка сценария: {}, условий: {}",
                            scenario.getName(), scenario.getConditions().size());
                    return !scenario.getConditions().isEmpty();
                })
                .filter(scenario -> {
                    boolean conditionsMet = checkAllConditions(scenario, sensorsState);
                    log.info("🔍 Сценарий '{}' - условия выполнены: {}", scenario.getName(), conditionsMet);
                    return conditionsMet;
                })
                .forEach(scenario -> {
                    log.info("⚡ Выполнение сценария: hubId={}, name={}", hubId, scenario.getName());
                    executeScenario(hubId, scenario, sensorsState);
                });
    }

    private boolean checkAllConditions(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        return scenario.getConditions().stream()
                .allMatch(sc -> checkCondition(sc, sensorsState));
    }

    private boolean checkCondition(ScenarioCondition sc, Map<String, SensorStateAvro> sensorsState) {
        String sensorId = sc.getSensor().getId();
        Condition condition = sc.getCondition();

        SensorStateAvro sensorState = sensorsState.get(sensorId);
        if (sensorState == null || sensorState.getData() == null) {
            log.debug("❌ Датчик {} не найден в снапшоте", sensorId);
            return false;
        }

        // ✅ Проверка на null для типа датчика
        if (condition.getType() == null) {
            log.warn("⚠️ Тип условия равен null для датчика {}", sensorId);
            return false;
        }

        // ✅ Проверка на null для операции
        if (condition.getOperation() == null) {
            log.warn("⚠️ Операция условия равна null для датчика {}", sensorId);
            return false;
        }

        Object actualValue = extractSensorValue(sensorState.getData(), condition.getType());
        if (actualValue == null) {
            log.debug("❌ Не удалось извлечь значение для датчика {} типа {}",
                    sensorId, condition.getType());
            return false;
        }

        // ✅ Проверка на null для значения условия
        if (condition.getValue() == null) {
            log.warn("⚠️ Значение условия равно null для датчика {} типа {}",
                    sensorId, condition.getType());
            return false;
        }

        try {
            return condition.getOperation().getPredicate().test(
                    (Double) actualValue,
                    condition.getValue().doubleValue()
            );
        } catch (ClassCastException e) {
            log.error("❌ Ошибка приведения типов: actualValue={}, expectedType=Double",
                    actualValue.getClass().getSimpleName(), e);
            return false;
        }
    }

    private Object extractSensorValue(Object payload, ConditionType type) {
        if (payload == null) {
            return null;
        }

        return switch (type) {
            case TEMPERATURE -> {
                if (payload instanceof TemperatureSensorAvro t) yield (double) t.getTemperatureC();
                if (payload instanceof ClimateSensorAvro c) yield (double) c.getTemperatureC();
                yield null;
            }
            case HUMIDITY -> payload instanceof ClimateSensorAvro c ? (double) c.getHumidity() : null;
            case CO2LEVEL -> payload instanceof ClimateSensorAvro c ? (double) c.getCo2Level() : null;
            case LUMINOSITY -> payload instanceof LightSensorAvro l ? (double) l.getLuminosity() : null;
            case MOTION -> payload instanceof MotionSensorAvro m ? (m.getMotion() ? 1.0 : 0.0) : null;
            case SWITCH -> payload instanceof SwitchSensorAvro s ? (s.getState() ? 1.0 : 0.0) : null;
            default -> {
                log.warn("⚠️ Неизвестный тип условия: {}", type);
                yield null;
            }
        };
    }

    private void executeScenario(String hubId, Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        log.info("⚡ Выполнение сценария: hubId={}, name={}", hubId, scenario.getName());

        scenario.getActions().forEach(sa -> {
            String sensorId = sa.getSensor().getId();
            Action action = sa.getAction();

            if (action == null) {
                log.warn("⚠️ Действие равно null для датчика {}", sensorId);
                return;
            }

            if (action.getType() == null) {
                log.warn("⚠️ Тип действия равен null для датчика {}", sensorId);
                return;
            }

            if (!sensorsState.containsKey(sensorId)) {
                log.warn("⚠️ Датчик {} не найден в снапшоте", sensorId);
                return;
            }

            try {
                hubRouterService.sendAction(hubId, scenario.getName(), sensorId, action);
                log.info("📤 Действие отправлено: sensorId={}, type={}, value={}",
                        sensorId, action.getType(), action.getValue());
            } catch (Exception e) {
                log.error("❌ Ошибка отправки действия: sensorId={}, type={}",
                        sensorId, action.getType(), e);
            }
        });
    }
}