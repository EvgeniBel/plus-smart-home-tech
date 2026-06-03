package ru.yandex.practicum.dto.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClimateSensorEventDto extends SensorEventDto {
    private Integer temperatureC;
    private Integer humidity;
    private Integer co2Level;

    @Override
    public String getType() {
        return "CLIMATE_SENSOR_EVENT";
    }
}