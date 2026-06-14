package ru.yandex.practicum.dto.sensor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class SwitchSensorEventDto extends SensorEventDto {
    Boolean state;

    @Override
    public String getType() {
        return "SWITCH_SENSOR_EVENT";
    }
}
