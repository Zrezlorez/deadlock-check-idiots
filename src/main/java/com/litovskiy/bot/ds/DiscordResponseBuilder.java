package com.litovskiy.bot.ds;

import com.litovskiy.entity.Platform;
import com.litovskiy.service.AbilityService;
import com.litovskiy.service.AdminCommandService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.GrowService;
import com.litovskiy.service.LeaderboardService;
import com.litovskiy.service.LinkService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscordResponseBuilder {


    private final AbilityService abilityService;
    private final ConversationStyleService conversationStyleService;
    private final GrowService growService;
    private final LeaderboardService leaderboardService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;

    public String buildResponse(SlashCommandInteractionEvent event) {
        return switch (event.getName()) {
            case "grow" -> buildGrowResponse(event);
            case "fuck" -> buildFuckResponse(event);
            case "jackpot" -> buildJackpotAbilityResponse(event);
            case "turtle" -> buildTurtleResponse(event);
            case "pray" -> buildPrayResponse(event);
            case "transfer" -> buildTransferResponse(event);
            case "slow" -> buildSlowResponse(event);
            case "top" -> buildLeaderboardResponse(event);
            case "link" -> buildLinkResponse(event);
            case "style" -> buildStyleResponse(event);
            case "admin" -> adminCommandService.handle(
                Platform.DISCORD,
                event.getUser().getIdLong(),
                event.getOption("command", "", OptionMapping::getAsString)
            );
            default -> null;
        };
    }

    private String buildGrowResponse(SlashCommandInteractionEvent event) {
        Long scopeId = event.isFromGuild() ? event.getGuild().getIdLong() : null;
        return growService.grow(Platform.DISCORD, event.getUser().getIdLong(), scopeId);
    }

    private String buildFuckResponse(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            return "Эта способность доступна только на сервере.";
        }

        User target = event.getOption("user", null, OptionMapping::getAsUser);
        if (target == null) {
            return "Нужно указать цель.";
        }

        return abilityService.fuck(
            Platform.DISCORD,
            event.getUser().getIdLong(),
            event.getGuild().getIdLong(),
            target.getIdLong()
        );
    }

    private String buildSlowResponse(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            return "Эта способность доступна только на сервере.";
        }

        User target = event.getOption("user", null, OptionMapping::getAsUser);
        if (target == null) {
            return "Нужно указать цель.";
        }

        return abilityService.slow(
            Platform.DISCORD,
            event.getUser().getIdLong(),
            event.getGuild().getIdLong(),
            target.getIdLong()
        );
    }

    private String buildTurtleResponse(SlashCommandInteractionEvent event) {
        return abilityService.turtle(Platform.DISCORD, event.getUser().getIdLong());
    }

    private String buildPrayResponse(SlashCommandInteractionEvent event) {
        return abilityService.pray(Platform.DISCORD, event.getUser().getIdLong());
    }

    private String buildJackpotAbilityResponse(SlashCommandInteractionEvent event) {
        return abilityService.jackpot(Platform.DISCORD, event.getUser().getIdLong());
    }

    private String buildTransferResponse(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            return "Эта способность доступна только на сервере.";
        }

        User target = event.getOption("user", null, OptionMapping::getAsUser);
        if (target == null) {
            return "Нужно указать цель.";
        }

        String value = event.getOption("value", null, OptionMapping::getAsString);
        if (value == null) {
            return "Нужно указать размер перевода";
        }

        return abilityService.transfer(
            Platform.DISCORD,
            event.getUser().getIdLong(),
            event.getGuild().getIdLong(),
            target.getIdLong(),
            value
        );
    }

    private String buildLeaderboardResponse(SlashCommandInteractionEvent event) {
        Long scopeId = event.isFromGuild() ? event.getGuild().getIdLong() : null;
        return leaderboardService.buildLeaderboard(Platform.DISCORD, event.getUser().getIdLong(), scopeId);
    }

    private String buildLinkResponse(SlashCommandInteractionEvent event) {
        String code = event.getOption("code", null, OptionMapping::getAsString);
        return code == null || code.isBlank()
            ? linkService.createCode(Platform.DISCORD, event.getUser().getIdLong())
            : linkService.linkProfile(Platform.DISCORD, event.getUser().getIdLong(), code);
    }

    private String buildStyleResponse(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            return "Стиль можно менять только на сервере.";
        }

        String styleName = event.getOption("name", null, OptionMapping::getAsString);
        long scopeId = event.getGuild().getIdLong();
        if (styleName == null || styleName.isBlank()) {
            return conversationStyleService.describeCurrentStyle(Platform.DISCORD, scopeId);
        }

        if (event.getMember() == null || !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            return "Менять стиль сервера могут только участники с правом Manage Server.";
        }

        return conversationStyleService.updateDiscordStyle(scopeId, styleName);
    }
}
