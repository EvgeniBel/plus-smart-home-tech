package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.service.WarehouseService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/warehouse")
public class WarehouseController implements WarehouseClient {

    private final WarehouseService warehouseService;

    @Override
    @PutMapping
    public void newProductInWarehouse(@Valid @RequestBody NewProductInWarehouseRequest request) {
        log.info("PUT /api/v1/warehouse - request: {}", request);
        warehouseService.newProductInWarehouse(request);
    }

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(@Valid @RequestBody ShoppingCartDto cart) {
        log.info("POST /api/v1/warehouse/check - cart: {}", cart);
        return warehouseService.checkProductQuantityEnoughForShoppingCart(cart);
    }

    @Override
    @PostMapping("/add")
    public void addProductToWarehouse(@Valid @RequestBody AddProductToWarehouseRequest request) {
        log.info("POST /api/v1/warehouse/add - request: {}", request);
        warehouseService.addProductToWarehouse(request);
    }

    @Override
    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        log.info("GET /api/v1/warehouse/address");
        return warehouseService.getWarehouseAddress();
    }

    // ========== НОВЫЕ МЕТОДЫ ==========

    /**
     * Собрать товары для заказа
     * POST /api/v1/warehouse/assembly
     */
    @Override
    @PostMapping("/assembly")
    public BookedProductsDto assemblyProductsForOrder(@Valid @RequestBody AssemblyProductsForOrderRequest request) {
        log.info("POST /api/v1/warehouse/assembly - orderId: {}, products: {}",
                request.getOrderId(), request.getProducts());
        return warehouseService.assemblyProductsForOrder(request);
    }

    /**
     * Передать товары в доставку
     * POST /api/v1/warehouse/shipped
     */
    @Override
    @PostMapping("/shipped")
    public void shippedToDelivery(@Valid @RequestBody ShippedToDeliveryRequest request) {
        log.info("POST /api/v1/warehouse/shipped - orderId: {}, deliveryId: {}",
                request.getOrderId(), request.getDeliveryId());
        warehouseService.shippedToDelivery(request);
    }

    /**
     * Вернуть товары на склад
     * POST /api/v1/warehouse/return
     */
    @Override
    @PostMapping("/return")
    public void acceptReturn(@RequestBody Map<UUID, Long> products) {
        log.info("POST /api/v1/warehouse/return - products: {}", products);
        warehouseService.acceptReturn(products);
    }
}