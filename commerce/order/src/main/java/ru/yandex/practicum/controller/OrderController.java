package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ProductReturnRequest;
import ru.yandex.practicum.service.OrderService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDto>> getClientOrders(@RequestParam String username) {
        log.info("GET /api/v1/order - username: {}", username);
        return ResponseEntity.ok(orderService.getClientOrders(username));
    }

    @PutMapping
    public ResponseEntity<OrderDto> createNewOrder(@Valid @RequestBody CreateNewOrderRequest request) {
        log.info("PUT /api/v1/order - username: {}", request.getUsername());
        return ResponseEntity.ok(orderService.createNewOrder(request));
    }

    @PostMapping("/payment")
    public ResponseEntity<OrderDto> payment(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/payment - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.payment(orderId));
    }

    @PostMapping("/payment/failed")
    public ResponseEntity<OrderDto> paymentFailed(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/payment/failed - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.paymentFailed(orderId));
    }

    @PostMapping("/assembly")
    public ResponseEntity<OrderDto> assembly(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/assembly - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.assembly(orderId));
    }

    @PostMapping("/assembly/failed")
    public ResponseEntity<OrderDto> assemblyFailed(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/assembly/failed - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.assemblyFailed(orderId));
    }

    @PostMapping("/delivery")
    public ResponseEntity<OrderDto> delivery(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/delivery - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.delivery(orderId));
    }

    @PostMapping("/delivery/failed")
    public ResponseEntity<OrderDto> deliveryFailed(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/delivery/failed - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.deliveryFailed(orderId));
    }

    @PostMapping("/completed")
    public ResponseEntity<OrderDto> complete(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/completed - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.complete(orderId));
    }

    @PostMapping("/return")
    public ResponseEntity<OrderDto> productReturn(@Valid @RequestBody ProductReturnRequest request) {
        log.info("POST /api/v1/order/return - orderId: {}", request.getOrderId());
        return ResponseEntity.ok(orderService.productReturn(request));
    }

    @PostMapping("/calculate/total")
    public ResponseEntity<OrderDto> calculateTotalCost(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/calculate/total - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.calculateTotalCost(orderId));
    }

    @PostMapping("/calculate/delivery")
    public ResponseEntity<OrderDto> calculateDeliveryCost(@RequestBody UUID orderId) {
        log.info("POST /api/v1/order/calculate/delivery - orderId: {}", orderId);
        return ResponseEntity.ok(orderService.calculateDeliveryCost(orderId));
    }
}