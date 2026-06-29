package ru.yandex.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.config.kafka.KafkaEventProducer;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.mapper.SensorEventDtoMapper;
import ru.yandex.practicum.mapper.SensorEventMapper;
import telemetry.service.collector.SensorEventProto;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SensorEventService {

    final KafkaEventProducer kafkaProducer;
    final SensorEventMapper protoMapper;
    final SensorEventDtoMapper dtoMapper;

    @Value("${kafka.topics.sensor-events:telemetry.sensors.v1}")
    String sensorEventsTopic;

    public void sendSensorEvent(SensorEventDto event) {
        log.info("Обработка события датчика из REST: id={}, hubId={}, type={}",
                event.getId(), event.getHubId(), event.getType());

        try {
            SensorEventAvro avroEvent = dtoMapper.toAvro(event);

            if (avroEvent == null) {
                log.error("Ошибка конвертации события датчика в Avro: id={}", event.getId());
                throw new RuntimeException("Ошибка конвертации события датчика");
            }

            String key = avroEvent.getHubId();
            kafkaProducer.send(sensorEventsTopic, key, avroEvent);

            log.info("Событие датчика успешно отправлено в Kafka: id={}, hubId={}",
                    event.getId(), event.getHubId());
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика: id={}", event.getId(), e);
            throw new RuntimeException("Не удалось обработать событие датчика", e);
        }
    }

    public void sendSensorEventFromProto(SensorEventProto event) {
        log.info("Обработка события датчика из Proto: id={}, hubId={}, type={}",
                event.getId(), event.getHubId(), event.getPayloadCase());

        try {
            SensorEventAvro avroEvent = protoMapper.toAvro(event);

            if (avroEvent == null) {
                log.error("Ошибка конвертации события датчика в Avro: id={}", event.getId());
                throw new RuntimeException("Ошибка конвертации события датчика");
            }

            String key = avroEvent.getHubId();
            kafkaProducer.send(sensorEventsTopic, key, avroEvent);

            log.info("Событие датчика успешно отправлено в Kafka: id={}, hubId={}",
                    event.getId(), event.getHubId());
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика: id={}", event.getId(), e);
            throw new RuntimeException(String.format("Не удалось обработать событие датчика: %s", event.getId()), e);
        }
    }
}