package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.enums.QuantityState;
import ru.yandex.practicum.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@Slf4j
@RequiredArgsConstructor
public class ShoppingStoreController implements ShoppingStoreClient {

    private final ProductService productService;

    @Override
    @GetMapping
    public Page<ProductDto> getProducts(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);
        log.info("GET /api/v1/shopping-store?category={}&page={}&size={}", category, page, size);
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
    public ProductDto createNewProduct(@Valid @RequestBody ProductDto productDto) {
        log.info("PUT /api/v1/shopping-store - creating product: {}", productDto);
        return productService.createNewProduct(productDto);
    }

    @Override
    @PostMapping
    public ProductDto updateProduct(@Valid @RequestBody ProductDto productDto) {
        log.info("POST /api/v1/shopping-store - updating product: {}", productDto);
        return productService.updateProduct(productDto);
    }

    @Override
    @PostMapping("/removeProductFromStore")
    public boolean removeProductFromStore(@Valid @RequestBody UUID productId) {
        log.info("POST /api/v1/shopping-store/removeProductFromStore - {}", productId);
        return productService.removeProductFromStore(productId);
    }

    @Override
    @PostMapping("/quantityState")
    public boolean setProductQuantityState(
            @RequestParam UUID productId,
            @RequestParam QuantityState quantityState
    ) {
        log.info("POST /api/v1/shopping-store/quantityState - productId={}, quantityState={}",
                productId, quantityState);

        SetProductQuantityStateRequest request = SetProductQuantityStateRequest.builder()
                .productId(productId)
                .quantityState(quantityState)
                .build();

        return productService.setProductQuantityState(request);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(",");
            Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("DESC")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(direction, parts[0]));
        }
        return PageRequest.of(page, size);
    }
}