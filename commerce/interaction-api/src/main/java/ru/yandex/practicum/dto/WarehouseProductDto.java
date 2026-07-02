package ru.yandex.practicum.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarehouseProductDto {
    UUID productId;
    String name;
    String description;
    Double weight;
    Double width;
    Double height;
    Double depth;
    boolean fragile;
    Integer quantity;
}
