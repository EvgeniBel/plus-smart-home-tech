package ru.yandex.practicum.model;

import java.util.function.BiPredicate;

public enum ConditionOperation {
    EQUALS((actual, expected) -> Double.compare(actual, expected) == 0),
    GREATER_THAN((actual, expected) -> actual > expected),
    LOWER_THAN((actual, expected) -> actual < expected);

    private final BiPredicate<Double, Double> predicate;

    ConditionOperation(BiPredicate<Double, Double> predicate) {
        this.predicate = predicate;
    }

    public BiPredicate<Double, Double> getPredicate() {
        return predicate;
    }
}