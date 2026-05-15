package com.litovskiy.bot.tg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litovskiy.entity.Platform;
import com.litovskiy.service.ConversationParticipantService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.activity.ActivityService;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TgBot implements LongPollingSingleThreadUpdateConsumer {

    private final TgResponseBuilder tgResponseBuilder;
    private final ActivityService activityService;
    private final ConversationStyleService conversationStyleService;
    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantService conversationParticipantService;

    private final OkHttpClient okHttpClient;
    private TelegramClient telegramClient;

    @Value("${telegram.token}")
    private String token;

    private Long botUserId;
    private String botUsername;

    @SneakyThrows
    public void start() {
        if (token == null || token.isBlank()) {
            log.warn("Discord bot is not enabled because token is empty");
            return;
        }
        telegramClient = new OkHttpTelegramClient(okHttpClient, token);
        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> okHttpClient);

        User me = telegramClient.execute(new GetMe());
        botUserId = me.getId();
        botUsername = me.getUserName();

        botsApplication.registerBot(token, this);
        log.info("Telegram bot started");
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        if (update.getMessage().getNewChatMembers() != null && !update.getMessage().getNewChatMembers().isEmpty()) {
            handleBotAdded(update);
        }

        if (update.getMessage().getFrom() != null) {
            playerAccountService.updateTelegramProfile(
                update.getMessage().getFrom().getId(),
                StringUtil.formatTelegramDisplayName(update.getMessage().getFrom()),
                update.getMessage().getFrom().getUserName()
            );
            if (update.getMessage().getChatId() < 0) {
                conversationParticipantService.registerParticipant(
                    Platform.TELEGRAM,
                    update.getMessage().getFrom().getId(),
                    update.getMessage().getChatId()
                );
            }
        }

        if (!update.getMessage().hasText()) {
            return;
        }

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        Integer messageThreadId = update.getMessage().getMessageThreadId();
        Integer replyToMessageId = update.getMessage().getMessageId();
        long profileId = update.getMessage().getFrom().getId();
        if (chatId < 0 && !text.startsWith("/")) {
            activityService.recordMessage(Platform.TELEGRAM, profileId, chatId);
        }

        String[] commandParts = text.split("\\s+", 2);
        String command = normalizeCommand(commandParts[0]);
        if (command == null) {
            return;
        }

        BotReply response = tgResponseBuilder.buildResponse(update, command, commandParts, chatId, profileId);
        if (response == null) {
            return;
        }

        sendMessage(chatId, messageThreadId, replyToMessageId, response);
    }

    private void sendMessage(long chatId, Integer messageThreadId, Integer replyToMessageId, BotReply reply) {
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
            .chatId(chatId)
            .text(reply.getText());

        if (messageThreadId != null) {
            builder.messageThreadId(messageThreadId);
        }

        if (replyToMessageId != null) {
            builder.replyToMessageId(replyToMessageId);
        }

        if (reply.isHtml()) {
            builder.parseMode("HTML");
            builder.disableWebPagePreview(true);
        }

        try {
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleBotAdded(Update update) {
        if (botUserId == null || update.getMessage().getFrom() == null) {
            return;
        }

        boolean botWasAdded = update.getMessage().getNewChatMembers().stream()
            .map(User::getId)
            .anyMatch(id -> id.equals(botUserId));

        if (botWasAdded) {
            conversationStyleService.registerTelegramManager(
                update.getMessage().getChatId(),
                update.getMessage().getFrom().getId()
            );
        }

        long chatId = update.getMessage().getChatId();
        update.getMessage().getNewChatMembers().stream()
            .filter(member -> !member.getIsBot())
            .forEach(member -> {
                playerAccountService.updateTelegramProfile(
                    member.getId(),
                    StringUtil.formatTelegramDisplayName(member),
                    member.getUserName()
                );
                conversationParticipantService.registerParticipant(Platform.TELEGRAM, member.getId(), chatId);
            });
    }

    private String normalizeCommand(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return null;
        }

        int mentionSeparator = rawCommand.indexOf('@');
        if (mentionSeparator < 0) {
            return rawCommand;
        }

        String mentionedBot = rawCommand.substring(mentionSeparator + 1);
        if (botUsername != null && botUsername.equalsIgnoreCase(mentionedBot)) {
            return rawCommand.substring(0, mentionSeparator);
        }

        return null;
    }
}
