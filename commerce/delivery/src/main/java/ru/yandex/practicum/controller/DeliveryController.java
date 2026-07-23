package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.DeliveryCostRequest;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.service.DeliveryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery")
@Slf4j
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * Создать новую доставку
     * PUT /api/v1/delivery
     */
    @PutMapping
    public DeliveryDto createDelivery(@Valid @RequestBody DeliveryDto deliveryDto) {
        log.info("PUT /api/v1/delivery - orderId: {}", deliveryDto.getOrderId());
        return deliveryService.createDelivery(deliveryDto);
    }

    /**
     * Рассчитать стоимость доставки
     * POST /api/v1/delivery/cost
     */
    @PostMapping("/cost")
    public double calculateDeliveryCost(@Valid @RequestBody DeliveryCostRequest request) {
        log.info("POST /api/v1/delivery/cost - orderId: {}",
                request.getOrder().getOrderId());
        return deliveryService.calculateDeliveryCost(
                request.getOrder(),
                request.getFromAddress(),
                request.getToAddress()
        );
    }

    /**
     * Принять товары в доставку
     * POST /api/v1/delivery/picked
     */
    @PostMapping("/picked")
    public void pickupDelivery(@RequestBody UUID orderId) {
        log.info("POST /api/v1/delivery/picked - orderId: {}", orderId);
        deliveryService.pickupDelivery(orderId);
    }

    /**
     * Подтвердить успешную доставку
     * POST /api/v1/delivery/successful
     */
    @PostMapping("/successful")
    public void completeDelivery(@RequestBody UUID orderId) {
        log.info("POST /api/v1/delivery/successful - orderId: {}", orderId);
        deliveryService.completeDelivery(orderId);
    }

    /**
     * Отметить доставку как неудачную
     * POST /api/v1/delivery/failed
     */
    @PostMapping("/failed")
    public void failDelivery(@RequestBody UUID orderId) {
        log.info("POST /api/v1/delivery/failed - orderId: {}", orderId);
        deliveryService.failDelivery(orderId);
    }

    /**
     * Получить доставку по ID заказа
     * GET /api/v1/delivery/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public DeliveryDto getDeliveryByOrderId(@PathVariable UUID orderId) {
        log.info("GET /api/v1/delivery/order/{}", orderId);
        return deliveryService.getDeliveryByOrderId(orderId);
    }
}