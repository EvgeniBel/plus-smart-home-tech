package ru.yandex.practicum.dto.sensor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ClimateSensorEventDto extends SensorEventDto {
    Integer temperatureC;
    Integer humidity;
    Integer co2Level;

    @Override
    public String getType() {
        return "CLIMATE_SENSOR_EVENT";
    }
}