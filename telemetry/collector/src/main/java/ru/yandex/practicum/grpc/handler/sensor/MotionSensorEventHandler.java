package ru.yandex.practicum.grpc.handler.sensor;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.SensorEventHandler;
import ru.yandex.practicum.service.SensorEventService;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Component
@RequiredArgsConstructor
@Slf4j
public class MotionSensorEventHandler implements SensorEventHandler {
    private final SensorEventService sensorEventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.MOTION_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        MotionSensorProto motionSensor = event.getMotionSensor();
        log.info("Обработка события датчика движения: id={}, hubId={}, motion={}, voltage={}",
                event.getId(), event.getHubId(), motionSensor.getMotion(), motionSensor.getVoltage());

        sensorEventService.sendSensorEventFromProto(event);
    }
}
