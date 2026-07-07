package ru.yandex.practicum.exception;

import java.util.List;
import java.util.UUID;

public class ProductInShoppingCartLowQuantityInWarehouse extends RuntimeException {
    private final List<UUID> unavailableProducts;

    public ProductInShoppingCartLowQuantityInWarehouse(String message, List<UUID> unavailableProducts) {
        super(message);
        this.unavailableProducts = unavailableProducts;
    }

    public ProductInShoppingCartLowQuantityInWarehouse(String message) {
        super(message);
        this.unavailableProducts = List.of();
    }

    public List<UUID> getUnavailableProducts() {
        return unavailableProducts;
    }
}