package ru.yandex.practicum.grpc.handler.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.handler.HubEventHandler;
import ru.yandex.practicum.service.HubEventService;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioAddedEventHandler implements HubEventHandler {

    private final HubEventService hubEventService;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioAddedEventProto scenarioAdded = event.getScenarioAdded();
        log.info("Обработка события добавления сценария: hubId={}, name={}, conditions={}, actions={}",
                event.getHubId(),
                scenarioAdded.getName(),
                scenarioAdded.getConditionCount(),
                scenarioAdded.getActionCount());

        hubEventService.sendHubEventFromProto(event);
    }
}