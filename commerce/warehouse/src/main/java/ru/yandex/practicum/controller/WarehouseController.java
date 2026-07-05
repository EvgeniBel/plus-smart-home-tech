package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.service.WarehouseService;

@RestController
@RequestMapping("/api/v1/warehouse")
@Slf4j
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PutMapping
    public void newProductInWarehouse(@RequestBody NewProductInWarehouseRequest request) {
        log.info("PUT /api/v1/warehouse - request: {}", request);
        warehouseService.newProductInWarehouse(request);
    }

    @PostMapping("/check")
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(
            @RequestBody ShoppingCartDto cart) {
        log.info("POST /api/v1/warehouse/check - cart: {}", cart);
        return warehouseService.checkProductQuantityEnoughForShoppingCart(cart);
    }

    @PostMapping("/add")
    public void addProductToWarehouse(@RequestBody AddProductToWarehouseRequest request) {
        log.info("POST /api/v1/warehouse/add - request: {}", request);
        warehouseService.addProductToWarehouse(request);
    }

    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        log.info("GET /api/v1/warehouse/address");
        return warehouseService.getWarehouseAddress();
    }
}