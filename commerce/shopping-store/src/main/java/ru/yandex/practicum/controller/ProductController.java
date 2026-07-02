package ru.yandex.practicum.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@Slf4j
@RequiredArgsConstructor
public class ProductController implements ShoppingStoreClient {

    private final ProductService productService;

    @Override
    @GetMapping
    public Page<ProductDto> getProducts(
            @RequestParam String category,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("GET /api/v1/shopping-store?category={}&page={}&size={}",
                category, pageable.getPageNumber(), pageable.getPageSize());
        return productService.getProducts(category, pageable);
    }

    @Override
    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        log.info("GET /api/v1/shopping-store/{}", productId);
        return productService.getProduct(productId);
    }

    @Override
    @PutMapping
    public ProductDto createNewProduct(@RequestBody ProductDto productDto) {
        log.info("PUT /api/v1/shopping-store - creating product: {}", productDto);
        return productService.createNewProduct(productDto);
    }

    @Override
    @PostMapping
    public ProductDto updateProduct(@RequestBody ProductDto productDto) {
        log.info("POST /api/v1/shopping-store - updating product: {}", productDto);
        return productService.updateProduct(productDto);
    }

    @Override
    @PostMapping("/removeProductFromStore")
    public boolean removeProductFromStore(@RequestBody UUID productId) {
        log.info("POST /api/v1/shopping-store/removeProductFromStore - {}", productId);
        return productService.removeProductFromStore(productId);
    }

    @Override
    @PostMapping("/quantityState")
    public boolean setProductQuantityState(@RequestBody SetProductQuantityStateRequest request) {
        log.info("POST /api/v1/shopping-store/quantityState - {}", request);
        return productService.setProductQuantityState(request);
    }
}