package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.CartItemDto;

@FeignClient(name = "shopping-cart")
public interface ShoppingCartClient {

    @GetMapping("/api/v1/carts/{username}")
    ShoppingCartDto getCart(@PathVariable("username") String userName);

    @PostMapping("/api/v1/carts/{ysername}/add")
    ShoppingCartDto addProduct(@PathVariable("username") String userName,
                               @RequestBody CartItemDto item);

    @DeleteMapping("/api/v1/carts/{username}/remove/productId")
    ShoppingCartDto removeProduct(@PathVariable("username") String userName,
                                  @PathVariable("productId") Long productId);

    @PutMapping("/api/v1/carts/{username}/deactivate")
    void deactivateCard(@PathVariable("username") String userName);

}
