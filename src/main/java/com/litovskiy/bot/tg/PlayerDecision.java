package com.litovskiy.bot.tg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PlayerDecision {
    ACCEPT("выбрал оставить"),
    DECLINE("выбрал аборт");

    private final String text;
}
