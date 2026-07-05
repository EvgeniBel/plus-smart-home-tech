package ru.yandex.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShoppingCartMapper {

    // Преобразование ShoppingCart в ShoppingCartDto
    public ShoppingCartDto toDto(ShoppingCart cart) {
        if (cart == null) {
            return null;
        }

        ShoppingCartDto dto = new ShoppingCartDto();
        dto.setShoppingCartId(cart.getId());
        dto.setProducts(convertItemsToMap(cart));

        return dto;
    }

    //Преобразование списка элементов корзины в Map<UUID, Integer>
    private Map<UUID, Integer> convertItemsToMap(ShoppingCart cart) {
        Map<UUID, Integer> products = new HashMap<>();

        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            for (CartItem item : cart.getItems()) {
                products.put(item.getProductId(), item.getQuantity());
            }
        }

        return products;
    }
}