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
        String token = BotConfig.discordToken();
        if (token == null || token.isBlank()) {
            System.out.println("Discord bot is not enabled because token is empty");
            return;
        }
        OkHttpClient okHttpClient = HttpClientFactory.create();
        JDA jda = JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
            .setHttpClient(okHttpClient)
            .addEventListeners(new DiscordBot(appServices))
            .build();

        jda.awaitReady();
        registerCommands(jda);
        System.out.println("Discord bot started");
    }

    private static void registerCommands(JDA jda) {
        jda.updateCommands()
            .addCommands(
                Commands.slash("grow", "Вырастить показатель"),
                Commands.slash("fuck", "Повысить шанс неудачи у цели")
                    .addOption(OptionType.USER, "user", "Цель", true),
                Commands.slash("jackpot", "Повысить шанс джекпота и неудачи для себя"),
                Commands.slash("slow", "Урезать следующий рост цели")
                    .addOption(OptionType.USER, "user", "Цель", true),
                Commands.slash("turtle", "Увеличить себе рост бесплатно"),
                Commands.slash("pray", "Уменьшить себе шанс неудачи"),
                Commands.slash("transfer", "перевести часть роста другому игроку с комиссией (переводить можно только тем, у кого меньше)")
                    .addOption(OptionType.USER, "user", "Цель", true)
                    .addOption(OptionType.NUMBER, "value", "Размер", true),
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

        conversationParticipantService.registerParticipant(Platform.DISCORD, target.getIdLong(), event.getGuild().getIdLong());
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

        conversationParticipantService.registerParticipant(Platform.DISCORD, target.getIdLong(), event.getGuild().getIdLong());
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
        conversationParticipantService.registerParticipant(Platform.DISCORD, target.getIdLong(), event.getGuild().getIdLong());
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

    private String formatDiscordTag(net.dv8tion.jda.api.entities.User user) {
        String discriminator = user.getDiscriminator();
        if (discriminator.equals("0")) {
            return user.getName();
        }
        return user.getName() + "#" + discriminator;
    }
}
