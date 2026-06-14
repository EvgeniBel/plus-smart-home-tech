package ru.yandex.practicum.dto.sensor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class UnknownSensorEventDto extends SensorEventDto {

    String unknownType;

    @Override
    public String getType() {
        return "UNKNOWN_SENSOR_EVENT";
    }
}