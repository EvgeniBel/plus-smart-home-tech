package ru.yandex.practicum.dto.hub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScenarioAddedEventDto extends HubEventDto {

    @NotBlank(message = "Scenario name не можеть быть blank")
    @Size(min = 3, message = "Название добавленного сценария должно содержать не менее 3 символов")
    private String name;

    @NotNull(message = "Conditions list не можеть быть null")
    @NotEmpty(message = "Conditions list не можеть быть empty")
    @Valid
    private List<ScenarioConditionDto> conditions;

    @NotNull(message = "Actions list не можеть быть null")
    @NotEmpty(message = "Actions list не можеть быть empty")
    @Valid
    private List<DeviceActionDto> actions;

    @Override
    public String getType() {
        return "SCENARIO_ADDED";
    }
}