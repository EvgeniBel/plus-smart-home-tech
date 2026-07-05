package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.*;

@Component
@Slf4j
public class WarehouseServiceFallback implements WarehouseClient {

    @Override
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.warn("Fallback: newProductInWarehouse called but warehouse is unavailable");
        throw new RuntimeException("Warehouse service is unavailable");
    }

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        log.warn("Fallback: checkProductQuantityEnoughForShoppingCart called but warehouse is unavailable");
        // Возвращаем "безопасный" ответ - считаем, что товаров достаточно
        return BookedProductsDto.builder()
                .deliveryWeight(0.0)
                .deliveryVolume(0.0)
                .fragile(false)
                .build();
    }

    @Override
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.warn("Fallback: addProductToWarehouse called but warehouse is unavailable");
        throw new RuntimeException("Warehouse service is unavailable");
    }

    @Override
    public AddressDto getWarehouseAddress() {
        log.warn("Fallback: getWarehouseAddress called but warehouse is unavailable");
        return AddressDto.builder()
                .country("Unknown")
                .city("Unknown")
                .street("Unknown")
                .house("Unknown")
                .flat("Unknown")
                .build();
    }
}