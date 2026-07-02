package ru.yandex.practicum.model;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;  // ID из shopping-store

    private String name;
    private String description;
    private Double weight;
    private Double width;
    private Double height;
    private Double depth;
    private boolean fragile;

    private Integer quantity;
    private LocalDateTime updatedAt;
}