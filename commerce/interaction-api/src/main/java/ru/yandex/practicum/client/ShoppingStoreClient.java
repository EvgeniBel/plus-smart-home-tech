package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.constants.ApiConstants;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.enums.QuantityState;

import java.util.UUID;

@FeignClient(name = "shopping-store")
public interface ShoppingStoreClient {

    @GetMapping(ApiConstants.BASE_PATH_SHOPPING_STORE)
    Page<ProductDto> getProducts(
            @RequestParam(ApiConstants.PARAM_CATEGORY) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    );

    @GetMapping(ApiConstants.STORE_PRODUCT_BY_ID)
    ProductDto getProduct(@PathVariable("productId") UUID productId);

    @PutMapping(ApiConstants.BASE_PATH_SHOPPING_STORE)
    ProductDto createNewProduct(@RequestBody ProductDto productDto);

    @PostMapping(ApiConstants.BASE_PATH_SHOPPING_STORE)
    ProductDto updateProduct(@RequestBody ProductDto productDto);

    @PostMapping(ApiConstants.STORE_REMOVE_PRODUCT)
    boolean removeProductFromStore(@RequestBody UUID productId);

    @PostMapping(ApiConstants.STORE_QUANTITY_STATE)
    boolean setProductQuantityState(
            @RequestParam(ApiConstants.PARAM_PRODUCT_ID) UUID productId,
            @RequestParam QuantityState quantityState
    );
}