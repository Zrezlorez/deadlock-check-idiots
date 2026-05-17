package com.litovskiy.log;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Action {
    GROW(LogTag.USED_GROW),
    PRAY(LogTag.USED_PRAY),
    JACKPOT(LogTag.USED_JACKPOT),
    TURTLE(LogTag.USED_TURTLE),
    SLOW(LogTag.USED_SLOW),
    FUCK(LogTag.USED_FUCK),
    TRANSFER(LogTag.USED_TRANSFER),
    HELP(LogTag.USED_OTHER);

    private final LogTag logTag;
}
