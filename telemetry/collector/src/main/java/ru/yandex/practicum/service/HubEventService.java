package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
import ru.yandex.practicum.dto.hub.HubEventDto;
import ru.yandex.practicum.mapper.HubEventMapper;
import telemetry.service.collector.HubEventProto;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubEventService {

    final KafkaEventProducer kafkaProducer;
    final HubEventMapper mapper;

    @Value("${kafka.topics.hub-events:telemetry.hubs.v1}")
    String hubEventsTopic;

    public void sendHubEvent(HubEventDto event) {
        log.info("Обработка события хаба из REST: hubId={}, type={}",
                event.getHubId(), event.getType());

        try {
            log.info("Событие хаба получено: {}", event);
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба: hubId={}", event.getHubId(), e);
            throw new RuntimeException("Не удалось обработать событие хаба", e);
        }
    }

    public void sendHubEventFromProto(HubEventProto event) {
        log.info("Обработка события хаба из Proto: hubId={}, type={}",
                event.getHubId(), event.getPayloadCase());

        try {
            var avroEvent = mapper.toAvro(event);

            if (avroEvent == null) {
                log.error("Ошибка конвертации события хаба в Avro: hubId={}", event.getHubId());
                throw new RuntimeException("Ошибка конвертации события хаба");
            }

            String key = avroEvent.getHubId();
            kafkaProducer.send(hubEventsTopic, key, avroEvent);

            log.info("Событие хаба успешно отправлено в Kafka: hubId={}, ключ={}",
                    event.getHubId(), key);
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба: hubId={}", event.getHubId(), e);
            throw new RuntimeException(String.format("Не удалось обработать событие хаба: %s", event.getHubId()), e);
        }
    }
}