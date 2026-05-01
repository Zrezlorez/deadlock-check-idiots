package com.litovskiy.bot;

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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;

public class DiscordBot extends ListenerAdapter {

    private final ActivityService activityService;
    private final AbilityService abilityService;
    private final ConversationParticipantService conversationParticipantService;
    private final ConversationStyleService conversationStyleService;
    private final GrowService growService;
    private final LeaderboardService leaderboardService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;
    private final PlayerAccountService playerAccountService;

    public DiscordBot(AppServices appServices) {
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
        OkHttpClient okHttpClient = HttpClientFactory.create();
        JDA jda = JDABuilder.createDefault(BotConfig.discordToken())
            .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
            .setHttpClient(okHttpClient)
            .addEventListeners(new DiscordBot(appServices))
            .build();

        jda.awaitReady();
        registerCommands(jda);
    }

    private static void registerCommands(JDA jda) {
        jda.updateCommands()
            .addCommands(
                Commands.slash("grow", "Вырастить показатель"),
                Commands.slash("fuck", "Повысить шанс неудачи у цели")
                    .addOption(OptionType.USER, "user", "Цель", true),
                Commands.slash("casino", "Повысить шанс джекпота для себя"),
                Commands.slash("slow", "Урезать следующий рост цели")
                    .addOption(OptionType.USER, "user", "Цель", true),
                Commands.slash("top", "Показать лидерборд"),
                Commands.slash("link", "Сгенерировать код привязки или привязать профиль")
                    .addOption(OptionType.STRING, "code", "Код из другого бота", false),
                Commands.slash("style", "Посмотреть или изменить стиль роста на сервере")
                    .addOption(OptionType.STRING, "name", "Название стиля", false),
                Commands.slash("admin", "Админ-команды")
                    .addOption(OptionType.STRING, "command", "Например: config, player show telegram @name, player show discord user#1234", false)
            )
            .queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        playerAccountService.updateDiscordTag(event.getUser().getIdLong(), formatDiscordTag(event.getUser()));
        if (event.isFromGuild()) {
            conversationParticipantService.registerParticipant(
                Platform.DISCORD,
                event.getUser().getIdLong(),
                event.getGuild().getIdLong()
            );
        }
        String response = buildSlashResponse(event);
        if (response != null) {
            event.reply(response).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        playerAccountService.updateDiscordTag(event.getAuthor().getIdLong(), formatDiscordTag(event.getAuthor()));
        conversationParticipantService.registerParticipant(
            Platform.DISCORD,
            event.getAuthor().getIdLong(),
            event.getGuild().getIdLong()
        );
        activityService.recordMessage(Platform.DISCORD, event.getAuthor().getIdLong(), event.getGuild().getIdLong());
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) {
            return;
        }

        conversationParticipantService.registerParticipant(
            Platform.DISCORD,
            event.getMember().getIdLong(),
            event.getGuild().getIdLong()
        );

        if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            activityService.endVoiceSession(Platform.DISCORD, event.getMember().getIdLong());
            return;
        }

        if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
            activityService.startVoiceSession(Platform.DISCORD, event.getMember().getIdLong(), event.getGuild().getIdLong());
        }
    }

    private String buildSlashResponse(SlashCommandInteractionEvent event) {
        return switch (event.getName()) {
            case "grow" -> buildGrowResponse(event);
            case "fuck" -> buildJinxResponse(event);
            case "casino" -> buildJackpotAbilityResponse(event);
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

    private String buildJinxResponse(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            return "Эта способность доступна только на сервере.";
        }

        User target = event.getOption("user", null, OptionMapping::getAsUser);
        if (target == null) {
            return "Нужно указать цель.";
        }

        conversationParticipantService.registerParticipant(Platform.DISCORD, target.getIdLong(), event.getGuild().getIdLong());
        return abilityService.increaseEnemyFailChance(
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

        conversationParticipantService.registerParticipant(Platform.DISCORD, target.getIdLong(), event.getGuild().getIdLong());
        return abilityService.reduceEnemyGrowth(
            Platform.DISCORD,
            event.getUser().getIdLong(),
            event.getGuild().getIdLong(),
            target.getIdLong()
        );
    }

    private String buildJackpotAbilityResponse(SlashCommandInteractionEvent event) {
        Long scopeId = event.isFromGuild() ? event.getGuild().getIdLong() : null;
        return abilityService.increaseOwnCritChance(Platform.DISCORD, scopeId, event.getUser().getIdLong());
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

    private String formatDiscordTag(net.dv8tion.jda.api.entities.User user) {
        String discriminator = user.getDiscriminator();
        if (discriminator == null || discriminator.equals("0")) {
            return user.getName();
        }
        return user.getName() + "#" + discriminator;
    }
}
