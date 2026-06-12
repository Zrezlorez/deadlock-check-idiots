package com.litovskiy.bot;

import java.util.Arrays;
import java.util.List;

public record KeyboardSpec(
    List<List<ButtonSpec>> rows
) {
    public static KeyboardSpec row(ButtonSpec... buttons) {
        return new KeyboardSpec(List.of(List.copyOf(Arrays.asList(buttons))));
    }
}
