package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
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
        log.info("Обработка события датчика: тип={}, id={}", event.getType(), event.getId());

        try {
            var avroEvent = mapper.toAvro(event);
            kafkaProducer.send(sensorEventsTopic, avroEvent);
            log.info("Событие датчика отправлено в Kafka: id={}", event.getId());
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика: id={}", event.getId(), e);
            throw new RuntimeException("Не удалось обработать событие датчика", e);
        }
    }
}