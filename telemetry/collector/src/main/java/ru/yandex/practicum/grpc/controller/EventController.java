package ru.yandex.practicum.grpc.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.handler.HubEventHandler;
import ru.yandex.practicum.grpc.handler.SensorEventHandler;
import telemetry.service.collector.CollectorControllerGrpc;
import telemetry.service.collector.HubEventProto;
import telemetry.service.collector.SensorEventProto;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@GrpcService
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {

    final Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    final Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    public EventController(Set<SensorEventHandler> sensorEventHandlers,
                           Set<HubEventHandler> hubEventHandlers) {
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));
        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
    }

    @Override
    public void collectSensorEvent(SensorEventProto request,
                                   io.grpc.stub.StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено событие от датчика: id={}, hubId={}, type={}",
                    request.getId(), request.getHubId(), request.getPayloadCase());

            if (sensorEventHandlers.containsKey(request.getPayloadCase())) {
                sensorEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                log.warn("Не найден обработчик для события датчика типа: {}", request.getPayloadCase());
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request,
                                io.grpc.stub.StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено событие от хаба: hubId={}, type={}",
                    request.getHubId(), request.getPayloadCase());

            if (hubEventHandlers.containsKey(request.getPayloadCase())) {
                hubEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                log.warn("Не найден обработчик для события хаба типа: {}", request.getPayloadCase());
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}