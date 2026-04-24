package com.litovskiy.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litovskiy.entity.Platform;
import com.litovskiy.service.ActivityService;
import com.litovskiy.service.AdminCommandService;
import com.litovskiy.service.AppServices;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.DickService;
import com.litovskiy.service.LinkService;
import com.litovskiy.util.BotConfig;
import com.litovskiy.util.HttpClientFactory;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class TgBot implements LongPollingSingleThreadUpdateConsumer {

    private final ActivityService activityService;
    private final ConversationStyleService conversationStyleService;
    private final DickService dickService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;

    private TelegramClient telegramClient;
    private Long botUserId;
    private String botUsername;

    public TgBot(AppServices appServices) {
        this.activityService = appServices.getActivityService();
        this.conversationStyleService = appServices.getConversationStyleService();
        this.dickService = appServices.getDickService();
        this.linkService = appServices.getLinkService();
        this.adminCommandService = appServices.getAdminCommandService();
    }

    @SneakyThrows
    public static void start(AppServices appServices) {
        TgBot tgBot = new TgBot(appServices);
        TelegramBotsLongPollingApplication botsApplication;
        OkHttpClient okHttpClient = HttpClientFactory.create();
        String token = BotConfig.telegramToken();

        tgBot.telegramClient = new OkHttpTelegramClient(okHttpClient, token);
        if (BotConfig.isProxyEnabled()) {
            botsApplication = new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> okHttpClient);
        } else {
            botsApplication = new TelegramBotsLongPollingApplication();
        }

        User me = tgBot.telegramClient.execute(new GetMe());
        tgBot.botUserId = me.getId();
        tgBot.botUsername = me.getUserName();

        botsApplication.registerBot(token, tgBot);
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        if (update.getMessage().getNewChatMembers() != null && !update.getMessage().getNewChatMembers().isEmpty()) {
            handleBotAdded(update);
        }

        if (!update.getMessage().hasText()) {
            return;
        }

        String text = update.getMessage().getText().trim();
        long chatId = update.getMessage().getChatId();
        long profileId = update.getMessage().getFrom().getId();
        if (chatId < 0 && !text.startsWith("/")) {
            activityService.recordMessage(Platform.TELEGRAM, profileId, chatId);
        }

        String[] commandParts = text.split("\\s+", 2);
        String command = normalizeCommand(commandParts[0]);
        if (command == null) {
            return;
        }

        String response = buildResponse(command, commandParts, chatId, profileId);
        if (response == null) {
            return;
        }

        sendMessage(chatId, response);
    }

    private String buildResponse(String command, String[] commandParts, long chatId, long profileId) {
        return switch (command) {
            case "/grow" -> buildGrowResponse(chatId, profileId);
            case "/link" -> buildLinkResponse(commandParts, profileId);
            case "/style" -> buildStyleResponse(commandParts, chatId, profileId);
            case "/admin" -> adminCommandService.handle(
                Platform.TELEGRAM,
                profileId,
                commandParts.length > 1 ? commandParts[1] : ""
            );
            default -> null;
        };
    }

    private String buildGrowResponse(long chatId, long profileId) {
        Long scopeId = chatId < 0 ? chatId : null;
        return dickService.grow(Platform.TELEGRAM, profileId, scopeId);
    }

    private String buildLinkResponse(String[] commandParts, long profileId) {
        return commandParts.length > 1
            ? linkService.linkProfile(Platform.TELEGRAM, profileId, commandParts[1])
            : linkService.createCode(Platform.TELEGRAM, profileId);
    }

    private String buildStyleResponse(String[] commandParts, long chatId, long profileId) {
        if (chatId >= 0) {
            return "Стиль настраивается только в группах.";
        }

        if (commandParts.length == 1 || commandParts[1].isBlank()) {
            return conversationStyleService.describeCurrentStyle(Platform.TELEGRAM, chatId);
        }

        return conversationStyleService.updateTelegramStyle(chatId, profileId, commandParts[1]);
    }

    private void sendMessage(long chatId, String response) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text(response)
            .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
