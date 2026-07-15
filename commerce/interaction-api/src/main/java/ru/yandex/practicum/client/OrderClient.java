package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.constants.ApiConstants;
import ru.yandex.practicum.enums.OrderState;

import java.util.UUID;

@FeignClient(name = "order")
public interface OrderClient {

    @PostMapping(ApiConstants.ORDER_UPDATE_STATE)
    void updateOrderState(
            @PathVariable UUID orderId,
            @RequestParam OrderState state
    );
}