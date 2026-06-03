package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.kafka.KafkaSensorEvent;
import ru.yandex.practicum.dto.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEventDto dto) {
        if (dto == null) {
            return null;
        }

        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(dto.getId())
                .setHubId(dto.getHubId())
                .setTimestamp(dto.getTimestamp().toEpochMilli());

        if (dto instanceof ClimateSensorEventDto) {
            ClimateSensorEventDto climateDto = (ClimateSensorEventDto) dto;

            int tempC = climateDto.getTemperatureC() != null ? climateDto.getTemperatureC() : 0;
            int humidity = climateDto.getHumidity() != null ? climateDto.getHumidity() : 0;
            int co2 = climateDto.getCo2Level() != null ? climateDto.getCo2Level() : 0;

            ClimateSensorAvro climateData = ClimateSensorAvro.newBuilder()
                    .setTemperatureC(tempC)
                    .setHumidity(humidity)
                    .setCo2Level(co2)
                    .build();
            builder.setPayload(climateData);

        } else if (dto instanceof LightSensorEventDto) {
            LightSensorEventDto lightDto = (LightSensorEventDto) dto;
            LightSensorAvro lightData = LightSensorAvro.newBuilder()
                    .setLinkQuality(lightDto.getLinkQuality() != null ? lightDto.getLinkQuality() : 0)
                    .setLuminosity(lightDto.getLuminosity() != null ? lightDto.getLuminosity() : 0)
                    .build();
            builder.setPayload(lightData);

        } else if (dto instanceof MotionSensorEventDto) {
            MotionSensorEventDto motionDto = (MotionSensorEventDto) dto;
            MotionSensorAvro motionData = MotionSensorAvro.newBuilder()
                    .setLinkQuality(motionDto.getLinkQuality() != null ? motionDto.getLinkQuality() : 0)
                    .setMotion(motionDto.getMotion() != null ? motionDto.getMotion() : false)
                    .setVoltage(motionDto.getVoltage() != null ? motionDto.getVoltage() : 0)
                    .build();
            builder.setPayload(motionData);

        } else if (dto instanceof SwitchSensorEventDto) {
            SwitchSensorEventDto switchDto = (SwitchSensorEventDto) dto;
            SwitchSensorAvro switchData = SwitchSensorAvro.newBuilder()
                    .setState(switchDto.getState() != null ? switchDto.getState() : false)
                    .build();
            builder.setPayload(switchData);

        } else if (dto instanceof TemperatureSensorEventDto) {
            TemperatureSensorEventDto tempDto = (TemperatureSensorEventDto) dto;
            TemperatureSensorAvro tempData = TemperatureSensorAvro.newBuilder()
                    .setId(tempDto.getId())
                    .setHubId(tempDto.getHubId())
                    .setTimestamp(tempDto.getTimestamp().toEpochMilli())
                    .setTemperatureC(tempDto.getTemperatureC() != null ? tempDto.getTemperatureC() : 0)
                    .setTemperatureF(tempDto.getTemperatureF() != null ? tempDto.getTemperatureF() : 32)
                    .build();
            builder.setPayload(tempData);
        }

        return builder.build();
    }

    public KafkaSensorEvent toKafkaEvent(SensorEventDto dto) {
        if (dto == null) {
            return null;
        }

        KafkaSensorEvent event = new KafkaSensorEvent();
        event.setId(dto.getId());
        event.setHubId(dto.getHubId());
        event.setTimestamp(dto.getTimestamp().toEpochMilli());

        Map<String, Object> payload = new HashMap<>();

        if (dto instanceof ClimateSensorEventDto) {
            ClimateSensorEventDto climateDto = (ClimateSensorEventDto) dto;
            payload.put("temperature_c", climateDto.getTemperatureC() != null ? climateDto.getTemperatureC() : 0);
            payload.put("humidity", climateDto.getHumidity() != null ? climateDto.getHumidity() : 0);
            payload.put("co2_level", climateDto.getCo2Level() != null ? climateDto.getCo2Level() : 0);

        } else if (dto instanceof LightSensorEventDto) {
            LightSensorEventDto lightDto = (LightSensorEventDto) dto;
            payload.put("link_quality", lightDto.getLinkQuality() != null ? lightDto.getLinkQuality() : 0);
            payload.put("luminosity", lightDto.getLuminosity() != null ? lightDto.getLuminosity() : 0);

        } else if (dto instanceof MotionSensorEventDto) {
            MotionSensorEventDto motionDto = (MotionSensorEventDto) dto;
            payload.put("link_quality", motionDto.getLinkQuality() != null ? motionDto.getLinkQuality() : 0);
            payload.put("motion", motionDto.getMotion() != null ? motionDto.getMotion() : false);
            payload.put("voltage", motionDto.getVoltage() != null ? motionDto.getVoltage() : 0);

        } else if (dto instanceof SwitchSensorEventDto) {
            SwitchSensorEventDto switchDto = (SwitchSensorEventDto) dto;
            payload.put("state", switchDto.getState() != null ? switchDto.getState() : false);

        } else if (dto instanceof TemperatureSensorEventDto) {
            TemperatureSensorEventDto tempDto = (TemperatureSensorEventDto) dto;
            payload.put("id", tempDto.getId());
            payload.put("hubId", tempDto.getHubId());
            payload.put("timestamp", tempDto.getTimestamp().toEpochMilli());
            payload.put("temperature_c", tempDto.getTemperatureC() != null ? tempDto.getTemperatureC() : 0);
            payload.put("temperature_f", tempDto.getTemperatureF() != null ? tempDto.getTemperatureF() : 32);
        }

        event.setPayload(payload);
        return event;
    }
}