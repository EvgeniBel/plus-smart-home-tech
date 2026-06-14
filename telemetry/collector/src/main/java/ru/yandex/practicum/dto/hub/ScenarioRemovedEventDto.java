package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ScenarioRemovedEventDto extends HubEventDto {

    @NotBlank(message = "Scenario name не может быть пустым")
    @Size(min = 3, message = "Название добавленного сценария должно содержать не менее 3 символов.")
    String name;

    @Override
    public String getType() {
        return "SCENARIO_REMOVED";
    }
}
