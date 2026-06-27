package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HubEventDtoMapper {

    public HubEventAvro toAvro(HubEventDto dto) {
        if (dto == null) {
            log.warn("Получен null HubEventDto");
            return null;
        }

        log.info("Маппинг HubEventDto: hubId={}, type={}", dto.getHubId(), dto.getType());

        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(dto.getHubId())
                .setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());

        if (dto instanceof DeviceAddedEventDto) {
            DeviceAddedEventDto event = (DeviceAddedEventDto) dto;
            DeviceAddedEventAvro payload = DeviceAddedEventAvro.newBuilder()
                    .setId(event.getId())
                    .setType(DeviceTypeAvro.valueOf(event.getDeviceType().name()))
                    .build();
            builder.setPayload(payload);

        } else if (dto instanceof DeviceRemovedEventDto) {
            DeviceRemovedEventDto event = (DeviceRemovedEventDto) dto;
            DeviceRemovedEventAvro payload = DeviceRemovedEventAvro.newBuilder()
                    .setId(event.getId())
                    .build();
            builder.setPayload(payload);

        } else if (dto instanceof ScenarioAddedEventDto) {
            ScenarioAddedEventDto event = (ScenarioAddedEventDto) dto;
            ScenarioAddedEventAvro payload = ScenarioAddedEventAvro.newBuilder()
                    .setName(event.getName())
                    .setConditions(event.getConditions().stream()
                            .map(this::toScenarioConditionAvro)
                            .collect(Collectors.toList()))
                    .setActions(event.getActions().stream()
                            .map(this::toDeviceActionAvro)
                            .collect(Collectors.toList()))
                    .build();
            builder.setPayload(payload);

        } else if (dto instanceof ScenarioRemovedEventDto) {
            ScenarioRemovedEventDto event = (ScenarioRemovedEventDto) dto;
            ScenarioRemovedEventAvro payload = ScenarioRemovedEventAvro.newBuilder()
                    .setName(event.getName())
                    .build();
            builder.setPayload(payload);

        } else {
            log.warn("Неизвестный тип события: {}", dto.getClass().getSimpleName());
            return null;
        }

        return builder.build();
    }

    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioConditionDto dto) {
        Object value = dto.getValue();
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(ConditionTypeAvro.valueOf(dto.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(dto.getOperation().name()))
                .setValue(value)
                .build();
    }

    private DeviceActionAvro toDeviceActionAvro(DeviceActionDto dto) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(ActionTypeAvro.valueOf(dto.getType().name()))
                .setValue(dto.getValue())
                .build();
    }
}