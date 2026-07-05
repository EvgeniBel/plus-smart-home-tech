package ru.yandex.practicum.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.mapper.ShoppingCartMapper;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ShoppingCartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShoppingCartMapper shoppingCartMapper;
    private final WarehouseClient warehouseClient;

    @Transactional(readOnly = true)
    public ShoppingCartDto getShoppingCart(String username) {
        validateUsername(username);
        ShoppingCart cart = getOrCreateActiveCart(username);
        log.info("Retrieved shopping cart for user: {}", username);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> products) {
        validateUsername(username);
        validateProducts(products);

        // Получаем текущую корзину
        ShoppingCart cart = getOrCreateActiveCart(username);

        // Добавляем товары в корзину
        for (Map.Entry<UUID, Integer> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            CartItem existingItem = findCartItem(cart, productId);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                log.info("Updated quantity for product {} in cart: new quantity {}",
                        productId, existingItem.getQuantity());
            } else {
                CartItem newItem = createCartItem(cart, productId, quantity);
                cart.getItems().add(newItem);
                log.info("Added new product {} to cart with quantity {}", productId, quantity);
            }
        }

        // Сохраняем корзину
        ShoppingCart savedCart = shoppingCartRepository.save(cart);

        // Проверяем наличие товаров на складе через отдельный метод с Circuit Breaker
        checkWarehouseAvailability(username, savedCart);

        log.info("Products added to cart for user: {}", username);
        return shoppingCartMapper.toDto(savedCart);
    }

    /**
     * Проверка наличия товаров на складе с Circuit Breaker
     */
    @CircuitBreaker(name = "warehouseService", fallbackMethod = "warehouseFallback")
    protected void checkWarehouseAvailability(String username, ShoppingCart cart) {
        ShoppingCartDto cartDto = shoppingCartMapper.toDto(cart);
        BookedProductsDto bookedProducts = warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);
        log.info("Warehouse check passed for user: {}. Delivery weight: {}, volume: {}, fragile: {}",
                username, bookedProducts.getDeliveryWeight(),
                bookedProducts.getDeliveryVolume(), bookedProducts.getFragile());
    }

    /**
     * Fallback метод для Circuit Breaker
     */
    protected void warehouseFallback(String username, ShoppingCart cart, Throwable throwable) {
        log.warn("Circuit breaker fallback for warehouse check. User: {}, Error: {}",
                username, throwable.getMessage());

        // Если ошибка не связана с недостатком товаров, просто логируем
        if (throwable instanceof ProductInShoppingCartLowQuantityInWarehouse) {
            throw (ProductInShoppingCartLowQuantityInWarehouse) throwable;
        }

        // В остальных случаях - логируем и продолжаем (warehouse недоступен)
        log.warn("Warehouse service is unavailable. Products added without availability check.");
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        validateUsername(username);

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs list cannot be empty");
        }

        ShoppingCart cart = getActiveCart(username);

        boolean hasAnyProduct = cart.getItems().stream()
                .anyMatch(item -> productIds.contains(item.getProductId()));

        if (!hasAnyProduct) {
            throw new NoProductsInShoppingCartException(
                    "None of the specified products found in the cart"
            );
        }

        cartItemRepository.deleteByCartAndProductIdIn(cart, productIds);

        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        log.info("Products removed from cart for user: {}", username);

        return shoppingCartMapper.toDto(updatedCart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);

        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        if (request.getNewQuantity() == null || request.getNewQuantity() < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }

        ShoppingCart cart = getActiveCart(username);

        CartItem cartItem = findCartItem(cart, request.getProductId());
        if (cartItem == null) {
            throw new NoProductsInShoppingCartException(
                    "Product not found in cart: " + request.getProductId()
            );
        }

        if (request.getNewQuantity() == 0) {
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
            log.info("Product {} removed from cart (quantity set to 0)", request.getProductId());
        } else {
            cartItem.setQuantity(request.getNewQuantity());
            log.info("Product {} quantity updated to {}",
                    request.getProductId(), request.getNewQuantity());
        }

        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        log.info("Product quantity changed for user: {}", username);

        return shoppingCartMapper.toDto(updatedCart);
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        validateUsername(username);

        ShoppingCart cart = getActiveCart(username);
        cart.setActive(false);
        shoppingCartRepository.save(cart);

        log.info("Shopping cart deactivated for user: {}", username);
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private ShoppingCart getOrCreateActiveCart(String username) {
        return shoppingCartRepository
                .findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createNewCart(username));
    }

    private ShoppingCart getActiveCart(String username) {
        return shoppingCartRepository
                .findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NotAuthorizedUserException(
                        "No active cart found for user: " + username
                ));
    }

    private ShoppingCart createNewCart(String username) {
        ShoppingCart newCart = ShoppingCart.builder()
                .username(username)
                .active(true)
                .items(new ArrayList<>())
                .build();
        return shoppingCartRepository.save(newCart);
    }

    private CartItem findCartItem(ShoppingCart cart, UUID productId) {
        if (cart == null || cart.getItems() == null) {
            return null;
        }

        for (CartItem item : cart.getItems()) {
            if (item.getProductId().equals(productId)) {
                return item;
            }
        }

        return null;
    }

    private CartItem createCartItem(ShoppingCart cart, UUID productId, Integer quantity) {
        return CartItem.builder()
                .cart(cart)
                .productId(productId)
                .quantity(quantity)
                .priceAtAddition(0.0)
                .build();
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new NotAuthorizedUserException("Username cannot be empty");
        }
    }

    private void validateProducts(Map<UUID, Integer> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Products map cannot be empty");
        }

        for (Map.Entry<UUID, Integer> entry : products.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Product ID cannot be null");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be positive for product: " + entry.getKey()
                );
            }
        }
    }
}