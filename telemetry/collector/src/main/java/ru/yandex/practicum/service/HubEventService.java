package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.hub.HubEventDto;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
import ru.yandex.practicum.mapper.HubEventMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubEventService {

    private final KafkaEventProducer kafkaProducer;
    private final HubEventMapper mapper;

    @Value("${kafka.topics.hub-events:hub-events}")
    private String hubEventsTopic;

    public void sendHubEvent(HubEventDto event) {
        log.info("Обработка события хаба: тип={}, ID хаба={}", event.getType(), event.getHubId());

        try {
            var avroEvent = mapper.toAvro(event);
            kafkaProducer.send(hubEventsTopic, avroEvent);
            log.info("Событие хаба отправлено в Kafka: тип={}", event.getType());
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба: тип={}", event.getType(), e);
            throw new RuntimeException("Не удалось обработать событие хаба", e);
        }
    }
}
