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
import ru.yandex.practicum.model.WarehouseProduct;
import ru.yandex.practicum.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseMapper warehouseMapper;

    // Адреса склада
    private static final String[] ADDRESSES = {"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS = ADDRESSES[new SecureRandom().nextInt(ADDRESSES.length)];

    /**
     * Добавить новый товар на склад
     */
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
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
            log.warn("Корзина пуста или не передана");
            throw new IllegalArgumentException("Корзина пуста");
        }

        List<UUID> unavailableProducts = new ArrayList<>();
        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        log.info("Проверка {} товаров в корзине", cart.getProducts().size());

        // ✅ ИСПРАВЛЕНО: итерируем по List<CartItemDto>
        for (CartItemDto item : cart.getProducts()) {
            UUID productId = item.getProductId();
            Integer requestedQuantity = item.getQuantity();

            log.debug("Проверка товара: {}, запрошено: {}", productId, requestedQuantity);

            WarehouseProduct product = warehouseProductRepository.findByProductId(productId)
                    .orElseThrow(() -> {
                        log.error("Товар не найден на складе: {}", productId);
                        return new NoSpecifiedProductInWarehouseException(
                                "Товар не найден на складе: " + productId
                        );
                    });

            log.debug("На складе: {}, запрошено: {}", product.getQuantity(), requestedQuantity);

            if (product.getQuantity() < requestedQuantity) {
                unavailableProducts.add(productId);
                log.warn("Недостаточное количество товара {}. На складе: {}, Запрошено: {}",
                        productId, product.getQuantity(), requestedQuantity);
                continue;
            }

            totalWeight += product.getWeight() * requestedQuantity;
            double volume = product.getWidth() * product.getHeight() * product.getDepth();
            totalVolume += volume * requestedQuantity;

            if (product.isFragile()) {
                hasFragile = true;
                log.debug("Товар {} является хрупким", productId);
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
        log.info("Запрос адреса склада. Текущий адрес: {}", CURRENT_ADDRESS);

        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }
}