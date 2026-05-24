package com.litovskiy.util;

public record CommandMessage(
    String text,
    MessageDelivery delivery,
    boolean deleteAfterSend
) {

    public static CommandMessage reply(String text) {
        return new CommandMessage(text, MessageDelivery.REPLY, false);
    }

    public static CommandMessage broadcast(String text) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, false);
    }

    public static CommandMessage temporary(String text) {
        return new CommandMessage(text, MessageDelivery.REPLY, true);
    }
}
