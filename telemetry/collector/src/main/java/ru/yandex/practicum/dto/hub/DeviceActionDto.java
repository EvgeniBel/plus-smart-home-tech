package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceActionDto {

    @NotBlank(message = "ID датчика не может быть пустым")
    String sensorId;

    @NotNull(message = "Action type не может быть равен null")
    ActionType type;

    Integer value;
}