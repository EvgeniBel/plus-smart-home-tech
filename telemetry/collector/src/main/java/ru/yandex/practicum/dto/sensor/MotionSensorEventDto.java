package ru.yandex.practicum.dto.sensor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class MotionSensorEventDto extends SensorEventDto {
    Integer linkQuality;
    Boolean motion;
    Integer voltage;

    @Override
    public String getType() {
        return "MOTION_SENSOR_EVENT";
    }
}
