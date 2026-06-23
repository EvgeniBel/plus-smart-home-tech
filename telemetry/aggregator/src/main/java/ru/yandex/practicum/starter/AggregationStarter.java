package ru.yandex.practicum.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.AggregatorService;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, SensorsSnapshotAvro> producer;
    private final AggregatorService aggregatorService;

    @Value("${kafka.topics.sensors:telemetry.sensors.v1}")
    private String sensorsTopic;

    @Value("${app.aggregation.poll-timeout:1000}")
    private long pollTimeout;

    @Value("${app.aggregation.shutdown-timeout:5000}")
    private long shutdownTimeout;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    public void start() {
        log.info("🚀 Запуск AggregationStarter...");

        try {
            consumer.subscribe(java.util.List.of(sensorsTopic));
            log.info("📡 Подписка на топик: {}", sensorsTopic);

            while (running) {
                log.debug("⏳ Ожидание сообщений...");
                long pollStart = System.currentTimeMillis();

                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofMillis(pollTimeout));

                long pollEnd = System.currentTimeMillis();
                log.debug("⏱️ poll() занял {} мс", pollEnd - pollStart);

                if (records.isEmpty()) {
                    log.debug("📭 Нет сообщений в топике");
                    continue;
                }

                log.info("📨 Получено {} записей из топика {}", records.count(), sensorsTopic);

                int count = 0;
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    try {
                        log.info("📄 Запись {}: offset={}, partition={}",
                                count, record.offset(), record.partition());

                        SensorEventAvro event = record.value();
                        log.info("📊 Событие: id={}, hubId={}, timestamp={}",
                                event.getId(), event.getHubId(), event.getTimestamp());

                        long processStart = System.currentTimeMillis();
                        Optional<SensorsSnapshotAvro> snapshotOpt =
                                aggregatorService.processEvent(event);
                        long processEnd = System.currentTimeMillis();
                        log.info("⏱️ processEvent() занял {} мс", processEnd - processStart);

                        if (snapshotOpt.isPresent()) {
                            log.info("🔄 Снапшот обновлён, отправка...");
                            long sendStart = System.currentTimeMillis();
                            executor.submit(() -> {
                                aggregatorService.sendSnapshot(producer, snapshotOpt.get());
                            });
                            long sendEnd = System.currentTimeMillis();
                            log.info("⏱️ Отправка в executor заняла {} мс", sendEnd - sendStart);

                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        } else {
                            log.info("⏭️ Снапшот не изменился, пропускаем");
                        }

                        // Управление оффсетами
                        manageOffsets(record, count);

                    } catch (Exception e) {
                        log.error("❌ Ошибка обработки записи: offset={}", record.offset(), e);
                    }
                    count++;
                }

                // Асинхронный коммит
                log.debug("📝 Фиксация оффсетов...");
                long commitStart = System.currentTimeMillis();
                consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                    long commitEnd = System.currentTimeMillis();
                    if (exception != null) {
                        log.error("❌ Ошибка фиксации оффсетов: {}", offsets, exception);
                    } else {
                        log.debug("✅ Оффсеты зафиксированы за {} мс", commitEnd - commitStart);
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

    // ✅ Управление оффсетами (как в примере)
    private void manageOffsets(ConsumerRecord<String, SensorEventAvro> record, int count) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void shutdown() {
        log.info("Завершение работы AggregationStarter...");

        try {
            // ✅ Синхронный коммит перед закрытием (как в примере)
            try {
                consumer.commitSync(currentOffsets);
                log.info("Смещения зафиксированы синхронно перед завершением");
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
            }

            // Завершаем ExecutorService
            executor.shutdown();
            try {
                if (!executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Сбрасываем буфер продюсера
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