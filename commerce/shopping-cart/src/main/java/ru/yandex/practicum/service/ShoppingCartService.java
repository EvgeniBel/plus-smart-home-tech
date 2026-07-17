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
        log.info("Запрос корзины для пользователя: {}", username);
        validateUsername(username);
        ShoppingCart cart = getOrCreateActiveCart(username);
        log.info("Корзина успешно получена для пользователя: {}", username);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
        log.info("Добавление товаров в корзину для пользователя: {}, количество позиций: {}",
                username, products != null ? products.size() : 0);
        validateUsername(username);
        if (products == null || products.isEmpty()) {
            ShoppingCart cart = getOrCreateActiveCart(username);
            return shoppingCartMapper.toDto(cart);
        }
        validateProducts(products);

        ShoppingCart cart = getOrCreateActiveCart(username);
        log.debug("Текущая корзина: {}, количество товаров: {}",
                cart.getId(), cart.getItems() != null ? cart.getItems().size() : 0);

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue().intValue();

            log.debug("Обработка товара: {}, количество: {}", productId, quantity);

            CartItem existingItem = findCartItem(cart, productId);

            if (existingItem != null) {
                int oldQuantity = existingItem.getQuantity();
                int newQuantity = oldQuantity + quantity;
                existingItem.setQuantity(newQuantity);
                log.info("Обновлено количество товара {} в корзине: {} → {}",
                        productId, oldQuantity, newQuantity);
            } else {
                CartItem newItem = createCartItem(cart, productId, quantity);
                cart.getItems().add(newItem);
                log.info("Добавлен новый товар {} в корзину с количеством {}", productId, quantity);
            }
        }

        ShoppingCart savedCart = shoppingCartRepository.save(cart);
        log.debug("Корзина сохранена, ID: {}", savedCart.getId());

        checkWarehouseAvailability(username, savedCart);

        log.info("Товары успешно добавлены в корзину для пользователя: {}", username);
        return shoppingCartMapper.toDto(savedCart);
    }

    @CircuitBreaker(name = "warehouseService", fallbackMethod = "warehouseFallback")
    protected void checkWarehouseAvailability(String username, ShoppingCart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.info("Корзина пуста, проверка склада не требуется");
            return;
        }
        log.info("Проверка наличия товаров на складе для пользователя: {}", username);

        ShoppingCartDto cartDto = shoppingCartMapper.toDto(cart);
        BookedProductsDto bookedProducts = warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);

        log.info("Проверка склада пройдена для пользователя: {}. Вес доставки: {} кг, Объем: {} м³, Хрупкие: {}",
                username,
                bookedProducts.getDeliveryWeight(),
                bookedProducts.getDeliveryVolume(),
                bookedProducts.getFragile());
    }

    protected void warehouseFallback(String username, ShoppingCart cart, Throwable throwable) {
        log.warn("Circuit Breaker сработал при проверке склада. Пользователь: {}, Ошибка: {}",
                username, throwable.getMessage());

        if (throwable instanceof ProductInShoppingCartLowQuantityInWarehouse) {
            log.error("На складе недостаточно товаров для пользователя: {}", username);
            throw (ProductInShoppingCartLowQuantityInWarehouse) throwable;
        }

        log.warn("Сервис склада временно недоступен. Товары добавлены без проверки наличия.");
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        log.info("Удаление товаров из корзины для пользователя: {}, количество: {}",
                username, productIds != null ? productIds.size() : 0);
        validateUsername(username);

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Список ID товаров не может быть пустым");
        }

        ShoppingCart cart = getActiveCart(username);
        log.debug("Найдена активная корзина, ID: {}", cart.getId());

        // Удаляем товары из коллекции (это синхронизирует состояние)
        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(item -> productIds.contains(item.getProductId()))
                .toList();

        if (!itemsToRemove.isEmpty()) {
            cart.getItems().removeAll(itemsToRemove);
            cartItemRepository.deleteAll(itemsToRemove);
            log.debug("Удалено товаров: {}", itemsToRemove.size());
        }

        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        return shoppingCartMapper.toDto(updatedCart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        log.info("Изменение количества товара для пользователя: {}, товар: {}, новое количество: {}",
                username, request.getProductId(), request.getNewQuantity());
        validateUsername(username);

        if (request.getProductId() == null) {
            throw new IllegalArgumentException("ID товара не может быть null");
        }

        if (request.getNewQuantity() == null || request.getNewQuantity() < 0) {
            throw new IllegalArgumentException("Количество должно быть неотрицательным");
        }

        ShoppingCart cart = getActiveCart(username);
        CartItem cartItem = findCartItem(cart, request.getProductId());

        if (cartItem == null) {
            throw new NoProductsInShoppingCartException(
                    String.format("Товар не найден в корзине: %s", request.getProductId()));
        }

        int newQuantity = request.getNewQuantity().intValue();

        if (newQuantity == 0) {
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
            log.info("Товар {} удален из корзины (количество установлено в 0)", request.getProductId());
        } else {
            int oldQuantity = cartItem.getQuantity();
            cartItem.setQuantity(newQuantity);
            log.info("Количество товара {} изменено: {} → {}",
                    request.getProductId(), oldQuantity, newQuantity);
        }

        ShoppingCart updatedCart = shoppingCartRepository.save(cart);
        return shoppingCartMapper.toDto(updatedCart);
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        log.info("Деактивация корзины для пользователя: {}", username);
        validateUsername(username);

        ShoppingCart cart = getActiveCart(username);
        cart.setActive(false);
        shoppingCartRepository.save(cart);

        log.info("Корзина успешно деактивирована для пользователя: {}", username);
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private ShoppingCart getOrCreateActiveCart(String username) {
        log.debug("Поиск активной корзины для пользователя: {}", username);
        return shoppingCartRepository
                .findByUsernameAndActiveTrue(username)
                .orElseGet(() -> {
                    log.debug("Активная корзина не найдена, создаем новую для пользователя: {}", username);
                    return createNewCart(username);
                });
    }

    private ShoppingCart getActiveCart(String username) {
        log.debug("Поиск активной корзины для пользователя: {}", username);
        return shoppingCartRepository
                .findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> {
                    log.warn("Активная корзина не найдена для пользователя: {}", username);
                    return new NotAuthorizedUserException(
                            String.format("Активная корзина не найдена для пользователя: %s", username));
                });
    }

    private ShoppingCart createNewCart(String username) {
        log.info("Создание новой корзины для пользователя: {}", username);
        ShoppingCart newCart = ShoppingCart.builder()
                .username(username)
                .active(true)
                .items(new ArrayList<>())
                .build();
        ShoppingCart saved = shoppingCartRepository.save(newCart);
        log.debug("Новая корзина создана, ID: {}", saved.getId());
        return saved;
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
            log.warn("Имя пользователя не может быть пустым");
            throw new NotAuthorizedUserException("Имя пользователя не может быть пустым");
        }
    }

    private void validateProducts(Map<UUID, Long> products) {
        if (products == null || products.isEmpty()) {
            log.warn("Список товаров не может быть пустым");
            throw new IllegalArgumentException("Список товаров не может быть пустым");
        }

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            if (entry.getKey() == null) {
                log.warn("ID товара не может быть null");
                throw new IllegalArgumentException("ID товара не может быть null");
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                log.warn("Количество должно быть положительным для товара: {}", entry.getKey());
                throw new IllegalArgumentException(
                        String.format("Количество должно быть положительным для товара: %s", entry.getKey()));
            }
        }
    }
}