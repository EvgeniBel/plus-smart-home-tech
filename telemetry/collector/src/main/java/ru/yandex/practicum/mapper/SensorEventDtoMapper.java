package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
@Slf4j
public class SensorEventDtoMapper {

    public SensorEventAvro toAvro(SensorEventDto dto) {
        // Реализация маппинга DTO -> Avro
        // Пока заглушка
        log.warn("Маппинг SensorEventDto в Avro не реализован полностью");
        return null;
    }
}