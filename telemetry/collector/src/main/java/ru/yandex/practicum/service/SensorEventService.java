package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.dto.sensor.UnknownSensorEventDto;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
import ru.yandex.practicum.mapper.SensorEventMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventService {

    private final KafkaEventProducer kafkaProducer;
    private final SensorEventMapper mapper;

    @Value("${kafka.topics.sensor-events:telemetry.sensors.v1}")
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