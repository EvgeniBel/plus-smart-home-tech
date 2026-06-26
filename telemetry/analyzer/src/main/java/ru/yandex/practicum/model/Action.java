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
public class Action {
    @Enumerated(EnumType.STRING)
    ActionType type;

    Integer value;
}