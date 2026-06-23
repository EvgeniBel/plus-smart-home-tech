package ru.yandex.practicum.grpc.handler.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.SensorEventHandler;
import telemetry.service.collector.ClimateSensorProto;
import telemetry.service.collector.SensorEventProto;
import ru.yandex.practicum.service.SensorEventService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClimateSensorEventHandler implements SensorEventHandler {

    private final SensorEventService sensorEventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        ClimateSensorProto climateSensor = event.getClimateSensor();
        log.info("Обработка события климатического датчика: id={}, hubId={}, temp={}°C, humidity={}%, CO2={}ppm",
                event.getId(), event.getHubId(),
                climateSensor.getTemperatureC(),
                climateSensor.getHumidity(),
                climateSensor.getCo2Level());

        sensorEventService.sendSensorEventFromProto(event);
    }
}