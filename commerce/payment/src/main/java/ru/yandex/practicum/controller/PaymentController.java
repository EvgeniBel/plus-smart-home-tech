package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Рассчитать стоимость товаров
     * POST /api/v1/payment/productCost
     */
    @PostMapping("/productCost")
    public double calculateProductCost(@Valid @RequestBody OrderDto order) {
        log.info("POST /api/v1/payment/productCost - orderId: {}", order.getOrderId());
        return paymentService.calculateProductCost(order);
    }

    /**
     * Рассчитать полную стоимость заказа
     * POST /api/v1/payment/totalCost
     */
    @PostMapping("/totalCost")
    public double calculateTotalCost(@Valid @RequestBody OrderDto order) {
        log.info("POST /api/v1/payment/totalCost - orderId: {}", order.getOrderId());
        return paymentService.calculateTotalCost(order);
    }

    /**
     * Создать оплату для заказа
     * POST /api/v1/payment
     */
    @PostMapping
    public PaymentDto createPayment(@Valid @RequestBody OrderDto order) {
        log.info("POST /api/v1/payment - orderId: {}", order.getOrderId());
        return paymentService.createPayment(order);
    }

    /**
     * Подтвердить успешную оплату
     * POST /api/v1/payment/refund
     */
    @PostMapping("/refund")
    public void processSuccessfulPayment(@RequestBody UUID paymentId) {
        log.info("POST /api/v1/payment/refund - paymentId: {}", paymentId);
        paymentService.processSuccessfulPayment(paymentId);
    }

    /**
     * Обработать отказ в оплате
     * POST /api/v1/payment/failed
     */
    @PostMapping("/failed")
    public void processFailedPayment(@RequestBody UUID paymentId) {
        log.info("POST /api/v1/payment/failed - paymentId: {}", paymentId);
        paymentService.processFailedPayment(paymentId);
    }
}