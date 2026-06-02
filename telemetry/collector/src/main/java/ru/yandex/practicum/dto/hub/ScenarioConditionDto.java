package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScenarioConditionDto {

    @NotBlank(message = "Sensor ID не может быть пустым")
    private String sensorId;

    @NotNull(message = "Condition type не может быть null")
    private ConditionType type;

    @NotNull(message = "Operation не может быть null")
    private ConditionOperation operation;

    private Object value;
}
