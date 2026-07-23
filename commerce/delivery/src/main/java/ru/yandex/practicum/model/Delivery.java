package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.DeliveryState;

import java.time.LocalDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID deliveryId;

    @Column(nullable = false)
    UUID orderId;

    @Column(nullable = false)
    String fromCountry;

    @Column(nullable = false)
    String fromCity;

    @Column(nullable = false)
    String fromStreet;

    @Column(nullable = false)
    String fromHouse;

    String fromFlat;

    @Column(nullable = false)
    String toCountry;

    @Column(nullable = false)
    String toCity;

    @Column(nullable = false)
    String toStreet;

    @Column(nullable = false)
    String toHouse;

    String toFlat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DeliveryState deliveryState;

    @Column(nullable = false)
    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (deliveryState == null) {
            deliveryState = DeliveryState.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}