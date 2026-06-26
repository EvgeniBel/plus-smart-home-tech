package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.model.ActionType;
import ru.yandex.practicum.model.ConditionOperation;
import ru.yandex.practicum.model.ConditionType;

@Component
public class EnumMapper {

    // Avro → Model
    public ActionType toActionType(ActionTypeAvro avro) {
        return ActionType.valueOf(avro.name());
    }

    public ConditionType toConditionType(ConditionTypeAvro avro) {
        return ConditionType.valueOf(avro.name());
    }

    public ConditionOperation toConditionOperation(ConditionOperationAvro avro) {
        return ConditionOperation.valueOf(avro.name());
    }

    // Model → Avro
    public ActionTypeAvro toActionTypeAvro(ActionType type) {
        return ActionTypeAvro.valueOf(type.name());
    }

    public ConditionTypeAvro toConditionTypeAvro(ConditionType type) {
        return ConditionTypeAvro.valueOf(type.name());
    }

    public ConditionOperationAvro toConditionOperationAvro(ConditionOperation operation) {
        return ConditionOperationAvro.valueOf(operation.name());
    }
}