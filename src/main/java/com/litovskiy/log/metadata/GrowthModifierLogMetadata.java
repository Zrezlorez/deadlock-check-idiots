package com.litovskiy.log.metadata;

import com.litovskiy.log.LogMetadata;

public record GrowthModifierLogMetadata(
    double oldGrowthModifier,
    double newGrowthModifier,
    double diff
) implements LogMetadata {
}
