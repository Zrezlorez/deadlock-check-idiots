package com.litovskiy.bot.tg;

import com.litovskiy.bot.KeyboardSpec;

public record CallbackResult(
    String answerText,
    String editText,
    KeyboardSpec editKeyboard
) {
    public static CallbackResult answer(String answerText) {
        return new CallbackResult(answerText, null, null);
    }

    public static CallbackResult edit(String editText, KeyboardSpec editKeyboard) {
        return new CallbackResult(null, editText, editKeyboard);
    }
}
