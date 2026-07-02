package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.ProductDto;

import java.util.List;

@FeignClient(name = "shopping-store")
public interface ShoppingStoreClient {

    @GetMapping("/api/v1/products")
    List<ProductDto> getAllProduct();

    @GetMapping("/api/v1/products/{productId}")
    ProductDto getProduct(@PathVariable("productId") Long productId);

    @GetMapping("/api/v1/products/category/{category}")
    List<ProductDto> getProductsByCategory(@PathVariable("category") String category);

    @PostMapping("/api/v1/products/{productId}")
    ProductDto updateProduct(@PathVariable("productId") Long productId,
                             @RequestBody ProductDto productDto);

    @DeleteMapping("/api/v1/products/{productId}")
    void deleteProduct(@PathVariable("productId") Long productId);
}
