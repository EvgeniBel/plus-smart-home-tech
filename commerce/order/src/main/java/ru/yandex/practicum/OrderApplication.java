package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Slf4j
public class OrderApplication {
    public static void main(String[] args) {
        log.info("Запуск Order сервера");
        SpringApplication.run(OrderApplication.class, args);
        log.info("Order успешно запущен");
    }
}