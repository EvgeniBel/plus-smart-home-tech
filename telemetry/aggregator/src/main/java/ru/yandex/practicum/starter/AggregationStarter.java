package ru.yandex.practicum.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.AggregatorService;

import java.time.Duration;
import java.util.Optional;
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

    public void start() {
        log.info("Запуск AggregationStarter...");

        try {
            consumer.subscribe(java.util.List.of(sensorsTopic));
            log.info("Подписка на топик: {}", sensorsTopic);

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} записей из топика {}", records.count(), sensorsTopic);

                for (var record : records) {
                    try {
                        SensorEventAvro event = record.value();
                        log.debug("Обработка события: id={}, hubId={}, timestamp={}",
                                event.getId(), event.getHubId(), event.getTimestamp());

                        Optional<SensorsSnapshotAvro> snapshotOpt =
                                aggregatorService.processEvent(event);

                        if (snapshotOpt.isPresent()) {
                            executor.submit(() -> {
                                aggregatorService.sendSnapshot(producer, snapshotOpt.get());
                            });
                        }

                    } catch (Exception e) {
                        log.error("Ошибка обработки записи: offset={}", record.offset(), e);
                    }
                }

                try {
                    consumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }

                executor.submit(() -> {
                    try {
                        producer.flush();
                    } catch (Exception e) {
                        log.error("Ошибка сброса буфера продюсера", e);
                    }
                });
            }

        } catch (WakeupException e) {
            log.info("Получен сигнал Wakeup, завершаем работу...");
        } catch (Exception e) {
            log.error("Критическая ошибка в цикле агрегации", e);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        log.info("Завершение работы AggregationStarter...");

        try {
            // ✅ Правильное завершение ExecutorService
            executor.shutdown();
            try {
                if (!executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                    log.warn("ExecutorService не завершился за {} мс, принудительно завершаем", shutdownTimeout);
                    executor.shutdownNow();
                    if (!executor.awaitTermination(shutdownTimeout / 2, TimeUnit.MILLISECONDS)) {
                        log.warn("ExecutorService не завершился после принудительного останова");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Ожидание завершения ExecutorService было прервано", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            try {
                consumer.commitSync();
                log.info("Смещения зафиксированы перед завершением");
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
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
        consumer.wakeup();
    }
}