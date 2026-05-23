package com.litovskiy.util;

import java.util.ArrayList;
import java.util.List;

public record CommandResult(
    List<CommandMessage> messages
) {

    public static CommandResult empty() {
        return new CommandResult(List.of());
    }

    public static CommandResult single(String text) {
        return new CommandResult(List.of(CommandMessage.reply(text)));
    }

    public static CommandResult of(CommandMessage... messages) {
        return new CommandResult(List.of(messages));
    }

    public CommandResult add(CommandMessage message) {
        List<CommandMessage> result = new ArrayList<>(messages);
        result.add(message);
        return new CommandResult(result);
    }
}