package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.*;
import ru.yandex.practicum.mapper.WarehouseMapper;
import ru.yandex.practicum.model.WarehouseProduct;
import ru.yandex.practicum.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.*;

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
     * PUT /api/v1/warehouse
     */
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.info("Adding new product to warehouse: {}", request.getProductId());

        // Проверяем, существует ли уже товар на складе
        if (warehouseProductRepository.existsById(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Product already exists in warehouse: " + request.getProductId()
            );
        }

        // Создаем новый товар на складе
        WarehouseProduct product = warehouseMapper.toEntity(request);
        warehouseMapper.updateDimensions(product, request.getDimension());

        // Сохраняем
        warehouseProductRepository.save(product);
        log.info("Product added to warehouse: {}", request.getProductId());
    }

    /**
     * Проверить доступность товаров на складе для корзины
     * POST /api/v1/warehouse/check
     */
    @Transactional(readOnly = true)
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        log.info("Checking product availability for shopping cart: {}", cart.getShoppingCartId());

        if (cart == null || cart.getProducts() == null || cart.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Shopping cart is empty");
        }

        List<UUID> unavailableProducts = new ArrayList<>();
        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        // Проверяем каждый товар в корзине
        for (Map.Entry<UUID, Integer> entry : cart.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            WarehouseProduct product = warehouseProductRepository.findByProductId(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Product not found in warehouse: " + productId
                    ));

            // Проверяем наличие достаточного количества
            if (product.getQuantity() < requestedQuantity) {
                unavailableProducts.add(productId);
                log.warn("Insufficient quantity for product {}. Available: {}, Requested: {}",
                        productId, product.getQuantity(), requestedQuantity);
                continue;
            }

            // Считаем общий вес
            totalWeight += product.getWeight() * requestedQuantity;

            // Считаем общий объем (ширина * высота * глубина)
            double volume = product.getWidth() * product.getHeight() * product.getDepth();
            totalVolume += volume * requestedQuantity;

            // Проверяем на хрупкость
            if (product.isFragile()) {
                hasFragile = true;
            }
        }

        // Если есть недоступные товары - выбрасываем исключение
        if (!unavailableProducts.isEmpty()) {
            throw new ProductInShoppingCartLowQuantityInWarehouse(
                    "Some products have insufficient quantity in warehouse",
                    unavailableProducts
            );
        }

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
        log.info("Adding product to warehouse: {}, quantity: {}",
                request.getProductId(), request.getQuantity());

        WarehouseProduct product = warehouseProductRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Product not found in warehouse: " + request.getProductId()
                ));

        // Увеличиваем количество
        int newQuantity = product.getQuantity() + request.getQuantity().intValue();
        product.setQuantity(newQuantity);

        warehouseProductRepository.save(product);
        log.info("Product quantity updated. Product: {}, New quantity: {}",
                request.getProductId(), newQuantity);
    }

    /**
     * Получить адрес склада
     * GET /api/v1/warehouse/address
     */
    public AddressDto getWarehouseAddress() {
        log.info("Getting warehouse address: {}", CURRENT_ADDRESS);

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