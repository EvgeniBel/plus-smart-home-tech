package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
@Slf4j
public class SensorEventDtoMapper {

    public SensorEventAvro toAvro(SensorEventDto dto) {
        if (dto == null) {
            log.warn("Получен null SensorEventDto");
            return null;
        }

        log.info("Маппинг SensorEventDto: id={}, hubId={}, type={}",
                dto.getId(), dto.getHubId(), dto.getType());

        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(dto.getId())
                .setHubId(dto.getHubId())
                .setTimestamp(dto.getTimestamp() != null ?
                        dto.getTimestamp().toEpochMilli() :
                        Instant.now().toEpochMilli());

        if (dto instanceof MotionSensorEventDto) {
            MotionSensorEventDto event = (MotionSensorEventDto) dto;
            MotionSensorAvro payload = MotionSensorAvro.newBuilder()
                    .setLinkQuality(event.getLinkQuality())
                    .setMotion(event.getMotion())
                    .setVoltage(event.getVoltage())
                    .build();
            builder.setPayload(payload);

        } else if (dto instanceof TemperatureSensorEventDto) {
            TemperatureSensorEventDto event = (TemperatureSensorEventDto) dto;
            TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(event.getTemperatureC())
                    .setTemperatureF(event.getTemperatureF())
                    .build();
            builder.setPayload(payload);

        } else if (dto instanceof LightSensorEventDto) {
            LightSensorEventDto event = (LightSensorEventDto) dto;
            LightSensorAvro payload = LightSensorAvro.newBuilder()
                    .setLinkQuality(event.getLinkQuality())
                    .setLuminosity(event.getLuminosity())
                    .build();
            builder.setPayload(payload);

        } else if (dto instanceof ClimateSensorEventDto) {
            ClimateSensorEventDto event = (ClimateSensorEventDto) dto;
            ClimateSensorAvro payload = ClimateSensorAvro.newBuilder()
                    .setTemperatureC(event.getTemperatureC())
                    .setHumidity(event.getHumidity())
                    .setCo2Level(event.getCo2Level())
                    .build();
            builder.setPayload(payload);

        } else if (dto instanceof SwitchSensorEventDto) {
            SwitchSensorEventDto event = (SwitchSensorEventDto) dto;
            SwitchSensorAvro payload = SwitchSensorAvro.newBuilder()
                    .setState(event.getState())
                    .build();
            builder.setPayload(payload);

        } else {
            log.warn("Неизвестный тип события датчика: {}", dto.getClass().getSimpleName());
            return null;
        }

        return builder.build();
    }
}