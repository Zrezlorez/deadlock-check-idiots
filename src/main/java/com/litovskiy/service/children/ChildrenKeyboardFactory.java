package com.litovskiy.service.children;

import com.litovskiy.bot.ButtonSpec;
import com.litovskiy.bot.KeyboardSpec;

import java.time.LocalDate;

public final class ChildrenKeyboardFactory {
    private ChildrenKeyboardFactory() {
    }

    public static KeyboardSpec care(long childrenId, LocalDate careDate) {
        return KeyboardSpec.row(
            careButton("Покормить", childrenId, careDate, ChildrenAction.EAT),
            careButton("Уложить спать", childrenId, careDate, ChildrenAction.SLEEP),
            careButton("Поиграть", childrenId, careDate, ChildrenAction.PLAY)
        );
    }

    private static ButtonSpec careButton(
        String text,
        long childrenId,
        LocalDate careDate,
        ChildrenAction action
    ) {
        return new ButtonSpec(
            text,
            new ChildrenCareCallback(childrenId, careDate, action).encode()
        );
    }
}
