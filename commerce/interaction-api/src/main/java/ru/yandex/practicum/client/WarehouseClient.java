package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.*;

import java.util.UUID;

@FeignClient(name = "warehouse")
public interface WarehouseClient {

    @PostMapping("/api/v1/warehouse/check")
    AvailabilityResponse checkAvailability(@RequestBody ShoppingCartDto cart);

    @GetMapping("/api/v1/warehouse/address")
    AddressDto getWarehouseAddress();

    @PostMapping("/api/v1/warehouse/products")
    WarehouseProductDto addProduct(@RequestBody AddProductRequest request);

    @PutMapping("/api/v1/warehouse/products/{productId}")
    WarehouseProductDto updateProductQuantity(@PathVariable("productId") UUID productId,
                                              @RequestBody QuantityUpdateRequest request);
}