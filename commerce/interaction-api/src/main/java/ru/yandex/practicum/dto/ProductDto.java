package ru.yandex.practicum.dto;

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
    String productName;
    String description;
    String imageSrc;
    QuantityState quantityState;
    ProductState productState;
    ProductCategory productCategory;
    Double price;
}