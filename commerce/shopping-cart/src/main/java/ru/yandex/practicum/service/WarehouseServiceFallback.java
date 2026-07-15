package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.*;

import java.util.Map;
import java.util.UUID;


@Slf4j
public class WarehouseServiceFallback implements WarehouseClient {

    @Override
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.warn("FALLBACK: Метод newProductInWarehouse вызван, но сервис склада недоступен. Товар: {}",
                request != null ? request.getProductId() : "null");
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        log.warn("FALLBACK: Проверка наличия товаров на складе недоступна. Корзина: {}",
                cart != null ? cart.getShoppingCartId() : "null");
        // Возвращаем "безопасный" ответ - считаем, что товаров достаточно
        log.info("Возвращаем безопасный ответ: товары считаются доступными");
        return BookedProductsDto.builder()
                .deliveryWeight(0.0)
                .deliveryVolume(0.0)
                .fragile(false)
                .build();
    }

    @Override
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.warn("FALLBACK: Метод addProductToWarehouse вызван, но сервис склада недоступен. Товар: {}",
                request != null ? request.getProductId() : "null");
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public AddressDto getWarehouseAddress() {
        log.warn("FALLBACK: Запрос адреса склада недоступен. Возвращаем адрес по умолчанию");
        return AddressDto.builder()
                .country("Неизвестно")
                .city("Неизвестно")
                .street("Неизвестно")
                .house("Неизвестно")
                .flat("Неизвестно")
                .build();
    }

    @Override
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        log.warn("FALLBACK: Метод assemblyProductsForOrder вызван, но сервис склада недоступен. Заказ: {}",
                request != null ? request.getOrderId() : "null");
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        log.warn("FALLBACK: Метод shippedToDelivery вызван, но сервис склада недоступен. Заказ: {}",
                request != null ? request.getOrderId() : "null");
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public void acceptReturn(Map<UUID, Long> products) {
        log.warn("FALLBACK: Метод acceptReturn вызван, но сервис склада недоступен. Количество товаров: {}",
                products != null ? products.size() : 0);
        throw new RuntimeException("Сервис склада временно недоступен");
    }
}