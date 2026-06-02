package ru.yandex.practicum.dto.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SwitchSensorEventDto extends SensorEventDto {
    private Boolean state;

    @Override
    public String getType() {
        return "SWITCH_SENSOR_EVENT";
    }
}
