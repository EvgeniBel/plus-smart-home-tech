package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceRemovedEventDto extends HubEventDto {

    @NotBlank(message = "Device ID не может быть пустым")
    private String id;

    @Override
    public String getType() {
        return "DEVICE_REMOVED";
    }
}
