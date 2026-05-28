package com.litovskiy.bot;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Data
@AllArgsConstructor
public class CommandMessage {

    private String text;
    private MessageDelivery delivery;
    private Boolean html;
    private InlineKeyboardMarkup keyboard;

    public static CommandMessage reply(String text) {
        return new CommandMessage(text, MessageDelivery.REPLY, false, null);
    }

    public static CommandMessage reply(String text, boolean html) {
        return new CommandMessage(text, MessageDelivery.REPLY, html, null);
    }

    public static CommandMessage broadcast(String text) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, false, null);
    }

    public static CommandMessage broadcast(String text, InlineKeyboardMarkup keyboard) {
        return new CommandMessage(text, MessageDelivery.BROADCAST, false, keyboard);
    }

}
