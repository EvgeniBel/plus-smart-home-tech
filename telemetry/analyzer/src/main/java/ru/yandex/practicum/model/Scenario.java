package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "scenarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hub_id", "name"})
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "hub_id")
    String hubId;

    @Column(name = "name")
    String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @MapKeyColumn(table = "scenario_conditions", name = "sensor_id")
    @JoinTable(name = "scenario_conditions",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "condition_id"))
    Map<String, Condition> conditions = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @MapKeyColumn(table = "scenario_actions", name = "sensor_id")
    @JoinTable(name = "scenario_actions",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "action_id"))
    Map<String, Action> actions = new HashMap<>();
}