package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.ShoppingCartClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.model.OrderState;
import ru.yandex.practicum.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingCartClient shoppingCartClient;
    private final WarehouseClient warehouseClient;

    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request) {
        log.info("Создание нового заказа для пользователя: {}", request.getUsername());

        // Получаем корзину
        ShoppingCartDto cart = shoppingCartClient.getShoppingCart(request.getUsername());
        if (cart == null || cart.getProducts() == null || cart.getProducts().isEmpty()) {
            throw new NoSpecifiedProductInWarehouseException("Корзина пуста или не найдена");
        }

        // Проверяем наличие товаров на складе
        BookedProductsDto bookedProducts = warehouseClient.checkProductQuantityEnoughForShoppingCart(cart);

        // Создаем заказ
        Order order = Order.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .username(request.getUsername())
                .state(OrderState.NEW)
                .deliveryWeight(bookedProducts.getDeliveryWeight())
                .deliveryVolume(bookedProducts.getDeliveryVolume())
                .fragile(bookedProducts.getFragile())
                .country(request.getDeliveryAddress().getCountry())
                .city(request.getDeliveryAddress().getCity())
                .street(request.getDeliveryAddress().getStreet())
                .house(request.getDeliveryAddress().getHouse())
                .flat(request.getDeliveryAddress().getFlat())
                .build();

        order = orderRepository.save(order);
        log.info("Заказ создан с ID: {}", order.getOrderId());

        return mapToDto(order);
    }

    public OrderDto getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Заказ не найден: " + orderId));
        return mapToDto(order);
    }

    public List<OrderDto> getClientOrders(String username) {
        log.info("Получение заказов для пользователя: {}", username);
        List<Order> orders = orderRepository.findByUsername(username);
        return orders.stream().map(this::mapToDto).toList();
    }

    @Transactional
    public OrderDto payment(UUID orderId) {
        Order order = getOrderEntity(orderId);
        order.setState(OrderState.PAID);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        Order order = getOrderEntity(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        Order order = getOrderEntity(orderId);
        order.setState(OrderState.ASSEMBLED);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        Order order = getOrderEntity(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        Order order = getOrderEntity(orderId);
        order.setState(OrderState.DELIVERED);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        Order order = getOrderEntity(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        Order order = getOrderEntity(orderId);
        order.setState(OrderState.COMPLETED);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        Order order = getOrderEntity(request.getOrderId());
        order.setState(OrderState.PRODUCT_RETURNED);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        Order order = getOrderEntity(orderId);
        // Расчет общей стоимости
        double productPrice = order.getProducts().values().stream().mapToDouble(Long::doubleValue).sum();
        double totalPrice = productPrice + (order.getDeliveryPrice() != null ? order.getDeliveryPrice() : 0);
        order.setProductPrice(productPrice);
        order.setTotalPrice(totalPrice);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        Order order = getOrderEntity(orderId);
        // Простой расчет стоимости доставки
        double deliveryPrice = 500.0; // Базовая стоимость
        order.setDeliveryPrice(deliveryPrice);
        order = orderRepository.save(order);
        return mapToDto(order);
    }

    private Order getOrderEntity(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Заказ не найден: " + orderId));
    }

    private OrderDto mapToDto(Order order) {
        AddressDto address = AddressDto.builder()
                .country(order.getCountry())
                .city(order.getCity())
                .street(order.getStreet())
                .house(order.getHouse())
                .flat(order.getFlat())
                .build();

        return OrderDto.builder()
                .orderId(order.getOrderId())
                .shoppingCartId(order.getShoppingCartId())
                .products(order.getProducts())
                .paymentId(order.getPaymentId())
                .deliveryId(order.getDeliveryId())
                .state(order.getState())
                .deliveryWeight(order.getDeliveryWeight())
                .deliveryVolume(order.getDeliveryVolume())
                .fragile(order.getFragile())
                .totalPrice(order.getTotalPrice())
                .deliveryPrice(order.getDeliveryPrice())
                .productPrice(order.getProductPrice())
                .deliveryAddress(address)
                .build();
    }
}