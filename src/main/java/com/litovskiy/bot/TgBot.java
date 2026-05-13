package com.litovskiy.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litovskiy.entity.Platform;
import com.litovskiy.service.AbilityService;
import com.litovskiy.service.ActivityService;
import com.litovskiy.service.AdminCommandService;
import com.litovskiy.service.AppServices;
import com.litovskiy.service.ConversationParticipantService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.GrowService;
import com.litovskiy.service.LeaderboardService;
import com.litovskiy.service.LinkService;
import com.litovskiy.service.PlayerAccountService;
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
    private final AbilityService abilityService;
    private final ConversationParticipantService conversationParticipantService;
    private final ConversationStyleService conversationStyleService;
    private final GrowService growService;
    private final LeaderboardService leaderboardService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;
    private final PlayerAccountService playerAccountService;

    private TelegramClient telegramClient;
    private Long botUserId;
    private String botUsername;

    public TgBot(AppServices appServices) {
        this.activityService = appServices.activityService();
        this.abilityService = appServices.abilityService();
        this.conversationParticipantService = appServices.conversationParticipantService();
        this.conversationStyleService = appServices.conversationStyleService();
        this.growService = appServices.dickService();
        this.leaderboardService = appServices.leaderboardService();
        this.linkService = appServices.linkService();
        this.adminCommandService = appServices.adminCommandService();
        this.playerAccountService = appServices.playerAccountService();
    }

    @SneakyThrows
    public static void start(AppServices appServices) {
        String token = BotConfig.telegramToken();
        if (token == null || token.isBlank()) {
            System.out.println("Telegram bot is not enabled because token is empty");
            return;
        }
        TgBot tgBot = new TgBot(appServices);
        TelegramBotsLongPollingApplication botsApplication;
        OkHttpClient okHttpClient = HttpClientFactory.create();

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
        System.out.println("Telegram bot started");
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
                formatTelegramDisplayName(update.getMessage().getFrom()),
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

        BotReply response = buildResponse(update, command, commandParts, chatId, profileId);
        if (response == null) {
            return;
        }

        sendMessage(chatId, messageThreadId, replyToMessageId, response);
    }

    private BotReply buildResponse(Update update, String command, String[] commandParts, long chatId, long profileId) {
        return switch (command) {
            case "/help" -> new BotReply(getHelp(), false);
            case "/grow" -> new BotReply(buildGrowResponse(chatId, profileId), false);
            case "/fuck" -> new BotReply(buildFuckResponse(update, chatId, profileId), false);
            case "/jackpot" -> new BotReply(abilityService.jackpot(Platform.TELEGRAM, profileId), false);
            case "/slow" -> new BotReply(buildSlowResponse(update, chatId, profileId), false);
            case "/turtle" -> new BotReply(abilityService.turtle(Platform.TELEGRAM, profileId), false);
            case "/pray" -> new BotReply(abilityService.pray(Platform.TELEGRAM, profileId), false);
            case "/transfer" -> new BotReply(buildTransferResponse(update, chatId, commandParts, profileId), false);
            case "/top" -> new BotReply(buildLeaderboardResponse(chatId, profileId), true);
            case "/link" -> new BotReply(buildLinkResponse(commandParts, profileId), false);
            case "/style" -> new BotReply(buildStyleResponse(commandParts, chatId, profileId), false);
            case "/admin" -> new BotReply(adminCommandService.handle(
                Platform.TELEGRAM,
                profileId,
                commandParts.length > 1 ? commandParts[1] : ""
            ), false);
            default -> null;
        };
    }

    private String getHelp() {
        return """
            /help - список всех команд
            /grow - вырастить показатель, раз в 8 часов
            /jackpot - увеличить шанс джекпота и неудачи за процент от своего размера
            /slow - замедлить рост врага бесплатно
            /turtle - увеличить себе рост бесплатно
            /pray - уменьшить себе шанс неудачи
            /transfer [число] - перевести часть роста другому игроку с комиссией (переводить можно только тем, у кого меньше)
            /top - увидеть топ игроков в своей беседе (в личных сообщениях - глобальный топ)
            /link - привязать свой тг/дс (рекомендуется использовать в лс с ботом)
            /style - изменить стиль роста (только в беседах)
            """;
    }

    private String buildGrowResponse(long chatId, long profileId) {
        Long scopeId = chatId < 0 ? chatId : null;
        return growService.grow(Platform.TELEGRAM, profileId, scopeId);
    }

    private String buildFuckResponse(Update update, long chatId, long profileId) {
        if (chatId >= 0) {
            return "Эта способность доступна только в группах.";
        }

        User target = extractReplyTarget(update);
        if (target == null) {
            return "Ответьте этой командой на сообщение цели.";
        }

        playerAccountService.updateTelegramProfile(target.getId(), formatTelegramDisplayName(target), target.getUserName());
        conversationParticipantService.registerParticipant(Platform.TELEGRAM, target.getId(), chatId);
        return abilityService.fuck(Platform.TELEGRAM, profileId, chatId, target.getId());
    }

    private String buildSlowResponse(Update update, long chatId, long profileId) {
        if (chatId >= 0) {
            return "Эта способность доступна только в группах.";
        }

        User target = extractReplyTarget(update);
        if (target == null) {
            return "Ответьте этой командой на сообщение цели.";
        }

        playerAccountService.updateTelegramProfile(target.getId(), formatTelegramDisplayName(target), target.getUserName());
        conversationParticipantService.registerParticipant(Platform.TELEGRAM, target.getId(), chatId);
        return abilityService.slow(Platform.TELEGRAM, profileId, chatId, target.getId());
    }

    private String buildTransferResponse(Update update, long chatId, String[] commandParts, long profileId) {
        if (chatId >= 0) {
            return "Эта способность доступна только в группах.";
        }

        User target = extractReplyTarget(update);
        if (target == null) {
            return "Ответьте этой командой на сообщение цели.";
        }

        if (commandParts.length < 2) {
            return "Нужно указать размер перевода";
        }

        playerAccountService.updateTelegramProfile(target.getId(), formatTelegramDisplayName(target), target.getUserName());
        conversationParticipantService.registerParticipant(Platform.TELEGRAM, target.getId(), chatId);
        return abilityService.transfer(Platform.TELEGRAM, profileId, chatId, target.getId(), commandParts[1]);
    }

    private String buildLeaderboardResponse(long chatId, long profileId) {
        Long scopeId = chatId < 0 ? chatId : null;
        return leaderboardService.buildLeaderboard(Platform.TELEGRAM, profileId, scopeId);
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

    private void sendMessage(long chatId, Integer messageThreadId, Integer replyToMessageId, BotReply reply) {
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
            .chatId(chatId)
            .text(reply.text());

        if (messageThreadId != null) {
            builder.messageThreadId(messageThreadId);
        }

        if (replyToMessageId != null) {
            builder.replyToMessageId(replyToMessageId);
        }

        if (reply.html()) {
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
                    formatTelegramDisplayName(member),
                    member.getUserName()
                );
                conversationParticipantService.registerParticipant(Platform.TELEGRAM, member.getId(), chatId);
            });
    }

    private User extractReplyTarget(Update update) {
        if (update.getMessage() == null || update.getMessage().getReplyToMessage() == null) {
            return null;
        }
        return update.getMessage().getReplyToMessage().getFrom();
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

    private String formatTelegramDisplayName(User user) {
        StringBuilder builder = new StringBuilder();
        if (!user.getFirstName().isBlank()) {
            builder.append(user.getFirstName().trim());
        }
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(user.getLastName().trim());
        }
        if (!builder.isEmpty()) {
            return builder.toString();
        }
        if (user.getUserName() != null && !user.getUserName().isBlank()) {
            return "@" + user.getUserName().trim();
        }
        return "Пользователь " + user.getId();
    }

    private record BotReply(String text, boolean html) {
    }
}
