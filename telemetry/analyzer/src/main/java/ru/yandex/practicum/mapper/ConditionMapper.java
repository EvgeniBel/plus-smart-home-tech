package ru.yandex.practicum.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.ConditionOperation;
import ru.yandex.practicum.model.ConditionType;

@Slf4j
@Component
public class ConditionMapper {

    public Condition fromAvro(ScenarioConditionAvro avro) {
        if (avro == null) {
            log.warn("⚠️ Условие не может быть null");
            return null;
        }

        if (avro.getType() == null) {
            log.warn("⚠️ Тип условия не указан");
            return null;
        }

        if (avro.getOperation() == null) {
            log.warn("⚠️ Операция условия не указана");
            return null;
        }

        // Преобразуем тип условия
        ConditionType conditionType = convertConditionType(avro.getType());
        if (conditionType == null) {
            log.warn("⚠️ Неизвестный тип условия: {}", avro.getType());
            return null;
        }

        // Преобразуем операцию
        ConditionOperation operation = convertConditionOperation(avro.getOperation());
        if (operation == null) {
            log.warn("⚠️ Неизвестная операция: {}", avro.getOperation());
            return null;
        }

        // Получаем значение
        Double value = getValue(avro);
        if (value == null) {
            log.warn("⚠️ Значение условия не указано");
            return null;
        }

        log.debug("✅ Преобразовано условие: тип={}, операция={}, значение={}",
                conditionType, operation, value);
        return new Condition(conditionType, operation, value);
    }

    private ConditionType convertConditionType(ConditionTypeAvro avro) {
        switch (avro) {
            case MOTION: return ConditionType.MOTION;
            case LUMINOSITY: return ConditionType.LUMINOSITY;
            case SWITCH: return ConditionType.SWITCH;
            case TEMPERATURE: return ConditionType.TEMPERATURE;
            case CO2LEVEL: return ConditionType.CO2LEVEL;
            case HUMIDITY: return ConditionType.HUMIDITY;
            default: return null;
        }
    }

    private ConditionOperation convertConditionOperation(ConditionOperationAvro avro) {
        switch (avro) {
            case EQUALS: return ConditionOperation.EQUALS;
            case GREATER_THAN: return ConditionOperation.GREATER_THAN;
            case LOWER_THAN: return ConditionOperation.LOWER_THAN;
            default: return null;
        }
    }

    private Double getValue(ScenarioConditionAvro avro) {
        // Пробуем получить значение через общий метод
        Object value = avro.getValue();
        if (value == null) {
            return null;
        }

        // Определяем тип значения
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1.0 : 0.0;
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }

        log.warn("⚠️ Неподдерживаемый тип значения: {}", value.getClass().getSimpleName());
        return null;
    }
}