package com.litovskiy.bot.ds;

import com.litovskiy.entity.Platform;
import com.litovskiy.service.ConversationParticipantService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.activity.ActivityService;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordBot extends ListenerAdapter {

    private final DiscordResponseBuilder discordResponseBuilder;
    private final ActivityService activityService;
    private final ConversationParticipantService conversationParticipantService;
    private final PlayerAccountService playerAccountService;
    private final OkHttpClient httpClient;
    private final DiscordMessageSender sender;

    @Value("${discord.token}")
    private String token;

    @SneakyThrows
    public void start() {
        if (token == null || token.isBlank()) {
            log.warn("Discord bot is not enabled because token is empty");
            return;
        }
        JDA jda = JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES)
            .setHttpClient(httpClient)
            .addEventListeners(this)
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
                Commands.slash("profile", "Посмотреть профиль (свой или другого игрока)"),
                Commands.slash("admin", "Админ-команды")
                    .addOption(OptionType.STRING, "command", "Например: config, player show telegram @name, player show discord user#1234", false)
            )
            .queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        playerAccountService.updateDiscordTag(event.getUser().getIdLong(), StringUtil.formatDiscordTag(event.getUser()));
        if (event.isFromGuild()) {
            conversationParticipantService.registerParticipant(
                Platform.DISCORD,
                event.getUser().getIdLong(),
                event.getGuild().getIdLong()
            );
        }
        CommandResult response = discordResponseBuilder.buildResponse(event);

        sender.sendMessages(event, response);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        playerAccountService.updateDiscordTag(event.getAuthor().getIdLong(), StringUtil.formatDiscordTag(event.getAuthor()));
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
}
