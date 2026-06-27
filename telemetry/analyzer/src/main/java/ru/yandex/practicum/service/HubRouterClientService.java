package ru.yandex.practicum.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.model.Action;
import ru.yandex.practicum.model.ActionType;
import telemetry.service.collector.ActionTypeProto;
import telemetry.service.collector.DeviceActionProto;
import telemetry.service.collector.DeviceActionRequest;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubRouterClientService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public boolean isReady() {
        return hubRouterClient != null;
    }

    public void sendAction(String hubId, String scenarioName, String sensorId, Action action) {
        if (hubRouterClient == null) {
            log.error("❌ gRPC клиент не инициализирован!");
            return;
        }

        try {
            log.info("📤 Отправка действия в Hub Router: hubId={}, scenarioName={}, sensorId={}, actionType={}",
                    hubId, scenarioName, sensorId, action.getType());

            // Преобразуем ActionType модели в ActionTypeProto
            ActionTypeProto actionTypeProto = convertToProto(action.getType());

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(DeviceActionProto.newBuilder()
                            .setSensorId(sensorId)
                            .setType(actionTypeProto)
                            .setValue(action.getValue() != null ? action.getValue().intValue() : 0)
                            .build())
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            log.debug("📦 Запрос: {}", request);

            Empty response = hubRouterClient.handleDeviceAction(request);

            log.info("✅ Действие успешно отправлено в Hub Router");

        } catch (Exception e) {
            log.error("❌ Ошибка отправки действия в Hub Router: sensorId={}, type={}",
                    sensorId, action.getType(), e);
            throw new RuntimeException("Failed to send action to Hub Router", e);
        }
    }

    private ActionTypeProto convertToProto(ActionType actionType) {
        if (actionType == null) {
            throw new IllegalArgumentException("Action type cannot be null");
        }

        return switch (actionType) {
            case ACTIVATE -> ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ActionTypeProto.DEACTIVATE;
            case INVERSE -> ActionTypeProto.INVERSE;
            case SET_VALUE -> ActionTypeProto.SET_VALUE;
            default -> throw new IllegalArgumentException("Unknown action type: " + actionType);
        };
    }
}