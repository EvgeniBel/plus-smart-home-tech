package ru.yandex.practicum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.enums.ProductState;
import ru.yandex.practicum.enums.QuantityState;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDto {
    UUID productId;

    @NotBlank(message = "productName must not be blank")
    String productName;

    @NotBlank(message = "description must not be blank")
    String description;

    String imageSrc;

    @NotNull(message = "quantityState must not be null")
    QuantityState quantityState;

    @NotNull(message = "productState must not be null")
    ProductState productState;

    @NotNull(message = "productCategory must not be null")
    ProductCategory productCategory;

    @NotNull(message = "price must not be null")
    @Min(value = 1, message = "price must be greater than 0")
    Double price;
}