package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.constants.ApiConstants;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "shopping-cart")
public interface ShoppingCartClient {

    @GetMapping(ApiConstants.BASE_PATH_SHOPPING_CART)
    ShoppingCartDto getShoppingCart(
            @RequestParam(ApiConstants.PARAM_USERNAME) String username
    );

    @PutMapping(ApiConstants.BASE_PATH_SHOPPING_CART)
    ShoppingCartDto addProductToShoppingCart(
            @RequestParam(ApiConstants.PARAM_USERNAME) String username,
            @RequestBody(required = false) Map<UUID, Long> products
    );

    @DeleteMapping(ApiConstants.BASE_PATH_SHOPPING_CART)
    void deactivateCurrentShoppingCart(
            @RequestParam(ApiConstants.PARAM_USERNAME) String username
    );

    @PostMapping(ApiConstants.CART_REMOVE)
    ShoppingCartDto removeFromShoppingCart(
            @RequestParam(ApiConstants.PARAM_USERNAME) String username,
            @RequestBody(required = false) List<UUID> productIds
    );

    @PostMapping(ApiConstants.CART_CHANGE_QUANTITY)
    ShoppingCartDto changeProductQuantity(
            @RequestParam(ApiConstants.PARAM_USERNAME) String username,
            @RequestBody(required = false) ChangeProductQuantityRequest request
    );
}