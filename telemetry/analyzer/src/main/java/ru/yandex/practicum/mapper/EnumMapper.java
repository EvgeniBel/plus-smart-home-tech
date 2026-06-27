package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.model.ActionType;
import ru.yandex.practicum.model.ConditionOperation;
import ru.yandex.practicum.model.ConditionType;

@Slf4j
@Component
public class EnumMapper {

    // Avro → Model
    public ActionType toActionType(ActionTypeAvro avro) {
        if (avro == null) {
            return null;
        }
        switch (avro) {
            case ACTIVATE: return ActionType.ACTIVATE;
            case DEACTIVATE: return ActionType.DEACTIVATE;
            case INVERSE: return ActionType.INVERSE;
            case SET_VALUE: return ActionType.SET_VALUE;
            default: {
                log.warn("⚠️ Неизвестный тип действия: {}", avro);
                return null;
            }
        }
    }

    public ConditionType toConditionType(ConditionTypeAvro avro) {
        if (avro == null) {
            return null;
        }
        switch (avro) {
            case MOTION: return ConditionType.MOTION;
            case LUMINOSITY: return ConditionType.LUMINOSITY;
            case SWITCH: return ConditionType.SWITCH;
            case TEMPERATURE: return ConditionType.TEMPERATURE;
            case CO2LEVEL: return ConditionType.CO2LEVEL;
            case HUMIDITY: return ConditionType.HUMIDITY;
            default: {
                log.warn("⚠️ Неизвестный тип условия: {}", avro);
                return null;
            }
        }
    }

    public ConditionOperation toConditionOperation(ConditionOperationAvro avro) {
        if (avro == null) {
            return null;
        }
        switch (avro) {
            case EQUALS: return ConditionOperation.EQUALS;
            case GREATER_THAN: return ConditionOperation.GREATER_THAN;
            case LOWER_THAN: return ConditionOperation.LOWER_THAN;
            default: {
                log.warn("⚠️ Неизвестная операция: {}", avro);
                return null;
            }
        }
    }

    // Model → Avro
    public ActionTypeAvro toActionTypeAvro(ActionType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case ACTIVATE: return ActionTypeAvro.ACTIVATE;
            case DEACTIVATE: return ActionTypeAvro.DEACTIVATE;
            case INVERSE: return ActionTypeAvro.INVERSE;
            case SET_VALUE: return ActionTypeAvro.SET_VALUE;
            default: {
                log.warn("⚠️ Неизвестный тип действия: {}", type);
                return null;
            }
        }
    }

    public ConditionTypeAvro toConditionTypeAvro(ConditionType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case MOTION: return ConditionTypeAvro.MOTION;
            case LUMINOSITY: return ConditionTypeAvro.LUMINOSITY;
            case SWITCH: return ConditionTypeAvro.SWITCH;
            case TEMPERATURE: return ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL: return ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY: return ConditionTypeAvro.HUMIDITY;
            default: {
                log.warn("⚠️ Неизвестный тип условия: {}", type);
                return null;
            }
        }
    }

    public ConditionOperationAvro toConditionOperationAvro(ConditionOperation operation) {
        if (operation == null) {
            return null;
        }
        switch (operation) {
            case EQUALS: return ConditionOperationAvro.EQUALS;
            case GREATER_THAN: return ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN: return ConditionOperationAvro.LOWER_THAN;
            default: {
                log.warn("⚠️ Неизвестная операция: {}", operation);
                return null;
            }
        }
    }
}