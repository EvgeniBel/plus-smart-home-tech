package ru.yandex.practicum.grpc.handler.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.HubEventHandler;
import ru.yandex.practicum.grpc.telemetry.event.DeviceRemovedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.service.HubEventService;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceRemovedEventHandler implements HubEventHandler {

    private final HubEventService hubEventService;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceRemovedEventProto deviceRemoved = event.getDeviceRemoved();
        log.info("Обработка события удаления устройства: hubId={}, deviceId={}",
                event.getHubId(), deviceRemoved.getId());

        hubEventService.sendHubEventFromProto(event);
    }
}