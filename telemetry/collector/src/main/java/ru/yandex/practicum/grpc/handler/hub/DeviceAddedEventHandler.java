package ru.yandex.practicum.grpc.handler.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.HubEventHandler;
import telemetry.service.collector.DeviceAddedEventProto;
import telemetry.service.collector.HubEventProto;
import ru.yandex.practicum.service.HubEventService;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceAddedEventHandler implements HubEventHandler {

    private final HubEventService hubEventService;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceAddedEventProto deviceAdded = event.getDeviceAdded();
        log.info("Обработка события добавления устройства: hubId={}, deviceId={}, type={}",
                event.getHubId(), deviceAdded.getId(), deviceAdded.getType());

        hubEventService.sendHubEventFromProto(event);
    }
}