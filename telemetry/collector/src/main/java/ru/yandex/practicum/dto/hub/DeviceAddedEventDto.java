package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class DeviceAddedEventDto extends HubEventDto {

    @NotBlank(message = "Device ID не может быть пустым")
    String id;

    @NotNull(message = "Device type не может быть null")
    DeviceType deviceType;

    @Override
    public String getType() {
        return "DEVICE_ADDED";
    }
}
