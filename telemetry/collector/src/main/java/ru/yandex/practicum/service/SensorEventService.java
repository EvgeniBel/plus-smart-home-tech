package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.dto.sensor.UnknownSensorEventDto;
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

            // Добавить проверку
            if (avroEvent == null) {
                log.error("Ошибка конвертации события датчика в Avro: id={}", event.getId());
                throw new RuntimeException("Ошибка конвертации события датчика");
            }

            String key = avroEvent.getHubId();
            kafkaProducer.send(sensorEventsTopic, key, avroEvent);

            log.info("Событие датчика успешно отправлено в Kafka: id={}, тип={}, hubId={}, ключ={}",
                    event.getId(), event.getType(), event.getHubId(), key);
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика: id={}, тип={}",
                    event.getId(), event.getType(), e);
            throw new RuntimeException("Не удалось обработать событие датчика: " + event.getId(), e);
        }
    }
}