package com.litovskiy.bot.tg;

import com.litovskiy.util.CommandMessage;
import com.litovskiy.util.MessageDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TgMessageSender {

    public void sendMessage(
        TelegramClient telegramClient,
        long chatId,
        Integer messageThreadId,
        Integer replyToMessageId,
        CommandMessage message
    ) {
        if (message == null || message.text() == null || message.text().isBlank()) {
            return;
        }

        Integer actualReplyToMessageId = message.delivery() == MessageDelivery.REPLY
            ? replyToMessageId
            : null;

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
            .chatId(chatId)
            .text(message.text());

        if (messageThreadId != null) {
            builder.messageThreadId(messageThreadId);
        }

        if (actualReplyToMessageId != null) {
            builder.replyToMessageId(actualReplyToMessageId);
        }

        if (message.html()) {
            builder.parseMode("HTML");
            builder.disableWebPagePreview(true);
        }
        try {
            Message sentMessage = telegramClient.execute(builder.build());

            if (message.deleteAfterSend() && sentMessage != null) {
                scheduleDelete(telegramClient, chatId, sentMessage.getMessageId());
            }
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram message. chatId={}, replyTo={}", chatId, actualReplyToMessageId, e);
        }
    }

    private void scheduleDelete(TelegramClient telegramClient, long chatId, Integer messageId) {
        if (messageId == null) {
            return;
        }

        CompletableFuture
            .delayedExecutor(30, TimeUnit.SECONDS)
            .execute(() -> deleteMessage(telegramClient, chatId, messageId));
    }

    private void deleteMessage(TelegramClient telegramClient, long chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();

            telegramClient.execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.debug("Failed to delete Telegram message. chatId={}, messageId={}", chatId, messageId, e);
        }
    }
}
