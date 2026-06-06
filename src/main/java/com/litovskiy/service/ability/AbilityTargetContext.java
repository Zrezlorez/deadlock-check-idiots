package com.litovskiy.service.ability;

import com.litovskiy.entity.Player;

import java.time.LocalDateTime;

public record AbilityTargetContext(
    boolean success,
    String rejectionMessage,
    LocalDateTime now,
    Player actor,
    Player target
) {

    public static AbilityTargetContext success(LocalDateTime now, Player actor, Player target) {
        return new AbilityTargetContext(true, null, now, actor, target);
    }

    public static AbilityTargetContext rejected(LocalDateTime now, Player actor, Player target, String message) {
        return new AbilityTargetContext(false, message, now, actor, target);
    }
    public static AbilityTargetContext rejected(String message) {
        return new AbilityTargetContext(false, message, null, null, null);
    }
}
