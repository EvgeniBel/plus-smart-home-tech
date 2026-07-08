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
        log.info("Запрос списка товаров по категории: {}, страница: {}, размер: {}",
                category, pageable.getPageNumber(), pageable.getPageSize());

        try {
            ProductCategory productCategory = ProductCategory.valueOf(category);
            log.debug("Категория валидна: {}", productCategory);

            Page<Product> products = productRepository
                    .findByProductCategoryAndProductState(
                            productCategory,
                            ProductState.ACTIVE,
                            pageable
                    );

            log.info("Найдено {} активных товаров в категории {}",
                    products.getTotalElements(), category);
            return products.map(productMapper::toDto);

        } catch (IllegalArgumentException e) {
            log.warn("Неверная категория: {}, возвращаем все активные товары", category);
            Page<Product> products = productRepository
                    .findByProductState(ProductState.ACTIVE, pageable);
            log.info("Найдено {} активных товаров (без фильтра по категории)",
                    products.getTotalElements());
            return products.map(productMapper::toDto);
        }
    }

    /**
     * GET /api/v1/shopping-store/{productId}
     * Получение товара по ID
     */
    public ProductDto getProduct(UUID productId) {
        log.info("Запрос товара по ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Товар не найден по ID: {}", productId);
                    return new ProductNotFoundException(
                            "Товар не найден с ID: " + productId
                    );
                });

        if (product.getProductState() == ProductState.DEACTIVATE) {
            log.warn("Товар {} деактивирован", productId);
            throw new ProductNotFoundException(
                    "Товар деактивирован: " + productId
            );
        }

        log.info("Товар успешно найден: {} (ID: {})",
                product.getProductName(), productId);
        return productMapper.toDto(product);
    }

    /**
     * PUT /api/v1/shopping-store
     * Создание нового товара
     */
    @Transactional
    public ProductDto createNewProduct(ProductDto productDto) {
        log.info("Создание нового товара: {}", productDto.getProductName());
        log.debug("Данные товара: наименование='{}', цена={}, категория={}",
                productDto.getProductName(),
                productDto.getPrice(),
                productDto.getProductCategory());

        Product product = productMapper.toEntityForCreate(productDto);
        product.setProductState(ProductState.ACTIVE);

        if (product.getQuantityState() == null) {
            log.debug("Статус количества не указан, устанавливаем ENDED");
            product.setQuantityState(QuantityState.ENDED);
        }

        Product saved = productRepository.save(product);
        log.info("Товар успешно создан с ID: {}, наименование: {}",
                saved.getProductId(), saved.getProductName());

        return productMapper.toDto(saved);
    }

    /**
     * POST /api/v1/shopping-store
     * Обновление существующего товара
     */
    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        if (productDto.getProductId() == null) {
            throw new IllegalArgumentException("ID товара должен быть указан для обновления");
        }

        log.info("Обновление товара с ID: {}", productDto.getProductId());

        Product existing = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Товар не найден с ID: " + productDto.getProductId()));

        boolean updated = false;

        if (productDto.getProductName() != null) {
            existing.setProductName(productDto.getProductName());
            updated = true;
        }
        if (productDto.getDescription() != null) {
            existing.setDescription(productDto.getDescription());
            updated = true;
        }
        if (productDto.getImageSrc() != null) {
            existing.setImageSrc(productDto.getImageSrc());
            updated = true;
        }
        if (productDto.getPrice() != null) {
            if (productDto.getPrice() <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            existing.setPrice(productDto.getPrice());
            updated = true;
        }
        if (productDto.getProductCategory() != null) {
            existing.setProductCategory(productDto.getProductCategory());
            updated = true;
        }
        if (productDto.getQuantityState() != null) {
            existing.setQuantityState(productDto.getQuantityState());
            updated = true;
        }

        if (!updated) {
            log.warn("Не передано ни одного поля для обновления товара {}", productDto.getProductId());
        }

        Product updatedProduct = productRepository.save(existing);
        return productMapper.toDto(updatedProduct);
    }

    /**
     * POST /api/v1/shopping-store/removeProductFromStore
     * Soft delete - деактивация товара
     */
    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        log.info("Деактивация товара (soft delete) с ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Товар не найден для деактивации: {}", productId);
                    return new ProductNotFoundException(
                            "Товар не найден с ID: " + productId
                    );
                });

        if (product.getProductState() == ProductState.DEACTIVATE) {
            log.warn("Товар {} уже деактивирован", productId);
            return true;
        }

        log.debug("Изменение статуса товара {}: {} → {}",
                productId, product.getProductState(), ProductState.DEACTIVATE);
        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);

        log.info("Товар успешно деактивирован: {}", productId);
        return true;
    }

    /**
     * POST /api/v1/shopping-store/quantityState
     * Обновление статуса количества (вызывается со стороны склада)
     */
    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        log.info("Обновление статуса количества для товара: {} → {}",
                request.getProductId(), request.getQuantityState());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> {
                    log.error("Товар не найден для обновления статуса количества: {}",
                            request.getProductId());
                    return new ProductNotFoundException(
                            "Товар не найден с ID: " + request.getProductId()
                    );
                });

        log.debug("Статус количества товара {} обновлен: {} → {}",
                request.getProductId(),
                product.getQuantityState(),
                request.getQuantityState());

        product.setQuantityState(request.getQuantityState());
        productRepository.save(product);

        log.info("Статус количества успешно обновлен для товара: {}", request.getProductId());
        return true;
    }
}