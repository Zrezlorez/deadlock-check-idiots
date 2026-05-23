package com.litovskiy.log;

public enum LogTag {

    // Growth outcome
    NORMAL_GROWTH,
    CRITICAL_GROWTH,
    FAILED_GROWTH,

    HIGH_GROWTH,
    LOW_GROWTH,

    LUCKY_STREAK,
    FAIL_STREAK,
    NORMAL_STREAK,

    // Action category
    AGGRESSIVE_ACTION,
    SUPPORTIVE_ACTION,
    SELF_BUFF_ACTION,
    RISK_ACTION,

    // Commands
    USED_GROW,
    USED_JACKPOT,
    USED_FUCK,
    USED_SLOW,
    USED_TURTLE,
    USED_PRAY,
    USED_TRANSFER,
    USED_OTHER,

    // Target / ranking context
    LEADER_TARGETED,
    TOP_3_TARGETED,
    UNDERDOG_TARGETED,
    HELPED_UNDERDOG,
    TARGETED_SAME_PLAYER,

    // Social / transfer
    LARGE_TRANSFER,
    RECEIVED_SUPPORT,

    // Cooldown / invalid actions
    COOLDOWN_BLOCKED,
    INVALID_TARGET,
    EFFECT_ALREADY_PRESENT,

    // Cases / items
    OPENED_CASE,
    RECEIVED_COMMON,
    RECEIVED_RARE,
    RECEIVED_EPIC,
    RECEIVED_LEGENDARY,
    USED_CONSUMABLE,
    USED_CURSED_ITEM,

    // AI
    AI_BUFFED,
    AI_PUNISHED,
    AI_PROTECTED,
    AI_TARGETED
}