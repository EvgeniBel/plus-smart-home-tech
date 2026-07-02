package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.CartItemDto;
import ru.yandex.practicum.dto.ShoppingCartDto;

import java.util.UUID;

@FeignClient(name = "shopping-cart")
public interface ShoppingCartClient {

    @GetMapping("/api/v1/carts/{username}")
    ShoppingCartDto getCart(@PathVariable("username") String username);

    @PostMapping("/api/v1/carts/{username}/add")
    ShoppingCartDto addProduct(@PathVariable("username") String username,
                               @RequestBody CartItemDto item);

    @DeleteMapping("/api/v1/carts/{username}/remove/{productId}")
    ShoppingCartDto removeProduct(@PathVariable("username") String username,
                                  @PathVariable("productId") UUID productId);

    @PutMapping("/api/v1/carts/{username}/deactivate")
    void deactivateCart(@PathVariable("username") String username);
}