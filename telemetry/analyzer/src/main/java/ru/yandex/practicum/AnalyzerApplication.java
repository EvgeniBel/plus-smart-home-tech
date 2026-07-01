package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class AnalyzerApplication {

    public static void main(String[] args) {
        log.info("Запуск Analyzer приложения");
        SpringApplication.run(AnalyzerApplication.class, args);
        log.info("Analyzer приложение успешно запущено");
    }
}