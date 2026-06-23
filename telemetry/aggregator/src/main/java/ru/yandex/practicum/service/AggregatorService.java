package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.model.SnapshotState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final Map<String, SnapshotState> snapshots = new ConcurrentHashMap<>();
    @Value("${kafka.topics.snapshots:telemetry.snapshots.v1}")
    private String snapshotsTopic;

    public Optional<SensorsSnapshotAvro> processEvent(SensorEventAvro event) {
        if (event == null) {
            return Optional.empty();
        }

        String hubId = event.getHubId();
        log.debug("Обработка события от датчика {} для хаба {}", event.getId(), hubId);

        SnapshotState snapshot = snapshots.computeIfAbsent(hubId, k -> new SnapshotState());
        return snapshot.update(event);
    }

    public void sendSnapshot(Producer<String, SensorsSnapshotAvro> producer, SensorsSnapshotAvro snapshot) {
        if (snapshot == null) {
            return;
        }

        String hubId = snapshot.getHubId();
        log.info("Отправка снапшота для хаба {} в топик {}", hubId, snapshotsTopic);

        ProducerRecord<String, SensorsSnapshotAvro> record =
                new ProducerRecord<>(snapshotsTopic, hubId, snapshot);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки снапшота для хаба {}", hubId, exception);
            } else {
                log.debug("Снапшот для хаба {} отправлен", hubId);
            }
        });
    }

    public int getSnapshotsCount() {
        return snapshots.size();
    }
}