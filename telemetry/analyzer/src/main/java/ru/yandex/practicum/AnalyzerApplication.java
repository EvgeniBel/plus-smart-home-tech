package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.processor.HubEventProcessor;
import ru.yandex.practicum.processor.SnapshotProcessor;


@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(AnalyzerApplication.class, args);

        SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);
        HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Получен сигнал завершения, останавливаем Analyzer...");
            snapshotProcessor.stop();
            hubEventProcessor.stop();
        }));

        Thread hubEventsThread = new Thread(hubEventProcessor);
        hubEventsThread.setName("HubEventHandlerThread");
        hubEventsThread.start();

        Thread snapshotThread = new Thread(snapshotProcessor::start);
        snapshotThread.setName("SnapshotHandlerThread");
        snapshotThread.start();

        log.info("✅ Analyzer успешно запущен в фоновых потоках");
    }
}