package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {

    public static void main(String[] args) {
        log.info("Запуск Analyzer приложения");
        SpringApplication.run(Analyzer.class, args);
        log.info("Analyzer приложение успешно запущено");
    }
}