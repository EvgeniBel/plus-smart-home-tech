package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "scenarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hub_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "hub_id")
    String hubId;

    @Column(name = "name")
    String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "scenario_conditions")
    @MapKeyColumn(name = "sensor_id")
    Map<String, Condition> conditions = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "scenario_actions")
    @MapKeyColumn(name = "sensor_id")
    Map<String, Action> actions = new HashMap<>();
}