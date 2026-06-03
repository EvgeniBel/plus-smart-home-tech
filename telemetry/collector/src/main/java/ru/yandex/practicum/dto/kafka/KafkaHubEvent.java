package ru.yandex.practicum.dto.kafka;

import lombok.Data;

@Data
public class KafkaHubEvent {
    private String hubId;
    private Long timestamp;
    private Object payload;
}