package ru.yandex.practicum.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Condition {
    @Enumerated(EnumType.STRING)
    ConditionType type;

    @Enumerated(EnumType.STRING)
    ConditionOperation operation;

    Integer value;
}