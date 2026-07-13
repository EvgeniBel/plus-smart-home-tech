package ru.yandex.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.constants.ApiConstants;
import ru.yandex.practicum.dto.*;

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
}