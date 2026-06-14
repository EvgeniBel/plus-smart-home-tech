package ru.yandex.practicum.dto.sensor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class TemperatureSensorEventDto extends SensorEventDto {
    Integer temperatureC;
    Integer temperatureF;

    @Override
    public String getType() {
        return "TEMPERATURE_SENSOR_EVENT";
    }
}
