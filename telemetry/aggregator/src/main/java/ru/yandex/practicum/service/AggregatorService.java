package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.model.SnapshotState;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregatorService {

    final SnapshotState snapshotState;

    @Value("${kafka.topics.snapshots:telemetry.snapshots.v1}")
    String snapshotsTopic;

    public SensorsSnapshotAvro processEvent(SensorEventAvro event) {
        if (event == null) {
            return null;
        }

        String hubId = event.getHubId();
        log.debug("Обработка события от датчика {} для хаба {}", event.getId(), hubId);

        return snapshotState.update(event);
    }

    @Async
    public void sendSnapshot(Producer<String, SensorsSnapshotAvro> producer, SensorsSnapshotAvro snapshot) {
        if (snapshot == null) {
            log.warn("⚠️ Попытка отправить null снапшот");
            return;
        }

        String hubId = snapshot.getHubId();
        int sensorsCount = snapshot.getSensorsState().size();
        log.info("📤 Отправка снапшота для хаба {} с {} датчиками", hubId, sensorsCount);

        ProducerRecord<String, SensorsSnapshotAvro> record =
                new ProducerRecord<>(snapshotsTopic, hubId, snapshot);

        long sendStart = System.currentTimeMillis();
        producer.send(record, (metadata, exception) -> {
            long sendEnd = System.currentTimeMillis();
            if (exception != null) {
                log.error("❌ Ошибка отправки снапшота для хаба {} за {} мс",
                        hubId, sendEnd - sendStart, exception);
            } else {
                log.info("✅ Снапшот для хаба {} отправлен за {} мс: offset={}, partition={}",
                        hubId, sendEnd - sendStart, metadata.offset(), metadata.partition());
            }
        });

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}