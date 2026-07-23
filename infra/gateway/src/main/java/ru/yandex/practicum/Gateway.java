package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties
@Slf4j
public class Gateway {
    public static void main(String[] args) {
        log.info("Запуск Gateway сервера");
        SpringApplication.run(Gateway.class, args);
        log.info("Gateway успешно запущен");
    }
}