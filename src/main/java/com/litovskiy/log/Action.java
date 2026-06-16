package com.litovskiy.log;

import com.litovskiy.service.ability.PlayerStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Action {
    GROW(
        LogTag.USED_GROW,
        PlayerStatus.NONE
    ),

    PRAY(
        LogTag.USED_PRAY,
        PlayerStatus.PRIEST
    ),

    JACKPOT(
        LogTag.USED_JACKPOT,
        PlayerStatus.LUDOMANIA
    ),

    TURTLE(
        LogTag.USED_TURTLE,
        PlayerStatus.SHAO_LIN
    ),

    SLOW(
        LogTag.USED_SLOW,
        PlayerStatus.CRUD
    ),

    FUCK(
        LogTag.USED_FUCK,
        PlayerStatus.CRUD
    ),

    TRANSFER(
        LogTag.USED_TRANSFER,
        PlayerStatus.TRADER
    ),

    HELP(
        LogTag.USED_OTHER,
        PlayerStatus.NONE
    );

    private final LogTag logTag;
    private final PlayerStatus event;
}
