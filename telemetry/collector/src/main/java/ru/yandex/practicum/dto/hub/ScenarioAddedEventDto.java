package ru.yandex.practicum.dto.hub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ScenarioAddedEventDto extends HubEventDto {

    @NotBlank(message = "Scenario name не может быть blank")
    @Size(min = 3, message = "Название добавленного сценария должно содержать не менее 3 символов")
    String name;

    @NotEmpty(message = "Conditions list не может быть empty")
    @Valid
    List<ScenarioConditionDto> conditions;

    @NotEmpty(message = "Actions list не может быть empty")
    @Valid
    List<DeviceActionDto> actions;

    @Override
    public String getType() {
        return "SCENARIO_ADDED";
    }
}