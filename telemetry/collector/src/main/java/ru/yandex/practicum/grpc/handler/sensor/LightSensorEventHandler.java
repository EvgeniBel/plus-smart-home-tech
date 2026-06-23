package ru.yandex.practicum.grpc.handler.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.SensorEventHandler;
import telemetry.service.collector.LightSensorProto;
import telemetry.service.collector.SensorEventProto;
import ru.yandex.practicum.service.SensorEventService;

@Component
@RequiredArgsConstructor
@Slf4j
public class LightSensorEventHandler implements SensorEventHandler {

    private final SensorEventService sensorEventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        LightSensorProto lightSensor = event.getLightSensor();
        log.info("Обработка события датчика освещенности: id={}, hubId={}, luminosity={}",
                event.getId(), event.getHubId(), lightSensor.getLuminosity());

        sensorEventService.sendSensorEventFromProto(event);
    }
}