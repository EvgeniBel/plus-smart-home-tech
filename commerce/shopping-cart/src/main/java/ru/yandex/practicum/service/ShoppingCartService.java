package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
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

    /**
     * Получить актуальную корзину для авторизованного пользователя
     */
    @Transactional(readOnly = true)
    public ShoppingCartDto getShoppingCart(String username) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);
        log.info("Retrieved shopping cart for user: {}", username);

        return shoppingCartMapper.toDto(cart);
    }

    /**
     * Добавить товар в корзину
     */
    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> products) {
        validateUsername(username);
        validateProducts(products);

        ShoppingCart cart = getOrCreateActiveCart(username);

        for (Map.Entry<UUID, Integer> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            // Проверяем, есть ли уже такой товар в корзине
            CartItem existingItem = findCartItem(cart, productId);

            if (existingItem != null) {
                // Обновляем количество существующего товара
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                log.info("Updated quantity for product {} in cart: new quantity {}",
                        productId, existingItem.getQuantity());
            } else {
                // Создаем новый элемент корзины
                CartItem newItem = createCartItem(cart, productId, quantity);
                cart.getItems().add(newItem);
                log.info("Added new product {} to cart with quantity {}", productId, quantity);
            }
        }

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        log.info("Products added to cart for user: {}", username);

        return shoppingCartMapper.toDto(savedCart);
    }

    /**
     * Удалить товары из корзины
     */
    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        validateUsername(username);

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs list cannot be empty");
        }

        ShoppingCart cart = getActiveCart(username);

        // Проверяем, есть ли хотя бы один товар из списка в корзине
        boolean hasAnyProduct = false;
        for (CartItem item : cart.getItems()) {
            if (productIds.contains(item.getProductId())) {
                hasAnyProduct = true;
                break;
            }
        }

        if (!hasAnyProduct) {
            throw new NoProductsInShoppingCartException(
                    "None of the specified products found in the cart"
            );
        }

        // Удаляем товары
        cartItemRepository.deleteByCartAndProductIdIn(cart, productIds);

        // Обновляем корзину
        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        log.info("Products removed from cart for user: {}", username);

        return shoppingCartMapper.toDto(updatedCart);
    }

    /**
     * Изменить количество товара в корзине
     */
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
            // Если количество 0, удаляем товар из корзины
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

    /**
     * Деактивация корзины
     */
    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        validateUsername(username);

        ShoppingCart cart = getActiveCart(username);
        cart.setActive(false);
        shoppingCartRepository.save(cart);

        log.info("Shopping cart deactivated for user: {}", username);
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    /**
     * Получить или создать активную корзину для пользователя
     */
    private ShoppingCart getOrCreateActiveCart(String username) {
        ShoppingCart cart = shoppingCartRepository.findByUsernameAndActiveTrue(username).orElse(null);

        if (cart == null) {
            cart = createNewCart(username);
        }

        return cart;
    }

    /**
     * Получить активную корзину пользователя
     */
    private ShoppingCart getActiveCart(String username) {
        ShoppingCart cart = shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NotAuthorizedUserException(
                        "No active cart found for user: " + username
                ));
        return cart;
    }

    /**
     * Создать новую корзину для пользователя
     */
    private ShoppingCart createNewCart(String username) {
        ShoppingCart newCart = new ShoppingCart();
        newCart.setUsername(username);
        newCart.setActive(true);
        newCart.setItems(new ArrayList<>());

        return shoppingCartRepository.save(newCart);
    }

    /**
     * Найти элемент корзины по ID товара
     */
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

    /**
     * Создать новый элемент корзины
     */
    private CartItem createCartItem(ShoppingCart cart, UUID productId, Integer quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPriceAtAddition(0.0); // Позже будет обновляться из сервиса товаров
        return item;
    }

    /**
     * Валидация имени пользователя
     */
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new NotAuthorizedUserException("Username cannot be empty");
        }
    }

    /**
     * Валидация списка товаров
     */
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