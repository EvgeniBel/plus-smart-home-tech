package ru.yandex.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.CartItemDto;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ShoppingCartMapper {

    /**
     * Преобразование ShoppingCart в ShoppingCartDto
     */
    public ShoppingCartDto toDto(ShoppingCart cart) {
        if (cart == null) return null;

        Map<UUID, Long> products = new HashMap<>();
        for (CartItem item : cart.getItems()) {
            products.put(item.getProductId(), (long) item.getQuantity());
        }

        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getId())
                .products(products)
                .build();
    }

    /**
     * Преобразование списка элементов корзины в List<CartItemDto>
     */
    private List<CartItemDto> convertItemsToList(ShoppingCart cart) {
        List<CartItemDto> products = new ArrayList<>();

        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            for (CartItem item : cart.getItems()) {
                CartItemDto dto = CartItemDto.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build();
                products.add(dto);
            }
        }

        return products;
    }

    /**
     * Преобразование ShoppingCartDto в ShoppingCart (если нужно)
     */
    public ShoppingCart toEntity(ShoppingCartDto dto) {
        if (dto == null) {
            return null;
        }

        ShoppingCart cart = new ShoppingCart();
        cart.setId(dto.getShoppingCartId());
        // items будут добавлены отдельно в сервисе

        return cart;
    }

    /**
     * Создание CartItem из CartItemDto
     */
    public CartItem toCartItemEntity(CartItemDto dto, ShoppingCart cart) {
        if (dto == null || cart == null) {
            return null;
        }

        return CartItem.builder()
                .cart(cart)
                .productId(dto.getProductId())
                .quantity(dto.getQuantity())
                .build();
    }
}