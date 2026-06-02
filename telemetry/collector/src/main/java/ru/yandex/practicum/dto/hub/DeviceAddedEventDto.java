package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceAddedEventDto extends HubEventDto {

    @NotBlank(message = "Device ID не может быть пустым")
    private String id;

    @NotNull(message = "Device type не может быть null")
    private DeviceType deviceType;

    @Override
    public String getType() {
        return "DEVICE_ADDED";
    }
}
