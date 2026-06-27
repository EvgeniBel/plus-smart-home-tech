package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.RuleEngineService;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final RuleEngineService ruleEngineService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Value("${kafka.topics.snapshots:telemetry.snapshots.v1}")
    private String snapshotsTopic;

    @Value("${app.analyzer.poll-timeout:1000}")
    private long pollTimeout;

    public void start() {
        log.info("🚀 Запуск SnapshotProcessor...");

        try {
            snapshotConsumer.subscribe(java.util.List.of(snapshotsTopic));
            log.info("📡 Подписка на топик снапшотов: {}", snapshotsTopic);

            while (running.get()) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        snapshotConsumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("📨 Получено {} снапшотов", records.count());

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        SensorsSnapshotAvro snapshot = record.value();
                        log.debug("📊 Обработка снапшота: hubId={}, sensors={}",
                                snapshot.getHubId(), snapshot.getSensorsState().size());
                        ruleEngineService.processSnapshot(snapshot);
                    } catch (Exception e) {
                        log.error("❌ Ошибка обработки снапшота: offset={}", record.offset(), e);
                    }
                }

                snapshotConsumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.error("❌ Ошибка фиксации оффсетов", exception);
                    } else {
                        log.debug("✅ Оффсеты зафиксированы");
                    }
                });
            }

        } catch (WakeupException e) {
            log.info("⏹️ Получен сигнал Wakeup");
        } catch (Exception e) {
            log.error("❌ Критическая ошибка", e);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        log.info("🔄 Завершение SnapshotProcessor...");
        try {
            snapshotConsumer.commitSync();
            snapshotConsumer.close();
            log.info("✅ Консьюмер закрыт");
        } catch (Exception e) {
            log.error("Ошибка при закрытии консьюмера", e);
        }
    }

    public void stop() {
        running.set(false);
        snapshotConsumer.wakeup();
    }
}