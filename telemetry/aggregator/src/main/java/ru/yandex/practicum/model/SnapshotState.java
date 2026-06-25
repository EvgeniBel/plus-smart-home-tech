package ru.yandex.practicum.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SnapshotState {

    final Map<String, HubState> hubStates = new ConcurrentHashMap<>();
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));

    public SensorsSnapshotAvro update(SensorEventAvro event) {
        if (event == null) {
            return null;
        }

        String hubId = event.getHubId();
        String sensorId = event.getId();
        long eventTime = event.getTimestamp();

        HubState hubState = hubStates.computeIfAbsent(hubId, HubState::new);
        SensorState currentState = hubState.getSensorState(sensorId);
        boolean isNewSensor = currentState == null;

        if (isNewSensor) {
            log.info("🆕 Новый датчик {} добавлен в хаб {}", sensorId, hubId);
            hubState.updateSensor(sensorId, event);
            return buildSnapshot(hubId);
        }

        Object currentPayload = currentState.getPayload();
        Object newPayload = event.getPayload();
        boolean payloadChanged = !payloadEquals(newPayload, currentPayload);

        if (payloadChanged) {
            log.info("🔄 Обновление датчика {} (изменился payload)", sensorId);
            hubState.updateSensor(sensorId, event);
            return buildSnapshot(hubId);
        }

        if (eventTime > currentState.getLastUpdateTime()) {
            log.info("🔄 Обновление датчика {} (новое время)", sensorId);
            hubState.updateSensor(sensorId, event);
            return buildSnapshot(hubId);
        }

        log.info("⏭️ Событие от датчика {} устарело, игнорируем", sensorId);
        return null;
    }

    private boolean payloadEquals(Object p1, Object p2) {
        if (p1 == p2) return true;
        if (p1 == null || p2 == null) return false;

        if (!p1.getClass().equals(p2.getClass())) {
            return false;
        }

        if (p1 instanceof ClimateSensorAvro) {
            ClimateSensorAvro c1 = (ClimateSensorAvro) p1;
            ClimateSensorAvro c2 = (ClimateSensorAvro) p2;
            return c1.getTemperatureC() == c2.getTemperatureC() &&
                    c1.getHumidity() == c2.getHumidity() &&
                    c1.getCo2Level() == c2.getCo2Level();
        }

        if (p1 instanceof LightSensorAvro) {
            LightSensorAvro l1 = (LightSensorAvro) p1;
            LightSensorAvro l2 = (LightSensorAvro) p2;
            return l1.getLuminosity() == l2.getLuminosity() &&
                    l1.getLinkQuality() == l2.getLinkQuality();
        }

        if (p1 instanceof MotionSensorAvro) {
            MotionSensorAvro m1 = (MotionSensorAvro) p1;
            MotionSensorAvro m2 = (MotionSensorAvro) p2;
            return m1.getMotion() == m2.getMotion() &&
                    m1.getVoltage() == m2.getVoltage() &&
                    m1.getLinkQuality() == m2.getLinkQuality();
        }

        if (p1 instanceof SwitchSensorAvro) {
            SwitchSensorAvro s1 = (SwitchSensorAvro) p1;
            SwitchSensorAvro s2 = (SwitchSensorAvro) p2;
            return s1.getState() == s2.getState();
        }

        return true;
    }

    private SensorsSnapshotAvro buildSnapshot(String hubId) {
        HubState hubState = hubStates.get(hubId);
        if (hubState == null) {
            return null;
        }

        Map<String, SensorState> sensors = hubState.getSensors();
        Map<String, SensorStateAvro> sensorsState = new HashMap<>();

        for (Map.Entry<String, SensorState> entry : sensors.entrySet()) {
            SensorState state = entry.getValue();

            SensorStateAvro sensorState = SensorStateAvro.newBuilder()
                    .setTimestamp(Instant.ofEpochMilli(state.getLastUpdateTime()))
                    .setData(state.getPayload())
                    .build();

            sensorsState.put(entry.getKey(), sensorState);
        }

        return SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(Instant.now())
                .setSensorsState(sensorsState)
                .build();
    }

    private String formatTime(long millis) {
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class HubState {
        final String hubId;
        final Map<String, SensorState> sensors = new HashMap<>();

        public HubState(String hubId) {
            this.hubId = hubId;
        }

        public void updateSensor(String sensorId, SensorEventAvro event) {
            sensors.put(sensorId, new SensorState(sensorId, event));
        }

        public SensorState getSensorState(String sensorId) {
            return sensors.get(sensorId);
        }

        public Map<String, SensorState> getSensors() {
            return new HashMap<>(sensors);
        }
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SensorState {
        final String sensorId;
        final Object payload;
        final long lastUpdateTime;

        public SensorState(String sensorId, SensorEventAvro event) {
            this.sensorId = sensorId;
            this.payload = event.getPayload();
            this.lastUpdateTime = event.getTimestamp();
        }
    }
}