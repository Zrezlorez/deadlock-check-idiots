package com.litovskiy.service.children;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public record ChildrenCareCallback(
    long childrenId,
    LocalDate careDate,
    ChildrenAction action
) {
    private static final String PREFIX = "children";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    public String encode() {
        return "%s:%d:%s:%s".formatted(
            PREFIX,
            childrenId,
            careDate.format(DATE_FORMATTER),
            action.name().toLowerCase(Locale.ROOT)
        );
    }

    public static boolean matches(String rawValue) {
        return rawValue != null && rawValue.startsWith(PREFIX + ":");
    }

    public static Optional<ChildrenCareCallback> decode(String rawValue) {
        String[] parts = rawValue == null ? new String[0] : rawValue.split(":");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ChildrenCareCallback(
                Long.parseLong(parts[1]),
                LocalDate.parse(parts[2], DATE_FORMATTER),
                ChildrenAction.valueOf(parts[3].toUpperCase(Locale.ROOT))
            ));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
