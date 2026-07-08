package ru.yandex.practicum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class DimensionDto {
    @NotNull(message = "width must not be null")
    @Min(value = 1, message = "width must be greater than 0")
    Double width;

    @NotNull(message = "height must not be null")
    @Min(value = 1, message = "height must be greater than 0")
    Double height;

    @NotNull(message = "depth must not be null")
    @Min(value = 1, message = "depth must be greater than 0")
    Double depth;
}