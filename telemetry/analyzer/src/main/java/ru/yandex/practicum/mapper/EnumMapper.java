package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;

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