package com.litovskiy.log.metadata;

import com.litovskiy.log.LogMetadata;

public record TransferLogMetadata(
    double cost
) implements LogMetadata {
}
