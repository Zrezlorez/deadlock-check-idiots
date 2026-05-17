package com.litovskiy.util;

public record GrowthCalculation(
    double oldValue,
    double newValue,
    double diff,

    GrowOutcome outcome,

    double baseGrowth,
    double activityBonus,
    double slowdown,

    double failChance,
    double critChance,

    double growthModifier,

    double modifierBeforeOutcome,
    double modifierAfterOutcome,
    double finalModifier
) {
}
