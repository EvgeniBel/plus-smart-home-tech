package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.model.Action;
import ru.yandex.practicum.model.ActionType;

@Slf4j
@Component
public class ActionMapper {

    public Action fromAvro(DeviceActionAvro avro) {
        if (avro == null) {
            log.warn("⚠️ Действие не может быть null");
            return null;
        }

        if (avro.getType() == null) {
            log.warn("⚠️ Тип действия не указан");
            return null;
        }

        // Преобразуем тип
        ActionType actionType = convertActionType(avro.getType());
        if (actionType == null) {
            log.warn("⚠️ Неизвестный тип действия: {}", avro.getType());
            return null;
        }

        // Получаем значение
        Double value = getValue(avro.getValue());

        log.debug("✅ Преобразовано действие: тип={}, значение={}", actionType, value);
        return new Action(actionType, value);
    }

    private ActionType convertActionType(ActionTypeAvro avro) {
        switch (avro) {
            case ACTIVATE: return ActionType.ACTIVATE;
            case DEACTIVATE: return ActionType.DEACTIVATE;
            case INVERSE: return ActionType.INVERSE;
            case SET_VALUE: return ActionType.SET_VALUE;
            default: return null;
        }
    }

    private Double getValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1.0 : 0.0;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        log.warn("⚠️ Неподдерживаемый тип значения: {}", value.getClass().getSimpleName());
        return null;
    }
}