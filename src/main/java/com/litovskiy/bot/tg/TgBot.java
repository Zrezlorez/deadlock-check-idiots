package com.litovskiy.bot.tg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litovskiy.entity.Platform;
import com.litovskiy.service.ConversationParticipantService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.children.TelegramCallbackService;
import com.litovskiy.service.children.ChildrenService;
import com.litovskiy.service.activity.ActivityService;
import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.bot.MessageDelivery;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TgBot implements LongPollingSingleThreadUpdateConsumer {
    private final TgResponseBuilder tgResponseBuilder;
    private final ActivityService activityService;
    private final ChildrenService childrenService;
    private final ConversationStyleService conversationStyleService;
    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantService conversationParticipantService;
    private final TelegramCallbackService callbackService;
    private final TgMessageSender sender;
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
        if (update.hasMessage()) {
            consumeMessage(update);
            return;
        }
        if (update.hasCallbackQuery()) {
            consumeCallback(update);
        }

    }

    @Scheduled(cron = "0 * * * * *", zone = "Europe/Moscow")
    public void sendDailyChildrenCareMessages() {
        if (telegramClient == null) {
            return;
        }

        ChildrenService.DailyCareDispatch dispatch = childrenService.prepareDailyCareDispatch();

        for (ChildrenService.CareMessageEdit edit : dispatch.edits()) {
            sender.editMessage(telegramClient, edit.scopeId(), edit.messageId(), edit.text(), null);
        }

        for (ChildrenService.DailyCareMessage message : dispatch.messages()) {
            Message sentMessage = sender.sendMessage(
                telegramClient,
                message.scopeId(),
                null,
                null,
                CommandMessage.broadcast(message.text(), message.keyboard())
            );

            if (sentMessage != null) {
                childrenService.setCareMessageId(message.careId(), sentMessage.getMessageId());
            }
        }
    }

    private void consumeCallback(Update update) {
        CallbackQuery callback = update.getCallbackQuery();
        String callData = callback.getData();
        String callId = callback.getId();
        long messageId = callback.getMessage().getMessageId();
        long chatId = callback.getMessage().getChatId();
        Long userId = callback.getFrom().getId();

        var result = callbackService.handleCallback(chatId, messageId, userId, callData);

        sender.answerCallback(telegramClient, callId, result.answerText());
        if (result.editText() != null) {
            sender.editMessage(
                telegramClient,
                chatId,
                messageId,
                result.editText(),
                result.editKeyboard()
            );
        }

    }

    private void consumeMessage(Update update) {
        Message message = update.getMessage();

        if (hasNewChatMembers(message)) {
            handleBotAdded(message);
        }

        User senderUser = message.getFrom();
        if (senderUser == null) {
            return;
        }

        updateTelegramProfile(senderUser);
        registerGroupParticipantIfNeeded(senderUser, message);

        if (!message.hasText()) {
            return;
        }

        String text = message.getText().trim();
        long chatId = message.getChatId();
        long profileId = message.getFrom().getId();


        if (chatId < 0 && !text.startsWith("/")) {
            activityService.recordMessage(Platform.TELEGRAM, profileId, chatId);
            return;
        }

        String[] commandParts = text.split("\\s+", 2);
        String command = normalizeCommand(commandParts[0]);
        if (command == null) {
            return;
        }

        CommandResult response = tgResponseBuilder.buildResponse(
            update,
            command,
            commandParts,
            chatId,
            profileId
        );

        sendResponseMessages(response, chatId, message);
    }

    private void sendResponseMessages(CommandResult response, long chatId, Message sourceMessage) {
        if (response == null || response.messages().isEmpty()) {
            return;
        }

        Integer messageThreadId = sourceMessage.getMessageThreadId();
        Integer sourceMessageId = sourceMessage.getMessageId();

        for (CommandMessage message : response.messages()) {
            Integer replyToMessageId = message.getDelivery() == MessageDelivery.REPLY
                ? sourceMessageId
                : null;

            Message newMessage = sender.sendMessage(
                telegramClient,
                chatId,
                messageThreadId,
                replyToMessageId,
                message
            );

            if (message.getRequest() != null && newMessage != null) {
                callbackService.setMessageCallback(message.getRequest(), newMessage.getMessageId());
            }
        }
    }

    private void handleBotAdded(Message message) {
        if (botUserId == null || message.getFrom() == null) {
            return;
        }

        boolean botWasAdded = message.getNewChatMembers().stream()
            .map(User::getId)
            .anyMatch(id -> id.equals(botUserId));

        if (botWasAdded) {
            conversationStyleService.registerTelegramManager(
                message.getChatId(),
                message.getFrom().getId()
            );
        }

        long chatId = message.getChatId();
        message.getNewChatMembers().stream()
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

    private void updateTelegramProfile(User user) {
        playerAccountService.updateTelegramProfile(
            user.getId(),
            StringUtil.formatTelegramDisplayName(user),
            user.getUserName()
        );
    }

    private void registerGroupParticipantIfNeeded(User user, Message message) {
        long chatId = message.getChatId();

        if (chatId >= 0) {
            return;
        }

        conversationParticipantService.registerParticipant(
            Platform.TELEGRAM,
            user.getId(),
            chatId
        );
    }

    private boolean hasNewChatMembers(Message message) {
        return message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty();
    }
}
