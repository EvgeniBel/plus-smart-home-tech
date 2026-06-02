package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
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
            ClimateSensorAvro climateData = ClimateSensorAvro.newBuilder()
                    .setTemperatureC(climateDto.getTemperature() != null ? climateDto.getTemperature().intValue() : 0)
                    .setHumidity(climateDto.getHumidity() != null ? climateDto.getHumidity().intValue() : 0)
                    .setCo2Level(climateDto.getCo2Level() != null ? climateDto.getCo2Level() : 0)
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
}