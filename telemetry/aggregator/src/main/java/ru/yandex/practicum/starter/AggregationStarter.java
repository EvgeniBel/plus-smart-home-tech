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

    /**
     * Запускает процесс агрегации данных.
     * Работает в бесконечном цикле до завершения приложения.
     */
    public void start() {
        log.info("Запуск AggregationStarter...");

        try {
            // Подписываемся на топик с событиями датчиков
            consumer.subscribe(java.util.List.of(sensorsTopic));
            log.info("Подписка на топик: {}", sensorsTopic);

            // Бесконечный цикл опроса
            while (true) {
                // Получаем записи из Kafka
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} записей из топика {}", records.count(), sensorsTopic);

                // Обрабатываем каждую запись
                for (var record : records) {
                    try {
                        SensorEventAvro event = record.value();
                        log.debug("Обработка события: id={}, hubId={}, timestamp={}",
                                event.getId(), event.getHubId(), event.getTimestamp());

                        // Обрабатываем событие через сервис
                        Optional<SensorsSnapshotAvro> snapshotOpt =
                                aggregatorService.processEvent(event);

                        // Если снапшот обновился — отправляем в Kafka
                        if (snapshotOpt.isPresent()) {
                            aggregatorService.sendSnapshot(producer, snapshotOpt.get());
                        }

                    } catch (Exception e) {
                        log.error("Ошибка обработки записи: offset={}", record.offset(), e);
                    }
                }

                // Фиксируем смещения после успешной обработки
                try {
                    consumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }

                // Сбрасываем буфер продюсера (отправляем все накопленные сообщения)
                try {
                    producer.flush();
                } catch (Exception e) {
                    log.error("Ошибка сброса буфера продюсера", e);
                }
            }

        } catch (WakeupException e) {
            // Игнорируем — это сигнал к завершению работы
            log.info("Получен сигнал Wakeup, завершаем работу...");
        } catch (Exception e) {
            log.error("Критическая ошибка в цикле агрегации", e);
        } finally {
            // Корректное завершение
            shutdown();
        }
    }

    /**
     * Корректно завершает работу: фиксирует смещения и закрывает продюсер/консьюмер.
     */
    private void shutdown() {
        log.info("Завершение работы AggregationStarter...");

        try {
            // Фиксируем последние смещения
            try {
                consumer.commitSync();
                log.info("Смещения зафиксированы перед завершением");
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
            }

            // Сбрасываем буфер продюсера
            try {
                producer.flush();
                log.info("Буфер продюсера сброшен");
            } catch (Exception e) {
                log.error("Ошибка сброса буфера продюсера при завершении", e);
            }

        } finally {
            // Закрываем ресурсы
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

    /**
     * Сигнал для остановки цикла опроса.
     * Вызывается извне (например, при завершении приложения).
     */
    public void stop() {
        log.info("Получен сигнал остановки AggregationStarter");
        consumer.wakeup();
    }
}