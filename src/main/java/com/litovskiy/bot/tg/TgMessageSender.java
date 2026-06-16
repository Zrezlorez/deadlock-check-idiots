package com.litovskiy.bot.tg;

import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.KeyboardSpec;
import com.litovskiy.bot.MessageDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class TgMessageSender {
    public Message sendMessage(
        TelegramClient telegramClient,
        long chatId,
        Integer messageThreadId,
        Integer replyToMessageId,
        CommandMessage message
    ) {
        if (message == null || message.getText() == null || message.getText().isBlank()) {
            return null;
        }

        Integer actualReplyToMessageId = message.getDelivery() == MessageDelivery.REPLY
            ? replyToMessageId
            : null;

        var builder = SendMessage.builder()
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

        InlineKeyboardMarkup keyboard = buildKeyboard(message.getKeyboard());
        if (keyboard != null) {
            builder.replyMarkup(keyboard);
        }

        try {
            return telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram message. chatId={}, replyTo={}", chatId, actualReplyToMessageId, e);
            return null;
        }
    }

    public void answerCallback(TelegramClient telegramClient, String callbackId, String text) {
        try {
            var builder = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .showAlert(false);

            if (text != null && !text.isBlank()) {
                builder.text(text);
            }

            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback. callbackId={}", callbackId, e);
        }
    }

    public void editMessage(
        TelegramClient telegramClient,
        long chatId,
        long messageId,
        String text,
        KeyboardSpec keyboard
    ) {
        try {
            telegramClient.execute(EditMessageText.builder()
                .chatId(chatId)
                .messageId(Math.toIntExact(messageId))
                .text(text)
                .replyMarkup(buildKeyboard(keyboard))
                .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to edit Telegram message. chatId={}, messageId={}", chatId, messageId, e);
        }
    }

    private InlineKeyboardMarkup buildKeyboard(KeyboardSpec keyboard) {
        if (keyboard == null || keyboard.rows() == null || keyboard.rows().isEmpty()) {
            return null;
        }

        return InlineKeyboardMarkup.builder()
            .keyboard(keyboard.rows().stream()
                .map(row -> new InlineKeyboardRow(row.stream()
                    .map(button -> InlineKeyboardButton.builder()
                        .text(button.text())
                        .callbackData(button.callbackData())
                        .build())
                    .toList()))
                .toList())
            .build();
    }
}
