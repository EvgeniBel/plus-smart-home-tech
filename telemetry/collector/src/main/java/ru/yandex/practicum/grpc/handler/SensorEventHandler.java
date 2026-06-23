package ru.yandex.practicum.grpc.handler;

import telemetry.service.collector.SensorEventProto;

public interface SensorEventHandler {
    SensorEventProto.PayloadCase getMessageType();

    void handle(SensorEventProto event);
}