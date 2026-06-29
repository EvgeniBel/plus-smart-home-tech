package ru.yandex.practicum.grpc.handler.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.SensorEventHandler;
import ru.yandex.practicum.service.SensorEventService;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemperatureSensorEventHandler implements SensorEventHandler {

    private final SensorEventService sensorEventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        TemperatureSensorProto tempSensor = event.getTemperatureSensor();
        log.info("Обработка события датчика температуры: id={}, hubId={}, temp={}°C, {}°F",
                event.getId(), event.getHubId(),
                tempSensor.getTemperatureC(),
                tempSensor.getTemperatureF());

        sensorEventService.sendSensorEventFromProto(event);
    }
}