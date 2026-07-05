package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCart(ShoppingCart cart);

    void deleteByCartAndProductIdIn(ShoppingCart cart, List<UUID> productIds);

    Optional<CartItem> findByCartAndProductId(ShoppingCart cart, UUID productId);
}