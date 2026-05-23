package com.litovskiy.log.metadata;

import com.litovskiy.log.LogMetadata;

public record PrayLogMetadata(
    double oldFailChance,
    double newFailChance,
    double failDiff
) implements LogMetadata {
}
