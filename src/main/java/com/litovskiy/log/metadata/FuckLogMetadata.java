package com.litovskiy.log.metadata;

import com.litovskiy.log.LogMetadata;

public record FuckLogMetadata(
    double oldFailChance,
    double newFailChance,
    double cost
) implements LogMetadata {
}
