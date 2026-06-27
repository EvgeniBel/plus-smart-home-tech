package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.ScenarioCondition;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, Long> {
}