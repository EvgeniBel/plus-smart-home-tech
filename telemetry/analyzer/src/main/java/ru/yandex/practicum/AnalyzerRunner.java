package ru.yandex.practicum;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.processor.HubEventProcessor;
import ru.yandex.practicum.processor.SnapshotProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    private final HubEventProcessor hubEventProcessor;
    private final SnapshotProcessor snapshotProcessor;

    @Override
    public void run(String... args) {
        log.info("Запуск AnalyzerRunner");

        try {
            Thread hubEventsThread = new Thread(hubEventProcessor);
            hubEventsThread.setName("HubEventHandlerThread");
            hubEventsThread.start();
            log.info("HubEventHandlerThread успешно запущен");

            snapshotProcessor.start();
            log.info("SnapshotProcessor успешно запущен");
        } catch (Exception e) {
            log.error("Ошибка при запуске компонентов Analyzer", e);
            throw new RuntimeException("Ошибка запуска Analyzer", e);
        }
    }
}