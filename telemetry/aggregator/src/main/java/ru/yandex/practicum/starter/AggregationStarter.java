package ru.yandex.practicum.starter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.AggregatorService;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationStarter {

    final Consumer<String, SensorEventAvro> consumer;
    final Producer<String, SensorsSnapshotAvro> producer;
    final AggregatorService aggregatorService;
    final ExecutorService executor = Executors.newFixedThreadPool(5);
    final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();
    @Value("${kafka.topics.sensors:telemetry.sensors.v1}")
    String sensorsTopic;
    @Value("${app.aggregation.poll-timeout:1000}")
    long pollTimeout;
    @Value("${app.aggregation.shutdown-timeout:5000}")
    long shutdownTimeout;
    volatile boolean running = true;

    public void start() {
        log.info("🚀 Запуск AggregationStarter...");

        try {
            consumer.subscribe(java.util.List.of(sensorsTopic));
            log.info("📡 Подписка на топик: {}", sensorsTopic);

            while (running) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("📨 Получено {} записей из топика {}", records.count(), sensorsTopic);

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    try {
                        SensorEventAvro event = record.value();
                        log.debug("📊 Событие: id={}, hubId={}", event.getId(), event.getHubId());

                        SensorsSnapshotAvro snapshot = aggregatorService.processEvent(event);

                        if (snapshot != null) {
                            log.info("🔄 Снапшот обновлён, отправка...");
                            executor.submit(() -> aggregatorService.sendSnapshot(producer, snapshot));
                        }

                        manageOffsets(record);

                    } catch (Exception e) {
                        log.error("❌ Ошибка обработки записи: offset={}", record.offset(), e);
                    }
                }

                consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                    if (exception != null) {
                        log.error("❌ Ошибка фиксации оффсетов: {}", offsets, exception);
                    }
                });
            }
        } catch (WakeupException e) {
            log.info("⏹️ Получен сигнал Wakeup, завершаем работу...");
        } catch (Exception e) {
            log.error("❌ Критическая ошибка в цикле агрегации", e);
        } finally {
            shutdown();
        }
    }

    private void manageOffsets(ConsumerRecord<String, SensorEventAvro> record) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
    }

    private void shutdown() {
        log.info("Завершение работы AggregationStarter...");

        try {
            try {
                consumer.commitSync(currentOffsets);
                log.info("Смещения зафиксированы синхронно перед завершением");
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            try {
                producer.flush();
                log.info("Буфер продюсера сброшен");
            } catch (Exception e) {
                log.error("Ошибка сброса буфера продюсера при завершении", e);
            }

        } finally {
            try {
                consumer.close();
                log.info("Консьюмер закрыт");
            } catch (Exception e) {
                log.error("Ошибка закрытия консьюмера", e);
            }

            try {
                producer.close();
                log.info("Продюсер закрыт");
            } catch (Exception e) {
                log.error("Ошибка закрытия продюсера", e);
            }
        }

        log.info("AggregationStarter завершён");
    }

    public void stop() {
        log.info("Получен сигнал остановки AggregationStarter");
        running = false;
        consumer.wakeup();
    }
}