package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceActionDto {

    @NotBlank(message = "ID датчика не может быть пустым")
    private String sensorId;

    @NotNull(message = "Action type не может быть равен null")
    private ActionType type;

    private Integer value;
}