package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.enums.DeliveryState;
import ru.yandex.practicum.enums.OrderState;
import ru.yandex.practicum.exception.NoDeliveryFoundException;
import ru.yandex.practicum.mapper.DeliveryMapper;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.repository.DeliveryRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    private static final double BASE_COST = 5.0;

    /**
     * Создать новую доставку
     * PUT /api/v1/delivery
     */
    @Transactional
    public DeliveryDto createDelivery(DeliveryDto deliveryDto) {
        log.info("Создание доставки для заказа: {}", deliveryDto.getOrderId());

        deliveryDto.setDeliveryState(DeliveryState.CREATED);
        Delivery delivery = deliveryMapper.toEntity(deliveryDto);
        Delivery saved = deliveryRepository.save(delivery);

        log.info("Доставка создана: {}", saved.getDeliveryId());
        return deliveryMapper.toDto(saved);
    }

    /**
     * Рассчитать стоимость доставки
     * POST /api/v1/delivery/cost
     */
    public double calculateDeliveryCost(OrderDto order, AddressDto fromAddress, AddressDto toAddress) {
        log.info("Расчёт стоимости доставки для заказа: {}", order.getOrderId());

        double cost = BASE_COST;
        log.debug("Базовая стоимость: {}", cost);

        // 1. Учитываем адрес склада
        double multiplier = 1.0;
        if (fromAddress.getStreet() != null && fromAddress.getStreet().contains("ADDRESS_2")) {
            multiplier = 2.0;
        }
        cost = cost + (BASE_COST * multiplier);
        log.debug("После учёта адреса склада: {}", cost);

        // 2. Учитываем хрупкость
        if (Boolean.TRUE.equals(order.getFragile())) {
            cost = cost + (cost * 0.2);
        }
        log.debug("После учёта хрупкости: {}", cost);

        // 3. Учитываем вес
        if (order.getDeliveryWeight() != null) {
            cost = cost + (order.getDeliveryWeight() * 0.3);
        }
        log.debug("После учёта веса: {}", cost);

        // 4. Учитываем объём
        if (order.getDeliveryVolume() != null) {
            cost = cost + (order.getDeliveryVolume() * 0.2);
        }
        log.debug("После учёта объёма: {}", cost);

        // 5. Учитываем адрес доставки
        if (fromAddress.getStreet() != null && !fromAddress.getStreet().equals(toAddress.getStreet())) {
            cost = cost + (cost * 0.2);
        }
        log.debug("После учёта адреса доставки: {}", cost);

        log.info("Итоговая стоимость доставки: {}", cost);
        return cost;
    }

    /**
     * Принять товары в доставку (статус IN_PROGRESS)
     * POST /api/v1/delivery/picked
     */
    @Transactional
    public void pickupDelivery(UUID orderId) {
        log.info("Приём товаров в доставку для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка не найдена для заказа: " + orderId));

        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        // Обновляем статус заказа
        orderClient.updateOrderState(orderId, OrderState.ASSEMBLED);

        ShippedToDeliveryRequest request = ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(delivery.getDeliveryId())
                .build();
        warehouseClient.shippedToDelivery(request);

        log.info("Доставка {} переведена в статус IN_PROGRESS", delivery.getDeliveryId());
    }

    /**
     * Подтвердить успешную доставку
     * POST /api/v1/delivery/successful
     */
    @Transactional
    public void completeDelivery(UUID orderId) {
        log.info("Подтверждение успешной доставки для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка не найдена для заказа: " + orderId));

        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        // Обновляем статус заказа
        orderClient.updateOrderState(orderId, OrderState.DELIVERED);

        log.info("Доставка {} успешно завершена", delivery.getDeliveryId());
    }

    /**
     * Отметить доставку как неудачную
     * POST /api/v1/delivery/failed
     */
    @Transactional
    public void failDelivery(UUID orderId) {
        log.info("Отказ в доставке для заказа: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка не найдена для заказа: " + orderId));

        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        // Обновляем статус заказа
        orderClient.updateOrderState(orderId, OrderState.DELIVERY_FAILED);

        log.info("Доставка {} отмечена как неудачная", delivery.getDeliveryId());
    }

    /**
     * Получить доставку по ID заказа
     */
    public DeliveryDto getDeliveryByOrderId(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка не найдена для заказа: " + orderId));
        return deliveryMapper.toDto(delivery);
    }
}