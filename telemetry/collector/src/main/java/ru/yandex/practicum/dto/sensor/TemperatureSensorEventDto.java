package ru.yandex.practicum.dto.sensor;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemperatureSensorEventDto extends SensorEventDto {
    private Integer temperatureC;
    private Integer temperatureF;

    @Override
    public String getType() {
        return "TEMPERATURE_SENSOR_EVENT";
    }
}
