package ru.yandex.practicum.dto.kafka;

import lombok.Data;


@Data
public class KafkaSensorEvent {
    private String id;
    private String hubId;
    private Long timestamp;
    private Object payload;
}
