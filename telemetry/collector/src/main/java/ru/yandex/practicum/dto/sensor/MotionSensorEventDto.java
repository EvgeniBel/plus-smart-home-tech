package ru.yandex.practicum.dto.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MotionSensorEventDto extends SensorEventDto {
    private Integer linkQuality;
    private Boolean motion;
    private Integer voltage;

    @Override
    public String getType() {
        return "MOTION_SENSOR_EVENT";
    }
}
