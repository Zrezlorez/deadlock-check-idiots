package com.litovskiy.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum TelegramCallbackMessage {
    ACCEPT("accept"),
    ABORT("abort");

    private static final Map<String, TelegramCallbackMessage> messages =
        Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                TelegramCallbackMessage::getMessage,
                Function.identity()
            ));

    private final String message;

    public static TelegramCallbackMessage fromMessage(String message) {
        TelegramCallbackMessage value = messages.get(message);

        if (value == null) {
            throw new IllegalArgumentException("Unknown callback message: " + message);
        }

        return value;
    }
}
