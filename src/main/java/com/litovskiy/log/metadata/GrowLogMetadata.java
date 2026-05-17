package com.litovskiy.log.metadata;

import com.litovskiy.log.LogMetadata;
import com.litovskiy.util.GrowOutcome;

public record GrowLogMetadata(
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
) implements LogMetadata {
}
