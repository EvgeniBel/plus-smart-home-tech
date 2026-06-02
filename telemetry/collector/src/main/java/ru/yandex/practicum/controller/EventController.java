package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.hub.HubEventDto;
import ru.yandex.practicum.dto.sensor.SensorEventDto;
import ru.yandex.practicum.service.HubEventService;
import ru.yandex.practicum.service.SensorEventService;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final SensorEventService sensorEventService;
    private final HubEventService hubEventService;

    @PostMapping("/sensors")
    public String handleSensorEvent(@Valid @RequestBody SensorEventDto event) {
        log.info("Получено событие датчика: type={}, id={}, hubId={}",
                event.getType(), event.getId(), event.getHubId());
        sensorEventService.sendSensorEvent(event);
        return "Получено событие датчика: " + event.getType();
    }

    @PostMapping("/hubs")
    public String handleHubEvent(@Valid @RequestBody HubEventDto event) {
        log.info("Получено событие Хаба: type={}, hubId={}",
                event.getType(), event.getHubId());
        hubEventService.sendHubEvent(event);
        return "Получено событие Хаба: " + event.getType();
    }
}