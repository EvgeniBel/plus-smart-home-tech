package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.enums.ProductState;
import ru.yandex.practicum.enums.QuantityState;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID productId;

    @Column(nullable = false)
    String productName;

    @Column(nullable = false, length = 1000)
    String description;

    String imageSrc;

    @Column(nullable = false)
    Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    QuantityState quantityState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProductState productState;

    @PrePersist
    public void prePersist() {
        if (productState == null) {
            productState = ProductState.ACTIVE;
        }
        if (quantityState == null) {
            quantityState = QuantityState.ENDED;
        }
    }
}