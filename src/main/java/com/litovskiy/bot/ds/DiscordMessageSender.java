package com.litovskiy.bot.ds;

import com.litovskiy.util.CommandMessage;
import com.litovskiy.util.CommandResult;
import com.litovskiy.util.MessageDelivery;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DiscordMessageSender {

    public void sendMessages(SlashCommandInteractionEvent event, CommandResult result) {
        if (result == null || result.messages() == null || result.messages().isEmpty()) {
            return;
        }

        List<CommandMessage> messages = result.messages();
        CommandMessage firstMessage = messages.get(0);

        event.reply(firstMessage.text()).queue(
            hook -> {
                if (firstMessage.deleteAfterSend()) {
                    hook.deleteOriginal().queueAfter(30, TimeUnit.SECONDS);
                }

                for (int i = 1; i < messages.size(); i++) {
                    sendAdditionalMessage(event, hook, messages.get(i));
                }
            },
            error -> log.warn("Failed to reply to Discord slash command. command={}", event.getName(), error)
        );
    }

    private void sendAdditionalMessage(
        SlashCommandInteractionEvent event,
        InteractionHook hook,
        CommandMessage message
    ) {
        if (message == null || message.text() == null || message.text().isBlank()) {
            return;
        }

        if (message.delivery() == MessageDelivery.BROADCAST) {
            event.getChannel()
                .sendMessage(message.text())
                .queue(
                    sentMessage -> {
                        if (message.deleteAfterSend()) {
                            sentMessage.delete().queueAfter(30, TimeUnit.SECONDS);
                        }
                    },
                    error -> log.warn("Failed to send Discord broadcast message. command={}", event.getName(), error)
                );
            return;
        }

        hook.sendMessage(message.text())
            .queue(
                sentMessage -> {
                    if (message.deleteAfterSend()) {
                        sentMessage.delete().queueAfter(30, TimeUnit.SECONDS);
                    }
                },
                error -> log.warn("Failed to send Discord follow-up message. command={}", event.getName(), error)
            );
    }
}
