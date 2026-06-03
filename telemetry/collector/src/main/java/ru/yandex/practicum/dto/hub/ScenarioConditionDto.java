package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScenarioConditionDto {

    @NotBlank(message = "Sensor ID не может быть пустым")
    String sensorId;

    @NotNull(message = "Condition type не может быть null")
    ConditionType type;

    @NotNull(message = "Operation не может быть null")
    ConditionOperation operation;

    Object value;
}
