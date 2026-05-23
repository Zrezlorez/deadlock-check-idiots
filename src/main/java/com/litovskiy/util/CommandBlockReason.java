package com.litovskiy.util;

public record CommandBlockReason(
    boolean allowed,
    String message
) {

    public static CommandBlockReason createAllowed() {
        return new CommandBlockReason(true, null);
    }

    public static CommandBlockReason createBlocked(String message) {
        return new CommandBlockReason(false, message);
    }




}