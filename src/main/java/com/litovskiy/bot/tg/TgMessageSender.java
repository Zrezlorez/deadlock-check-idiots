package com.litovskiy.bot.tg;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.MessageDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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
        if (message == null || message.getText() == null || message.getText().isBlank()) {
            return;
        }

        Integer actualReplyToMessageId = message.getDelivery() == MessageDelivery.REPLY
            ? replyToMessageId
            : null;

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
            .chatId(chatId)
            .text(message.getText());

        if (messageThreadId != null) {
            builder.messageThreadId(messageThreadId);
        }

        if (actualReplyToMessageId != null) {
            builder.replyToMessageId(actualReplyToMessageId);
        }

        if (message.getHtml()) {
            builder.parseMode("HTML");
            builder.disableWebPagePreview(true);
        }

        if (message.getKeyboard() != null) {
            builder.replyMarkup(message.getKeyboard());
        }

        try {
            telegramClient.execute(builder.build());

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
