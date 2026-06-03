package ru.yandex.practicum.mapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.hub.*;
import ru.yandex.practicum.dto.kafka.KafkaHubEvent;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HubEventMapper {

    // Существующий метод для Avro (если нужен)
    public HubEventAvro toAvro(HubEventDto dto) {
        if (dto == null) {
            return null;
        }

        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(dto.getHubId())
                .setTimestamp(dto.getTimestamp());

        if (dto instanceof DeviceAddedEventDto) {
            DeviceAddedEventDto deviceDto = (DeviceAddedEventDto) dto;
            DeviceAddedEventAvro deviceAdded = DeviceAddedEventAvro.newBuilder()
                    .setId(deviceDto.getId())
                    .setType(DeviceTypeAvro.valueOf(deviceDto.getDeviceType().name()))
                    .build();
            builder.setPayload(deviceAdded);

        } else if (dto instanceof DeviceRemovedEventDto) {
            DeviceRemovedEventDto deviceDto = (DeviceRemovedEventDto) dto;
            DeviceRemovedEventAvro deviceRemoved = DeviceRemovedEventAvro.newBuilder()
                    .setId(deviceDto.getId())
                    .build();
            builder.setPayload(deviceRemoved);

        } else if (dto instanceof ScenarioAddedEventDto) {
            ScenarioAddedEventDto scenarioDto = (ScenarioAddedEventDto) dto;

            var conditions = scenarioDto.getConditions().stream()
                    .map(this::toScenarioConditionAvro)
                    .collect(Collectors.toList());

            var actions = scenarioDto.getActions().stream()
                    .map(this::toDeviceActionAvro)
                    .collect(Collectors.toList());

            ScenarioAddedEventAvro scenarioAdded = ScenarioAddedEventAvro.newBuilder()
                    .setName(scenarioDto.getName())
                    .setConditions(conditions)
                    .setActions(actions)
                    .build();
            builder.setPayload(scenarioAdded);

        } else if (dto instanceof ScenarioRemovedEventDto) {
            ScenarioRemovedEventDto scenarioDto = (ScenarioRemovedEventDto) dto;
            ScenarioRemovedEventAvro scenarioRemoved = ScenarioRemovedEventAvro.newBuilder()
                    .setName(scenarioDto.getName())
                    .build();
            builder.setPayload(scenarioRemoved);
        }

        return builder.build();
    }

    // НОВЫЙ МЕТОД для отправки в Kafka (без Avro)
    public KafkaHubEvent toKafkaEvent(HubEventDto dto) {
        if (dto == null) {
            return null;
        }

        KafkaHubEvent event = new KafkaHubEvent();
        event.setHubId(dto.getHubId());
        event.setTimestamp(dto.getTimestamp().toEpochMilli());

        Map<String, Object> payload = new HashMap<>();

        if (dto instanceof DeviceAddedEventDto) {
            DeviceAddedEventDto deviceDto = (DeviceAddedEventDto) dto;
            payload.put("id", deviceDto.getId());
            payload.put("type", deviceDto.getDeviceType().name());

        } else if (dto instanceof DeviceRemovedEventDto) {
            DeviceRemovedEventDto deviceDto = (DeviceRemovedEventDto) dto;
            payload.put("id", deviceDto.getId());

        } else if (dto instanceof ScenarioAddedEventDto) {
            ScenarioAddedEventDto scenarioDto = (ScenarioAddedEventDto) dto;
            payload.put("name", scenarioDto.getName());

            var conditions = scenarioDto.getConditions().stream()
                    .map(this::toConditionMap)
                    .collect(Collectors.toList());
            payload.put("conditions", conditions);

            var actions = scenarioDto.getActions().stream()
                    .map(this::toActionMap)
                    .collect(Collectors.toList());
            payload.put("actions", actions);

        } else if (dto instanceof ScenarioRemovedEventDto) {
            ScenarioRemovedEventDto scenarioDto = (ScenarioRemovedEventDto) dto;
            payload.put("name", scenarioDto.getName());
        }

        event.setPayload(payload);
        return event;
    }

    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioConditionDto dto) {
        Object value = dto.getValue();
        Object avroValue = null;

        if (value instanceof Integer) {
            avroValue = value;
        } else if (value instanceof Boolean) {
            avroValue = value;
        }

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(ConditionTypeAvro.valueOf(dto.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(dto.getOperation().name()))
                .setValue(avroValue)
                .build();
    }

    private DeviceActionAvro toDeviceActionAvro(DeviceActionDto dto) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(ActionTypeAvro.valueOf(dto.getType().name()))
                .setValue(dto.getValue())
                .build();
    }

    // Вспомогательные методы для конвертации в Map
    private Map<String, Object> toConditionMap(ScenarioConditionDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("sensor_id", dto.getSensorId());
        map.put("type", dto.getType().name());
        map.put("operation", dto.getOperation().name());
        if (dto.getValue() != null) {
            map.put("value", dto.getValue());
        }
        return map;
    }

    private Map<String, Object> toActionMap(DeviceActionDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("sensor_id", dto.getSensorId());
        map.put("type", dto.getType().name());
        if (dto.getValue() != null) {
            map.put("value", dto.getValue());
        }
        return map;
    }
}