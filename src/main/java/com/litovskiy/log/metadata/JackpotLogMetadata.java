package com.litovskiy.log.metadata;

import com.litovskiy.log.LogMetadata;

public record JackpotLogMetadata(
    double oldCritChance,
    double newCritChance,
    double oldFailChance,
    double newFailChance,
    double critDiff,
    double failDiff
) implements LogMetadata {
}