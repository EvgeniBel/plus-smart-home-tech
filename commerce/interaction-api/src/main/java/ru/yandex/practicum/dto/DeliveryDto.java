package ru.yandex.practicum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.DeliveryState;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class DeliveryDto {
    UUID deliveryId;

    @NotNull
    @Valid
    AddressDto fromAddress;

    @NotNull
    @Valid
    AddressDto toAddress;

    @NotNull
    UUID orderId;

    DeliveryState deliveryState;
}