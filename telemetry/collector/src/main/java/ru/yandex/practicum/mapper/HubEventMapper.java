package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.event.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubEventMapper {

    public HubEventAvro toAvro(HubEventProto proto) {
        if (proto == null) {
            log.warn("Получен null proto");
            return null;
        }

        log.info("Маппинг HubEventProto: hubId={}, payloadCase={}",
                proto.getHubId(), proto.getPayloadCase());

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
                log.info("DEVICE_ADDED: id={}", deviceAdded.getId());
                break;

            case DEVICE_REMOVED:
                DeviceRemovedEventProto deviceRemoved = proto.getDeviceRemoved();
                DeviceRemovedEventAvro deviceRemovedAvro = DeviceRemovedEventAvro.newBuilder()
                        .setId(deviceRemoved.getId())
                        .build();
                builder.setPayload(deviceRemovedAvro);
                log.info("DEVICE_REMOVED: id={}", deviceRemoved.getId());
                break;

            case SCENARIO_ADDED:
                ScenarioAddedEventProto scenarioAdded = proto.getScenarioAdded();
                log.info("SCENARIO_ADDED: name={}, conditionsCount={}, actionsCount={}",
                        scenarioAdded.getName(),
                        scenarioAdded.getConditionCount(),
                        scenarioAdded.getActionCount());

                var conditions = scenarioAdded.getConditionList().stream()
                        .map(this::toScenarioConditionAvro)
                        .collect(Collectors.toList());

                var actions = scenarioAdded.getActionList().stream()
                        .map(this::toDeviceActionAvro)
                        .collect(Collectors.toList());

                if (conditions == null) conditions = new ArrayList<>();
                if (actions == null) actions = new ArrayList<>();

                ScenarioAddedEventAvro scenarioAddedAvro = ScenarioAddedEventAvro.newBuilder()
                        .setName(scenarioAdded.getName())
                        .setConditions(conditions)
                        .setActions(actions)
                        .build();
                builder.setPayload(scenarioAddedAvro);
                log.info("SCENARIO_ADDED успешно смаплен: name={}, conditions={}, actions={}",
                        scenarioAdded.getName(), conditions.size(), actions.size());
                break;

            case SCENARIO_REMOVED:
                ScenarioRemovedEventProto scenarioRemoved = proto.getScenarioRemoved();
                ScenarioRemovedEventAvro scenarioRemovedAvro = ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())
                        .build();
                builder.setPayload(scenarioRemovedAvro);
                log.info("SCENARIO_REMOVED: name={}", scenarioRemoved.getName());
                break;

            default:
                log.warn("Неизвестный тип события хаба: {}", proto.getPayloadCase());
                return null;
        }

        return builder.build();
    }

    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioConditionProto proto) {
        if (proto == null) {
            log.warn("Получен null ScenarioConditionProto");
            return null;
        }

        Object value = null;
        log.debug("Маппинг ScenarioCondition: sensorId={}, hasInt={}, hasBool={}",
                proto.getSensorId(), proto.hasIntValue(), proto.hasBoolValue());

        if (proto.hasIntValue()) {
            value = proto.getIntValue();
        } else if (proto.hasBoolValue()) {
            value = proto.getBoolValue();
        }

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ConditionTypeAvro.valueOf(proto.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(proto.getOperation().name()))
                .setValue(value)
                .build();
    }

    private DeviceActionAvro toDeviceActionAvro(DeviceActionProto proto) {
        if (proto == null) {
            log.warn("Получен null DeviceActionProto");
            return null;
        }

        Integer value = null;
        log.debug("Маппинг DeviceAction: sensorId={}, hasValue={}",
                proto.getSensorId(), proto.hasValue());

        if (proto.hasValue()) {
            value = proto.getValue();
        }

        return DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(ActionTypeAvro.valueOf(proto.getType().name()))
                .setValue(value)
                .build();
    }
}