package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.model.Action;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActionMapper {

    EnumMapper enumMapper;

    public Action fromAvro(DeviceActionAvro avro) {
        if (avro == null) {
            log.warn("⚠️ DeviceActionAvro is null");
            return null;
        }

        return new Action(
                enumMapper.toActionType(avro.getType()),
                avro.getValue()
        );
    }

    public DeviceActionAvro toAvro(String sensorId, Action action) {
        if (action == null) {
            log.warn("⚠️ Action is null");
            return null;
        }

        return DeviceActionAvro.newBuilder()
                .setSensorId(sensorId)
                .setType(enumMapper.toActionTypeAvro(action.getType()))
                .setValue(action.getValue())
                .build();
    }
}