package ru.yandex.practicum.dto.hub;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UnknownHubEventDto extends HubEventDto {

    private String unknownType;

    @Override
    public String getType() {
        return "UNKNOWN_EVENT";
    }
}