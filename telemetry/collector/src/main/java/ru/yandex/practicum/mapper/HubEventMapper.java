package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telemetry.service.collector.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HubEventMapper {

    public HubEventAvro toAvro(HubEventProto proto) {
        if (proto == null) {
            return null;
        }

        Instant timestamp = Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        );

        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(timestamp);

        switch (proto.getPayloadCase()) {
            case DEVICE_ADDED:
                DeviceAddedEventProto deviceAdded = proto.getDeviceAdded();
                DeviceAddedEventAvro deviceAddedAvro = DeviceAddedEventAvro.newBuilder()
                        .setId(deviceAdded.getId())
                        .setType(DeviceTypeAvro.valueOf(deviceAdded.getType().name()))
                        .build();
                builder.setPayload(deviceAddedAvro);
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEventProto deviceRemoved = proto.getDeviceRemoved();
                DeviceRemovedEventAvro deviceRemovedAvro = DeviceRemovedEventAvro.newBuilder()
                        .setId(deviceRemoved.getId())
                        .build();
                builder.setPayload(deviceRemovedAvro);
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEventProto scenarioAdded = proto.getScenarioAdded();

                var conditions = scenarioAdded.getConditionList().stream()
                        .map(this::toScenarioConditionAvro)
                        .collect(Collectors.toList());

                var actions = scenarioAdded.getActionList().stream()
                        .map(this::toDeviceActionAvro)
                        .collect(Collectors.toList());

                ScenarioAddedEventAvro scenarioAddedAvro = ScenarioAddedEventAvro.newBuilder()
                        .setName(scenarioAdded.getName())
                        .setConditions(conditions)
                        .setActions(actions)
                        .build();
                builder.setPayload(scenarioAddedAvro);
                break;

            case SCENARIO_REMOVED:
                ScenarioRemovedEventProto scenarioRemoved = proto.getScenarioRemoved();
                ScenarioRemovedEventAvro scenarioRemovedAvro = ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())  // ✅ Исправлено
                        .build();
                builder.setPayload(scenarioRemovedAvro);
                break;

            default:
                log.warn("Неизвестный тип события хаба: {}", proto.getPayloadCase());
                return null;
        }

        return builder.build();
    }

    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioConditionProto proto) {
        Object value = null;

        if (proto.hasIntValue()) {
            value = proto.getIntValue().getValue();
        } else if (proto.hasBoolValue()) {
            value = proto.getBoolValue().getValue();
        }

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ConditionTypeAvro.valueOf(proto.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(proto.getOperation().name()))
                .setValue(value)
                .build();
    }

    private DeviceActionAvro toDeviceActionAvro(DeviceActionProto proto) {
        Integer value = null;

        // ✅ Проверяем наличие значения
        if (proto.hasValue()) {
            value = proto.getValue().getValue();
        }

        return DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ActionTypeAvro.valueOf(proto.getType().name()))
                .setValue(value)
                .build();
    }
}