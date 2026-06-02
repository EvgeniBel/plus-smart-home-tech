package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
import ru.yandex.practicum.dto.sensor.UnknownSensorEventDto;
import ru.yandex.practicum.mapper.SensorEventMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventService {

    private final KafkaEventProducer kafkaProducer;
    private final SensorEventMapper mapper;

    @Value("${kafka.topics.sensor-events:sensor-events}")
    private String sensorEventsTopic;

    public void sendSensorEvent(SensorEventDto event) {
        if (event instanceof UnknownSensorEventDto) {
            log.warn("Получено неизвестное событие датчика: id={}, hubId={}",
                    event.getId(), event.getHubId());
            return;
        }

        log.info("Обработка события датчика: тип={}, id={}, hubId={}",
                event.getType(), event.getId(), event.getHubId());

        try {
            var avroEvent = mapper.toAvro(event);

            if (avroEvent == null) {
                log.error("Не удалось сконвертировать событие датчика в Avro: id={}", event.getId());
                throw new RuntimeException("Ошибка конвертации события датчика");
            }

            kafkaProducer.send(sensorEventsTopic, avroEvent);
            log.info("Событие датчика успешно отправлено в Kafka: id={}, тип={}",
                    event.getId(), event.getType());
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика: id={}, тип={}",
                    event.getId(), event.getType(), e);
            throw new RuntimeException("Не удалось обработать событие датчика: " + event.getId(), e);
        }
    }
}