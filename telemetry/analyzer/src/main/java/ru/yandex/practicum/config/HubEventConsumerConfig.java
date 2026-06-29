package ru.yandex.practicum.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@Configuration
@ConfigurationProperties("custom.kafka-hub-event")
public class HubEventConsumerConfig {

    @Value("${custom.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private HubEventConsumer hubEventConsumer = new HubEventConsumer();

    @Getter
    @Setter
    @FieldDefaults(level = PRIVATE)
    public static class HubEventConsumer {
        String keyDeserializer;
        String valueDeserializer;
        String autoOffsetReset;
        String groupId;
    }
}