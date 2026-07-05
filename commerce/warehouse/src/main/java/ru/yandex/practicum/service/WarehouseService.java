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
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseService {

    // Адреса склада
    private static final String[] ADDRESSES = {"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS = ADDRESSES[new SecureRandom().nextInt(ADDRESSES.length)];
    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseMapper warehouseMapper;

    /**
     * Добавить новый товар на склад
     * PUT /api/v1/warehouse
     */
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.info("Добавление нового товара на склад: {}", request.getProductId());

        // Проверяем, существует ли уже товар на складе
        if (warehouseProductRepository.existsById(request.getProductId())) {
            log.warn("Товар уже существует на складе: {}", request.getProductId());
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Товар уже существует на складе: " + request.getProductId()
            );
        }

        // Создаем новый товар на складе
        WarehouseProduct product = warehouseMapper.toEntity(request);
        warehouseMapper.updateDimensions(product, request.getDimension());

        // Сохраняем
        warehouseProductRepository.save(product);
        log.info("Товар успешно добавлен на склад: {}", request.getProductId());
    }

    /**
     * Проверить доступность товаров на складе для корзины
     * POST /api/v1/warehouse/check
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

        // Проверяем каждый товар в корзине
        for (Map.Entry<UUID, Integer> entry : cart.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            log.debug("Проверка товара: {}, запрошено: {}", productId, requestedQuantity);

            WarehouseProduct product = warehouseProductRepository.findByProductId(productId)
                    .orElseThrow(() -> {
                        log.error("Товар не найден на складе: {}", productId);
                        return new NoSpecifiedProductInWarehouseException(
                                "Товар не найден на складе: " + productId
                        );
                    });

            log.debug("На складе: {}, запрошено: {}", product.getQuantity(), requestedQuantity);

            // Проверяем наличие достаточного количества
            if (product.getQuantity() < requestedQuantity) {
                unavailableProducts.add(productId);
                log.warn("Недостаточное количество товара {}. На складе: {}, Запрошено: {}",
                        productId, product.getQuantity(), requestedQuantity);
                continue;
            }

            // Считаем общий вес
            double weight = product.getWeight() * requestedQuantity;
            totalWeight += weight;
            log.debug("Вес товара {}: {} кг", productId, weight);

            // Считаем общий объем (ширина * высота * глубина)
            double volume = product.getWidth() * product.getHeight() * product.getDepth();
            double totalVolumeForProduct = volume * requestedQuantity;
            totalVolume += totalVolumeForProduct;
            log.debug("Объем товара {}: {} м³", productId, totalVolumeForProduct);

            // Проверяем на хрупкость
            if (product.isFragile()) {
                hasFragile = true;
                log.debug("Товар {} является хрупким", productId);
            }
        }

        // Если есть недоступные товары - выбрасываем исключение
        if (!unavailableProducts.isEmpty()) {
            log.warn("Обнаружены товары с недостаточным количеством: {}", unavailableProducts);
            throw new ProductInShoppingCartLowQuantityInWarehouse(
                    "Недостаточное количество товаров на складе",
                    unavailableProducts
            );
        }

        log.info("Проверка завершена успешно. Общий вес: {} кг, Общий объем: {} м³, Хрупкие: {}",
                totalWeight, totalVolume, hasFragile);

        // Возвращаем результат бронирования
        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(hasFragile)
                .build();
    }

    /**
     * Принять товар на склад (увеличить количество)
     * POST /api/v1/warehouse/add
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

        // Увеличиваем количество
        int oldQuantity = product.getQuantity();
        int newQuantity = oldQuantity + request.getQuantity().intValue();
        product.setQuantity(newQuantity);

        warehouseProductRepository.save(product);
        log.info("Количество товара обновлено. Товар: {}, Было: {}, Стало: {}",
                request.getProductId(), oldQuantity, newQuantity);
    }

    /**
     * Получить адрес склада
     * GET /api/v1/warehouse/address
     */
    public AddressDto getWarehouseAddress() {
        log.info("Запрос адреса склада. Текущий адрес: {}", CURRENT_ADDRESS);

        // Преобразуем строку адреса в объект AddressDto
        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }
}