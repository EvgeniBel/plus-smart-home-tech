package ru.yandex.practicum.grpc.handler.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.SensorEventHandler;
import telemetry.service.collector.SensorEventProto;
import telemetry.service.collector.SwitchSensorProto;
import ru.yandex.practicum.service.SensorEventService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SwitchSensorEventHandler implements SensorEventHandler {

    private final SensorEventService sensorEventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        SwitchSensorProto switchSensor = event.getSwitchSensor();
        log.info("Обработка события переключателя: id={}, hubId={}, state={}",
                event.getId(), event.getHubId(), switchSensor.getState());

        sensorEventService.sendSensorEventFromProto(event);
    }
}