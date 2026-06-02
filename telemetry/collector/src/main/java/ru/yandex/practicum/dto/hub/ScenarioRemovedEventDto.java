package ru.yandex.practicum.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScenarioRemovedEventDto extends HubEventDto {

    @NotBlank(message = "Scenario name не может быть пустым")
    @Size(min = 3, message = "Название добавленного сценария должно содержать не менее 3 символов.")
    private String name;

    @Override
    public String getType() {
        return "SCENARIO_REMOVED";
    }
}
