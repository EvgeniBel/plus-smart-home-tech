package ru.yandex.practicum.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Data
public class SnapshotState {

    private final Map<String, SensorStateAvro> sensorsState = new HashMap<>();
    private String hubId;
    private Instant lastUpdateTimestamp;

    public Optional<SensorsSnapshotAvro> update(SensorEventAvro event) {
        String sensorId = event.getId();

        // Конвертируем long в Instant
        Instant eventTimestamp = Instant.ofEpochMilli(event.getTimestamp());

        SensorStateAvro oldState = sensorsState.get(sensorId);

        if (oldState != null) {
            Instant oldTimestamp = oldState.getTimestamp();
            if (eventTimestamp.isBefore(oldTimestamp) || eventTimestamp.equals(oldTimestamp)) {
                log.debug("Событие от датчика {} устарело, игнорируем", sensorId);
                return Optional.empty();
            }
            if (oldState.getData().equals(event.getPayload())) {
                log.debug("Данные датчика {} не изменились, игнорируем", sensorId);
                return Optional.empty();
            }
        }

        // Создаём состояние с Instant
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventTimestamp)
                .setData(event.getPayload())
                .build();

        sensorsState.put(sensorId, newState);
        this.hubId = event.getHubId();

        if (lastUpdateTimestamp == null || eventTimestamp.isAfter(lastUpdateTimestamp)) {
            this.lastUpdateTimestamp = eventTimestamp;
        }

        log.info("Снапшот для хаба {} обновлён: датчик {}, timestamp {}",
                hubId, sensorId, eventTimestamp);

        return Optional.of(buildSnapshot());
    }

    private SensorsSnapshotAvro buildSnapshot() {
        // ✅ Используем Instant напрямую
        Instant timestamp = lastUpdateTimestamp != null
                ? lastUpdateTimestamp
                : Instant.now();

        return SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(timestamp)
                .setSensorsState(sensorsState)
                .build();
    }
}