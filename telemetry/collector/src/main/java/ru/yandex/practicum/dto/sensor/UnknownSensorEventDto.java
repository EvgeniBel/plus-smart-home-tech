package ru.yandex.practicum.dto.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UnknownSensorEventDto extends SensorEventDto {

    private String unknownType;

    @Override
    public String getType() {
        return "UNKNOWN_SENSOR_EVENT";
    }
}