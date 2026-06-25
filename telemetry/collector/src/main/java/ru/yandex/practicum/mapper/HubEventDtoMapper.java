package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.hub.HubEventDto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
@Slf4j
public class HubEventDtoMapper {

    public HubEventAvro toAvro(HubEventDto dto) {
        // Реализация маппинга DTO -> Avro
        // Пока заглушка
        log.warn("Маппинг HubEventDto в Avro не реализован полностью");
        return null;
    }
}