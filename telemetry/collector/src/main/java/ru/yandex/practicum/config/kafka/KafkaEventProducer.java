package ru.yandex.practicum.config.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer {

    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

    public void send(String topic, String key, SpecificRecordBase event) {
        log.info("Отправка события в топик {} с ключом {}: {}", topic, key, event);
        kafkaTemplate.send(topic, key, event);
    }
}