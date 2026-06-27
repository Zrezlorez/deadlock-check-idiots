package com.litovskiy.bot;

import com.litovskiy.entity.TelegramCallbackRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommandMessage {

    private String text;
    private MessageDelivery delivery;
    private Boolean html;
    private KeyboardSpec keyboard;
    private TelegramCallbackRequest request;

    public static CommandMessage reply(String text) {
        return new CommandMessage(text, MessageDelivery.REPLY, false, null, null);
    }

    public static CommandMessage reply(String text, boolean html) {
        return new CommandMessage(text, MessageDelivery.REPLY, html, null, null);
    }

    public static CommandMessage broadcast(String text) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, false, null, null);
    }

    public static CommandMessage broadcast(String text, KeyboardSpec keyboard) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, true, keyboard, null);
    }

    public static CommandMessage broadcast(String text, KeyboardSpec keyboard, TelegramCallbackRequest request) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, true, keyboard, request);
    }

}
