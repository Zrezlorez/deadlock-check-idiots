package com.litovskiy.bot.tg;

import com.litovskiy.entity.Platform;
import com.litovskiy.service.AbilityService;
import com.litovskiy.service.AdminCommandService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.GrowService;
import com.litovskiy.service.LeaderboardService;
import com.litovskiy.service.LinkService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.util.CommandMessage;
import com.litovskiy.util.CommandResult;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgResponseBuilder {

    private final AbilityService abilityService;
    private final ConversationStyleService conversationStyleService;
    private final GrowService growService;
    private final LeaderboardService leaderboardService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;
    private final PlayerAccountService playerAccountService;

    public CommandResult buildResponse(Update update, String command, String[] commandParts, long chatId, long profileId) {
        return switch (command) {
            case "/help", "/start" ->
                CommandResult.single(getHelp());
            case "/grow" -> {
                Boolean isScheduledMessage = update.getMessage().getIsFromOffline();
                yield buildGrowResponse(chatId, profileId, isScheduledMessage);
            }
            case "/fuck" ->
                buildFuckResponse(update, chatId, profileId);
            case "/jackpot" ->
                abilityService.jackpot(Platform.TELEGRAM, profileId);
            case "/slow" ->
                buildSlowResponse(update, chatId, profileId);
            case "/turtle" ->
                abilityService.turtle(Platform.TELEGRAM, profileId);
            case "/pray" ->
                abilityService.pray(Platform.TELEGRAM, profileId);
            case "/transfer" ->
                buildTransferResponse(update, chatId, commandParts, profileId);
            case "/top" ->
                buildLeaderboardResponse(chatId, profileId);
            case "/link" ->
                buildLinkResponse(commandParts, profileId);
            case "/style" ->
                buildStyleResponse(commandParts, chatId, profileId);
            case "/profile" ->
                buildProfileResponse(update, chatId, profileId);
            case "/admin" ->
                adminCommandService.handle(
                Platform.TELEGRAM,
                profileId,
                commandParts.length > 1 ? commandParts[1] : ""
            );
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
            /profile - посмотреть профиль (свой или другого игрока)
            """;
    }

    private CommandResult buildGrowResponse(long chatId, long profileId, Boolean isScheduledMessage) {
        Long scopeId = chatId < 0 ? chatId : null;
        boolean isScheduled = isScheduledMessage != null && isScheduledMessage;

        return growService.grow(Platform.TELEGRAM, profileId, scopeId, isScheduled);
    }

    private CommandResult buildFuckResponse(Update update, long chatId, long profileId) {
        if (chatId >= 0) {
            return CommandResult.single("Эта способность доступна только в группах.");
        }

        User target = extractReplyTarget(update.getMessage());
        if (target == null) {
            return CommandResult.single("Ответьте этой командой на сообщение цели.");
        }

        playerAccountService.updateTelegramProfile(target.getId(), StringUtil.formatTelegramDisplayName(target), target.getUserName());
        return abilityService.fuck(Platform.TELEGRAM, profileId, chatId, target.getId());
    }

    private CommandResult buildSlowResponse(Update update, long chatId, long profileId) {
        if (chatId >= 0) {
            return CommandResult.single("Эта способность доступна только в группах.");
        }

        User target = extractReplyTarget(update.getMessage());
        if (target == null) {
            return CommandResult.single("Ответьте этой командой на сообщение цели.");
        }

        playerAccountService.updateTelegramProfile(target.getId(), StringUtil.formatTelegramDisplayName(target), target.getUserName());
        return abilityService.slow(Platform.TELEGRAM, profileId, chatId, target.getId());
    }

    private CommandResult buildTransferResponse(Update update, long chatId, String[] commandParts, long profileId) {
        if (chatId >= 0) {
            return CommandResult.single("Эта способность доступна только в группах.");
        }

        User target = extractReplyTarget(update.getMessage());
        if (target == null) {
            return CommandResult.single("Ответьте этой командой на сообщение цели.");
        }

        if (commandParts.length < 2) {
            return CommandResult.single("Нужно указать размер перевода");
        }

        playerAccountService.updateTelegramProfile(target.getId(), StringUtil.formatTelegramDisplayName(target), target.getUserName());
        return abilityService.transfer(Platform.TELEGRAM, profileId, chatId, target.getId(), commandParts[1]);
    }

    private CommandResult buildLeaderboardResponse(long chatId, long profileId) {
        Long scopeId = chatId < 0 ? chatId : null;
        return CommandResult.of(
            CommandMessage.reply(leaderboardService.buildLeaderboard(Platform.TELEGRAM, profileId, scopeId), true)
        );
    }

    private CommandResult buildLinkResponse(String[] commandParts, long profileId) {
        return CommandResult.single(commandParts.length > 1
            ? linkService.linkProfile(Platform.TELEGRAM, profileId, commandParts[1])
            : linkService.createCode(Platform.TELEGRAM, profileId));
    }

    private CommandResult buildStyleResponse(String[] commandParts, long chatId, long profileId) {
        if (chatId >= 0) {
            return CommandResult.single("Стиль настраивается только в группах.");
        }

        if (commandParts.length == 1 || commandParts[1].isBlank()) {
            return CommandResult.single(conversationStyleService.describeCurrentStyle(Platform.TELEGRAM, chatId));
        }

        return CommandResult.single(conversationStyleService.updateTelegramStyle(chatId, profileId, commandParts[1]));
    }

    private CommandResult buildProfileResponse(Update update, long chatId, long profileId) {
        User target = extractReplyTarget(update.getMessage());
        if (target != null && target.getIsBot()) {
            return CommandResult.single("Бот нищий, у него нет профиля");
        }

        long targetProfileId = target == null
                ? profileId
                : target.getId();

        return growService.buildProfileResponse(Platform.TELEGRAM, targetProfileId, chatId);
    }

    private User extractReplyTarget(Message message) {
        Message replyMessage = message.getReplyToMessage();
        if (replyMessage == null
            || replyMessage.getMessageId().equals(replyMessage.getMessageThreadId())) {
            return null;
        }
        return replyMessage.getFrom();
    }
}
