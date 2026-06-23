package ru.yandex.practicum.grpc.handler;

import telemetry.service.collector.HubEventProto;

public interface HubEventHandler {
    HubEventProto.PayloadCase getMessageType();

    void handle(HubEventProto event);
}