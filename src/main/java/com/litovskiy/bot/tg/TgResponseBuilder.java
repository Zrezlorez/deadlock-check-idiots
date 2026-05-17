package com.litovskiy.bot.tg;

import com.litovskiy.entity.Platform;
import com.litovskiy.service.AbilityService;
import com.litovskiy.service.AdminCommandService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.GrowService;
import com.litovskiy.service.LeaderboardService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.LinkService;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

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

    public BotReply buildResponse(Update update, String command, String[] commandParts, long chatId, long profileId) {
        return switch (command) {
            case "/help", "/start" ->
                new BotReply(getHelp(), false);
            case "/grow" ->
                new BotReply(buildGrowResponse(chatId, profileId), false);
            case "/fuck" ->
                new BotReply(buildFuckResponse(update, chatId, profileId), false);
            case "/jackpot" ->
                new BotReply(abilityService.jackpot(Platform.TELEGRAM, profileId), false);
            case "/slow" ->
                new BotReply(buildSlowResponse(update, chatId, profileId), false);
            case "/turtle" ->
                new BotReply(abilityService.turtle(Platform.TELEGRAM, profileId), false);
            case "/pray" ->
                new BotReply(abilityService.pray(Platform.TELEGRAM, profileId), false);
            case "/transfer" ->
                new BotReply(buildTransferResponse(update, chatId, commandParts, profileId), false);
            case "/top" ->
                new BotReply(buildLeaderboardResponse(chatId, profileId), true);
            case "/link" ->
                new BotReply(buildLinkResponse(commandParts, profileId), false);
            case "/style" ->
                new BotReply(buildStyleResponse(commandParts, chatId, profileId), false);
            case "/admin" ->
                new BotReply(adminCommandService.handle(
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

        playerAccountService.updateTelegramProfile(target.getId(), StringUtil.formatTelegramDisplayName(target), target.getUserName());
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

        playerAccountService.updateTelegramProfile(target.getId(), StringUtil.formatTelegramDisplayName(target), target.getUserName());
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

        playerAccountService.updateTelegramProfile(target.getId(), StringUtil.formatTelegramDisplayName(target), target.getUserName());
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

    private User extractReplyTarget(Update update) {
        if (update.getMessage() == null || update.getMessage().getReplyToMessage() == null) {
            return null;
        }
        return update.getMessage().getReplyToMessage().getFrom();
    }

}
