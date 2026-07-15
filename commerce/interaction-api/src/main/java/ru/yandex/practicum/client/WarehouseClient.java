package ru.yandex.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.constants.ApiConstants;
import ru.yandex.practicum.dto.*;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "warehouse")
public interface WarehouseClient {

    @PutMapping(ApiConstants.BASE_PATH_WAREHOUSE)
    void newProductInWarehouse(@Valid @RequestBody NewProductInWarehouseRequest request);

    @PostMapping(ApiConstants.WAREHOUSE_CHECK)
    BookedProductsDto checkProductQuantityEnoughForShoppingCart(@RequestBody ShoppingCartDto cart);

    @PostMapping(ApiConstants.WAREHOUSE_ADD)
    void addProductToWarehouse(@Valid @RequestBody AddProductToWarehouseRequest request);

    @GetMapping(ApiConstants.WAREHOUSE_ADDRESS)
    AddressDto getWarehouseAddress();


    /**
     * Собрать товары для заказа
     * POST /api/v1/warehouse/assembly
     */
    @PostMapping(ApiConstants.WAREHOUSE_ASSEMBLY)
    BookedProductsDto assemblyProductsForOrder(@Valid @RequestBody AssemblyProductsForOrderRequest request);

    /**
     * Передать товары в доставку
     * POST /api/v1/warehouse/shipped
     */
    @PostMapping(ApiConstants.WAREHOUSE_SHIPPED)
    void shippedToDelivery(@Valid @RequestBody ShippedToDeliveryRequest request);

    /**
     * Вернуть товары на склад
     * POST /api/v1/warehouse/return
     */
    @PostMapping(ApiConstants.WAREHOUSE_RETURN)
    void acceptReturn(@RequestBody Map<UUID, Long> products);
}