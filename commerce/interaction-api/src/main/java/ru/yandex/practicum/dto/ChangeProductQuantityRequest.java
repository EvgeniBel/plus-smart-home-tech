package ru.yandex.practicum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class ChangeProductQuantityRequest {
    @NotNull(message = "productId must not be null")
    UUID productId;

    @NotNull(message = "newQuantity must not be null")
    @Min(value = 0, message = "newQuantity must be >= 0")
    Long newQuantity;
}