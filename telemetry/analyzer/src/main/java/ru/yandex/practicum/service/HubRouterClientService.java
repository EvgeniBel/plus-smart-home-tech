package ru.yandex.practicum.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.model.Action;
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

    public void sendAction(String hubId, String scenarioName, String sensorId, Action action) {
        try {
            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(DeviceActionProto.newBuilder()
                            .setSensorId(sensorId)
                            .setType(ActionTypeProto.valueOf(action.getType().name()))
                            .setValue(action.getValue() != null ? action.getValue() : 0)
                            .build())
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            hubRouterClient.handleDeviceAction(request);
            log.info("✅ Действие отправлено: sensorId={}, type={}", sensorId, action.getType());
        } catch (Exception e) {
            log.error("❌ Ошибка отправки действия: sensorId={}", sensorId, e);
        }
    }
}