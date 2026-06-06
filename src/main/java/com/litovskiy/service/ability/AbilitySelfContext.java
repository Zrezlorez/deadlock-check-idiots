package com.litovskiy.service.ability;

import com.litovskiy.entity.Player;

import java.time.LocalDateTime;

public record AbilitySelfContext(
    boolean success,
    String rejectionMessage,
    LocalDateTime now,
    Player player
) {

    public static AbilitySelfContext success(LocalDateTime now, Player player) {
        return new AbilitySelfContext(true, null, now, player);
    }

    public static AbilitySelfContext rejected(String message) {
        return new AbilitySelfContext(false, message, null, null);
    }
}