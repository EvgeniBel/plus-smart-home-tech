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
        long eventTimestamp = event.getTimestamp();

        log.info("🔄 Обновление состояния: датчик {}, время {}", sensorId, eventTimestamp);

        // Конвертируем long в Instant
        Instant eventInstant = Instant.ofEpochMilli(eventTimestamp);

        SensorStateAvro oldState = sensorsState.get(sensorId);

        if (oldState != null) {
            Instant oldTimestamp = oldState.getTimestamp();
            if (eventInstant.isBefore(oldTimestamp) || eventInstant.equals(oldTimestamp)) {
                log.info("⏭️ Событие от датчика {} устарело ({} < {}), игнорируем",
                        sensorId, eventInstant, oldTimestamp);
                return Optional.empty();
            }
            if (oldState.getData().equals(event.getPayload())) {
                log.info("⏭️ Данные датчика {} не изменились, игнорируем", sensorId);
                return Optional.empty();
            }
            log.info("🔄 Данные датчика {} изменились: {} -> {}",
                    sensorId, oldState.getData(), event.getPayload());
        } else {
            log.info("🆕 Новый датчик {} добавлен", sensorId);
        }

        // Создаём состояние с Instant
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventInstant)
                .setData(event.getPayload())
                .build();

        sensorsState.put(sensorId, newState);
        this.hubId = event.getHubId();

        if (lastUpdateTimestamp == null || eventInstant.isAfter(lastUpdateTimestamp)) {
            this.lastUpdateTimestamp = eventInstant;
        }

        log.info("✅ Снапшот для хаба {} обновлён: датчик {}, timestamp {}",
                hubId, sensorId, eventInstant);

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