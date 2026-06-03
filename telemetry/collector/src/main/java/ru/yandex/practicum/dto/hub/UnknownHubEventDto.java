package ru.yandex.practicum.dto.hub;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class UnknownHubEventDto extends HubEventDto {

    String unknownType;

    @Override
    public String getType() {
        return "UNKNOWN_EVENT";
    }
}