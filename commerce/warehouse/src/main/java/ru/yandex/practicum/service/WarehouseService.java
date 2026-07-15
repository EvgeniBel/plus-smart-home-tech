package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.mapper.WarehouseMapper;
import ru.yandex.practicum.model.WarehouseAddress;
import ru.yandex.practicum.model.WarehouseProduct;
import ru.yandex.practicum.repository.WarehouseProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseAddress warehouseAddress;

    /**
     * Добавить новый товар на склад
     */
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        log.info("Добавление нового товара на склад: {}", request.getProductId());

        if (warehouseProductRepository.existsById(request.getProductId())) {
            log.warn("Товар уже существует на складе: {}", request.getProductId());
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Товар уже существует на складе: " + request.getProductId()
            );
        }

        WarehouseProduct product = warehouseMapper.toEntity(request);
        warehouseMapper.updateDimensions(product, request.getDimension());

        warehouseProductRepository.save(product);
        log.info("Товар успешно добавлен на склад: {}", request.getProductId());
    }

    /**
     * Проверить доступность товаров на складе для корзины
     */
    @Transactional(readOnly = true)
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        log.info("Проверка доступности товаров для корзины: {}",
                cart != null ? cart.getShoppingCartId() : "null");

        if (cart == null || cart.getProducts() == null || cart.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Корзина пуста");
        }

        List<UUID> unavailableProducts = new ArrayList<>();
        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (Map.Entry<UUID, Long> entry : cart.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQuantity = entry.getValue();

            if (requestedQuantity == null || requestedQuantity <= 0) {
                log.warn("Некорректное количество для товара {}: {}", productId, requestedQuantity);
                continue;
            }

            WarehouseProduct product = warehouseProductRepository.findByProductId(productId)
                    .orElseThrow(() -> {
                        log.error("Товар не найден на складе: {}", productId);
                        return new NoSpecifiedProductInWarehouseException(
                                "Товар не найден на складе: " + productId
                        );
                    });

            if (product.getQuantity() < requestedQuantity) {
                log.warn("Недостаточное количество товара {}. На складе: {}, Запрошено: {}",
                        productId, product.getQuantity(), requestedQuantity);
                unavailableProducts.add(productId);
                continue;
            }

            totalWeight += product.getWeight() * requestedQuantity;
            double volume = product.getWidth() * product.getHeight() * product.getDepth();
            totalVolume += volume * requestedQuantity;

            if (product.isFragile()) {
                hasFragile = true;
            }
        }

        if (!unavailableProducts.isEmpty()) {
            log.warn("Обнаружены товары с недостаточным количеством: {}", unavailableProducts);
            throw new ProductInShoppingCartLowQuantityInWarehouse(
                    "Недостаточное количество товаров на складе",
                    unavailableProducts
            );
        }

        log.info("Проверка завершена успешно. Общий вес: {} кг, Общий объем: {} м³, Хрупкие: {}",
                totalWeight, totalVolume, hasFragile);

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(hasFragile)
                .build();
    }

    /**
     * Принять товар на склад (увеличить количество)
     */
    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        log.info("Поступление товара на склад: {}, количество: {}",
                request.getProductId(), request.getQuantity());

        WarehouseProduct product = warehouseProductRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> {
                    log.error("Товар не найден на складе: {}", request.getProductId());
                    return new NoSpecifiedProductInWarehouseException(
                            "Товар не найден на складе: " + request.getProductId()
                    );
                });

        int oldQuantity = product.getQuantity();
        int newQuantity = oldQuantity + request.getQuantity().intValue();
        product.setQuantity(newQuantity);

        warehouseProductRepository.save(product);
        log.info("Количество товара обновлено. Товар: {}, Было: {}, Стало: {}",
                request.getProductId(), oldQuantity, newQuantity);
    }

    /**
     * Получить адрес склада
     */
    public AddressDto getWarehouseAddress() {
        AddressDto address = warehouseAddress.getAddress();
        log.info("Запрос адреса склада: {}", address);
        return address;
    }

    @Transactional
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        log.info("Сборка товаров для заказа: {}", request.getOrderId());

        if (request.getProducts() == null || request.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Список товаров пуст");
        }

        List<UUID> unavailableProducts = new ArrayList<>();
        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (Map.Entry<UUID, Long> entry : request.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQuantity = entry.getValue();

            if (requestedQuantity == null || requestedQuantity <= 0) {
                log.warn("Некорректное количество для товара {}", productId);
                continue;
            }

            WarehouseProduct product = warehouseProductRepository.findByProductId(productId)
                    .orElseThrow(() -> {
                        log.error("Товар не найден на складе: {}", productId);
                        return new NoSpecifiedProductInWarehouseException(
                                "Товар не найден на складе: " + productId
                        );
                    });

            if (product.getQuantity() < requestedQuantity) {
                log.warn("Недостаточное количество товара {}. На складе: {}, Запрошено: {}",
                        productId, product.getQuantity(), requestedQuantity);
                unavailableProducts.add(productId);
                continue;
            }

            // Уменьшаем остаток на складе
            int newQuantity = product.getQuantity() - requestedQuantity.intValue();
            product.setQuantity(newQuantity);
            warehouseProductRepository.save(product);

            totalWeight += product.getWeight() * requestedQuantity;
            double volume = product.getWidth() * product.getHeight() * product.getDepth();
            totalVolume += volume * requestedQuantity;

            if (product.isFragile()) {
                hasFragile = true;
            }
        }

        if (!unavailableProducts.isEmpty()) {
            log.warn("Обнаружены товары с недостаточным количеством: {}", unavailableProducts);
            throw new ProductInShoppingCartLowQuantityInWarehouse(
                    "Недостаточное количество товаров на складе",
                    unavailableProducts
            );
        }

        log.info("Сборка заказа {} завершена. Вес: {} кг, Объём: {} м³, Хрупкие: {}",
                request.getOrderId(), totalWeight, totalVolume, hasFragile);

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(hasFragile)
                .build();
    }

    /**
     * Передать товары в доставку
     * POST /api/v1/warehouse/shipped
     */
    @Transactional
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        log.info("Передача товаров в доставку. Заказ: {}, Доставка: {}",
                request.getOrderId(), request.getDeliveryId());

        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("ID заказа не может быть null");
        }
        if (request.getDeliveryId() == null) {
            throw new IllegalArgumentException("ID доставки не может быть null");
        }

        // Здесь можно сохранить связь заказа с доставкой в отдельной сущности
        // Например, в OrderDelivery или обновить заказ
        log.info("Товары переданы в доставку для заказа {}", request.getOrderId());
    }

    /**
     * Вернуть товары на склад
     * POST /api/v1/warehouse/return
     */
    @Transactional
    public void acceptReturn(Map<UUID, Long> products) {
        log.info("Приём возврата товаров на склад: {}", products);

        if (products == null || products.isEmpty()) {
            log.warn("Список товаров для возврата пуст");
            return;
        }

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                log.warn("Некорректное количество для возврата товара {}", productId);
                continue;
            }

            WarehouseProduct product = warehouseProductRepository.findByProductId(productId)
                    .orElseThrow(() -> {
                        log.error("Товар не найден на складе: {}", productId);
                        return new NoSpecifiedProductInWarehouseException(
                                "Товар не найден на складе: " + productId
                        );
                    });

            int oldQuantity = product.getQuantity();
            int newQuantity = oldQuantity + quantity.intValue();
            product.setQuantity(newQuantity);
            warehouseProductRepository.save(product);

            log.info("Количество товара {} увеличено: {} → {}",
                    productId, oldQuantity, newQuantity);
        }

        log.info("Возврат товаров успешно завершён");
    }
}