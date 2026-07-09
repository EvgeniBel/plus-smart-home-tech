package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.ShoppingCartClient;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.service.ShoppingCartService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@Slf4j
@RequiredArgsConstructor
public class ShoppingCartController implements ShoppingCartClient {

    private final ShoppingCartService shoppingCartService;

    @Override
    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam String username) {
        log.info("GET /api/v1/shopping-cart?username={}", username);
        return shoppingCartService.getShoppingCart(username);
    }

    @Override
    @PutMapping
    public ShoppingCartDto addProductToShoppingCart(
            @RequestParam String username,
            @RequestBody(required = false) Map<UUID, Long> products
    ) {
        log.info("PUT /api/v1/shopping-cart?username={}, products={}", username, products);

        if (products == null || products.isEmpty()) {
            log.warn("Тело запроса пустое, возвращаем текущую корзину для {}", username);
            return shoppingCartService.getShoppingCart(username);
        }

        return shoppingCartService.addProductToShoppingCart(username, products);
    }

    @Override
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCurrentShoppingCart(@RequestParam String username) {
        log.info("DELETE /api/v1/shopping-cart?username={}", username);
        shoppingCartService.deactivateCurrentShoppingCart(username);
    }

    @Override
    @PostMapping("/remove")
    public ShoppingCartDto removeFromShoppingCart(
            @RequestParam String username,
            @RequestBody(required = false) List<UUID> productIds
    ) {
        log.info("POST /api/v1/shopping-cart/remove?username={}, productIds={}",
                username, productIds);

        if (productIds == null || productIds.isEmpty()) {
            log.warn("Список товаров для удаления пуст, возвращаем текущую корзину");
            return shoppingCartService.getShoppingCart(username);
        }

        return shoppingCartService.removeFromShoppingCart(username, productIds);
    }

    @Override
    @PostMapping("/change-quantity")
    public ShoppingCartDto changeProductQuantity(
            @RequestParam String username,
            @RequestBody(required = false) ChangeProductQuantityRequest request
    ) {
        log.info("POST /api/v1/shopping-cart/change-quantity?username={}, request={}",
                username, request);

        if (request == null || request.getProductId() == null) {
            log.warn("Тело запроса пустое или не содержит productId, возвращаем текущую корзину");
            return shoppingCartService.getShoppingCart(username);
        }

        return shoppingCartService.changeProductQuantity(username, request);
    }
}