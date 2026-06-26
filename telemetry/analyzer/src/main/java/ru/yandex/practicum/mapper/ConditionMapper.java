package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.model.Condition;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConditionMapper {

    private EnumMapper enumMapper;

    public Condition fromAvro(ScenarioConditionAvro avro) {
        if (avro == null) {
            log.warn("⚠️ ScenarioConditionAvro is null");
            return null;
        }

        return new Condition(
                enumMapper.toConditionType(avro.getType()),
                enumMapper.toConditionOperation(avro.getOperation()),
                (Integer) avro.getValue()
        );
    }

    public ScenarioConditionAvro toAvro(String sensorId, Condition condition) {
        if (condition == null) {
            log.warn("⚠️ Condition is null");
            return null;
        }

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(sensorId)
                .setType(enumMapper.toConditionTypeAvro(condition.getType()))
                .setOperation(enumMapper.toConditionOperationAvro(condition.getOperation()))
                .setValue(condition.getValue())
                .build();
    }
}