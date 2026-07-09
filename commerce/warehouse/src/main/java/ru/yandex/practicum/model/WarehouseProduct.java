package ru.yandex.practicum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "warehouse_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class WarehouseProduct {
    @Id
    UUID productId;

    @Column
    String name;

    @Column(length = 1000)
    String description;

    @Column(nullable = false)
    Double weight;

    @Column(nullable = false)
    Double width;

    @Column(nullable = false)
    Double height;

    @Column(nullable = false)
    Double depth;

    @Column
    boolean fragile;

    @Column(nullable = false)
    Integer quantity;
}