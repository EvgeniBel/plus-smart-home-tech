package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.enums.ProductState;
import ru.yandex.practicum.enums.QuantityState;
import ru.yandex.practicum.exception.ProductNotFoundException;
import ru.yandex.practicum.mapper.ProductMapper;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /**
     * GET /api/v1/shopping-store
     * Получение страницы товаров по категории
     */
    public Page<ProductDto> getProducts(String category, Pageable pageable) {
        try {
            ProductCategory productCategory = ProductCategory.valueOf(category);

            Page<Product> products = productRepository
                    .findByProductCategoryAndProductState(
                            productCategory,
                            ProductState.ACTIVE,
                            pageable
                    );

            return products.map(productMapper::toDto);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid category: {}, returning all active products", category);
            Page<Product> products = productRepository
                    .findByProductState(ProductState.ACTIVE, pageable);
            return products.map(productMapper::toDto);
        }
    }

    /**
     * GET /api/v1/shopping-store/{productId}
     * Получение товара по ID
     */
    public ProductDto getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        if (product.getProductState() == ProductState.DEACTIVATE) {
            throw new ProductNotFoundException(
                    "Product is deactivated: " + productId
            );
        }

        return productMapper.toDto(product);
    }

    /**
     * PUT /api/v1/shopping-store
     * Создание нового товара
     */
    @Transactional
    public ProductDto createNewProduct(ProductDto productDto) {
        log.info("Creating new product: {}", productDto.getProductName());

        Product product = productMapper.toEntity(productDto);
        product.setProductState(ProductState.ACTIVE);

        if (product.getQuantityState() == null) {
            product.setQuantityState(QuantityState.ENDED);
        }

        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getProductId());

        return productMapper.toDto(saved);
    }

    /**
     * POST /api/v1/shopping-store
     * Обновление существующего товара
     */
    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        if (productDto.getProductId() == null) {
            throw new IllegalArgumentException("Product ID must be provided for update");
        }

        log.info("Updating product: {}", productDto.getProductId());

        Product existing = productRepository
                .findById(productDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productDto.getProductId()
                ));

        // Обновляем только переданные поля
        if (productDto.getProductName() != null) {
            existing.setProductName(productDto.getProductName());
        }
        if (productDto.getDescription() != null) {
            existing.setDescription(productDto.getDescription());
        }
        if (productDto.getImageSrc() != null) {
            existing.setImageSrc(productDto.getImageSrc());
        }
        if (productDto.getPrice() != null) {
            existing.setPrice(productDto.getPrice());
        }
        if (productDto.getProductCategory() != null) {
            existing.setProductCategory(productDto.getProductCategory());
        }
        if (productDto.getQuantityState() != null) {
            existing.setQuantityState(productDto.getQuantityState());
        }
        // productState не обновляем - используется removeProductFromStore

        Product updated = productRepository.save(existing);
        log.info("Product updated: {}", updated.getProductId());

        return productMapper.toDto(updated);
    }

    /**
     * POST /api/v1/shopping-store/removeProductFromStore
     * Soft delete - деактивация товара
     */
    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        log.info("Removing product from store: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);

        log.info("Product deactivated: {}", productId);
        return true;
    }

    /**
     * POST /api/v1/shopping-store/quantityState
     * Обновление статуса количества (вызывается со стороны склада)
     */
    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        log.info("Updating quantity state for product: {} to {}",
                request.getProductId(),
                request.getQuantityState()
        );

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + request.getProductId()
                ));

        product.setQuantityState(request.getQuantityState());
        productRepository.save(product);

        log.info("Quantity state updated for product: {}", request.getProductId());
        return true;
    }
}