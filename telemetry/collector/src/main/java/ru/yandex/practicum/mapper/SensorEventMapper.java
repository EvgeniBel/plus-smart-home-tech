package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telemetry.service.collector.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
@Slf4j
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEventProto proto) {
        if (proto == null) {
            return null;
        }

        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(proto.getTimestamp().getSeconds() * 1000 +
                        proto.getTimestamp().getNanos() / 1_000_000);

        switch (proto.getPayloadCase()) {
            case MOTION_SENSOR:
                MotionSensorProto motion = proto.getMotionSensor();
                MotionSensorAvro motionData = MotionSensorAvro.newBuilder()
                        .setLinkQuality(motion.getLinkQuality())
                        .setMotion(motion.getMotion())
                        .setVoltage(motion.getVoltage())
                        .build();
                builder.setPayload(motionData);
                break;

            case TEMPERATURE_SENSOR:
                TemperatureSensorProto temp = proto.getTemperatureSensor();
                TemperatureSensorAvro tempData = TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(temp.getTemperatureC())
                        .setTemperatureF(temp.getTemperatureF())
                        .build();
                builder.setPayload(tempData);
                break;

            case LIGHT_SENSOR:
                LightSensorProto light = proto.getLightSensor();
                LightSensorAvro lightData = LightSensorAvro.newBuilder()
                        .setLinkQuality(light.getLinkQuality())
                        .setLuminosity(light.getLuminosity())
                        .build();
                builder.setPayload(lightData);
                break;

            case CLIMATE_SENSOR:
                ClimateSensorProto climate = proto.getClimateSensor();
                ClimateSensorAvro climateData = ClimateSensorAvro.newBuilder()
                        .setTemperatureC(climate.getTemperatureC())
                        .setHumidity(climate.getHumidity())
                        .setCo2Level(climate.getCo2Level())
                        .build();
                builder.setPayload(climateData);
                break;

            case SWITCH_SENSOR:
                SwitchSensorProto switchProto = proto.getSwitchSensor();
                SwitchSensorAvro switchData = SwitchSensorAvro.newBuilder()
                        .setState(switchProto.getState())
                        .build();
                builder.setPayload(switchData);
                break;

            default:
                log.warn("Неизвестный тип сенсора: {}", proto.getPayloadCase());
                return null;
        }

        return builder.build();
    }
}