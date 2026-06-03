package ru.yandex.practicum.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.hub.HubEventDto;
import ru.yandex.practicum.dto.hub.UnknownHubEventDto;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
import ru.yandex.practicum.mapper.HubEventMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubEventService {

    private final KafkaEventProducer kafkaProducer;
    private final HubEventMapper mapper;

    @Value("${kafka.topics.hub-events:telemetry.hubs.v1}")
    private String hubEventsTopic;

    public void sendHubEvent(HubEventDto event) {
        if (event instanceof UnknownHubEventDto) {
            log.warn("Получено неизвестное событие хаба: hubId={}, тип неизвестен", event.getHubId());
            return;
        }

        log.info("Обработка события хаба: тип={}, hubId={}", event.getType(), event.getHubId());

        try {
            var avroEvent = mapper.toAvro(event);

            // Используем hubId как ключ для партиционирования
            String key = avroEvent.getHubId();

            // Отправляем с ключом
            kafkaProducer.send(hubEventsTopic, key, avroEvent);

            log.info("Событие хаба успешно отправлено в Kafka: тип={}, hubId={}, ключ={}",
                    event.getType(), event.getHubId(), key);
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба: тип={}, hubId={}",
                    event.getType(), event.getHubId(), e);
            throw new RuntimeException("Не удалось обработать событие хаба: " + event.getType(), e);
        }
    }
}