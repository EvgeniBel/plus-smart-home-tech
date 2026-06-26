package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterClientService hubRouterService;

    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        // ✅ Исправлено: SensorStateAvro вместо SensorEventAvro
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        scenarioRepository.findByHubId(hubId).stream()
                .filter(scenario -> !scenario.getConditions().isEmpty())
                .filter(scenario -> checkAllConditions(scenario, sensorsState))
                .forEach(scenario -> executeScenario(hubId, scenario, sensorsState));
    }

    private boolean checkAllConditions(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        return scenario.getConditions().entrySet().stream()
                .allMatch(entry -> {
                    String sensorId = entry.getKey();
                    Condition condition = entry.getValue();

                    SensorStateAvro sensorState = sensorsState.get(sensorId);
                    if (sensorState == null || sensorState.getData() == null) {
                        return false;
                    }

                    Object actualValue = extractSensorValue(sensorState.getData(), condition.getType());
                    if (actualValue == null) {
                        return false;
                    }

                    return condition.getOperation().getPredicate().test((Double) actualValue, (double) condition.getValue());
                });
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
        };
    }

    private void executeScenario(String hubId, Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        log.info("⚡ Выполнение сценария: hubId={}, name={}", hubId, scenario.getName());

        scenario.getActions().forEach((sensorId, action) -> {
            if (sensorsState.containsKey(sensorId)) {
                hubRouterService.sendAction(hubId, scenario.getName(), sensorId, action);
                log.info("📤 Действие отправлено: sensorId={}, type={}, value={}",
                        sensorId, action.getType(), action.getValue());
            } else {
                log.warn("⚠️ Датчик {} не найден в снапшоте", sensorId);
            }
        });
    }
}