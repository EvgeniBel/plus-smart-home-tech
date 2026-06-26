package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hub_id")
    private String hubId;

    @Column(name = "name")
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "scenario_conditions")
    @MapKeyColumn(name = "sensor_id")
    private Map<String, Condition> conditions = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "scenario_actions")
    @MapKeyColumn(name = "sensor_id")
    private Map<String, Action> actions = new HashMap<>();
}