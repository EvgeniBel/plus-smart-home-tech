package ru.yandex.practicum.dto.kafka;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaSensorEvent {
    String id;
    String hubId;
    Long timestamp;
    Object payload;
}
