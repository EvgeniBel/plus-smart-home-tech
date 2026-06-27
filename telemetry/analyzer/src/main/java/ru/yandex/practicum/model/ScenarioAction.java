package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "scenario_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"scenario", "sensor", "action"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScenarioAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "scenario_id")
    Scenario scenario;

    @ManyToOne
    @JoinColumn(name = "sensor_id")
    Sensor sensor;

    @ManyToOne
    @JoinColumn(name = "action_id")
    Action action;
}