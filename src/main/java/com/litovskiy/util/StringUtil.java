package com.litovskiy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StringUtil {

    private StringUtil() {
    }

    public static String convertValue(double value) {
        if (value > 100_000_000) {
            return round(value / 100_000_000) + " к км";
        }

        if (value > 100_000) {
            return round(value / 100_000) + " км";
        }

        if (value > 100) {
            return round(value / 100) + " м";
        }

        return round(value) + " см";
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    public static double round(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
