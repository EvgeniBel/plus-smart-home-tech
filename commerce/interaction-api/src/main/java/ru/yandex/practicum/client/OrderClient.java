package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import ru.yandex.practicum.enums.OrderState;

import java.util.UUID;

@FeignClient(name = "order")
public interface OrderClient {

    @PostMapping("/api/v1/order/{orderId}/state")
    void updateOrderState(@PathVariable UUID orderId, OrderState state);
}