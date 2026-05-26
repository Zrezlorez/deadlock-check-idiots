package com.litovskiy.util;

public record CommandMessage(
    String text,
    MessageDelivery delivery,
    boolean html,
    boolean deleteAfterSend
) {

    public static CommandMessage reply(String text) {
        return new CommandMessage(text, MessageDelivery.REPLY, false, false);
    }

    public static CommandMessage reply(String text, boolean html) {
        return new CommandMessage(text, MessageDelivery.REPLY, html, false);
    }

    public static CommandMessage broadcast(String text) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, false, false);
    }

    public static CommandMessage temporary(String text) {
        return new CommandMessage(text, MessageDelivery.REPLY, false,true);
    }
}
