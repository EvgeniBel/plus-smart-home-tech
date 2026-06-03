package ru.yandex.practicum.dto.sensor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class LightSensorEventDto extends SensorEventDto {
    Integer linkQuality;
    Integer luminosity;

    @Override
    public String getType() {
        return "LIGHT_SENSOR_EVENT";
    }
}