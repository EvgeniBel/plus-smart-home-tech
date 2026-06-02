package ru.yandex.practicum.dto.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LightSensorEventDto extends SensorEventDto {
    private Integer linkQuality;
    private Integer luminosity;

    @Override
    public String getType() {
        return "LIGHT_SENSOR_EVENT";
    }
}