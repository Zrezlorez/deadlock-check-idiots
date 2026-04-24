package com.litovskiy.bot;

import com.litovskiy.dao.GenericDao;
import com.litovskiy.entity.Player;
import com.litovskiy.service.DickService;
import com.litovskiy.util.PropsManager;
import com.litovskiy.util.ProxyManager;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import okhttp3.OkHttpClient;

public class DiscordBot extends ListenerAdapter {

    private static final String TOKEN =  "MTA2NTMyMTM1MDY2OTAyOTQ2Ng.G-tnAF.OH6pexLYNwEp7I1C8qDQ5LYr9eSvC1CgpLKnOw";
    private final DickService dickService = new DickService(new GenericDao<>(Player.class));

    @SneakyThrows
    public static void start() {
        OkHttpClient okHttpClient = new OkHttpClient();
        String isEnabledProxy = PropsManager.getProps().getProperty("proxy.isEnabled");
        if (Boolean.parseBoolean(isEnabledProxy)) {
            okHttpClient = ProxyManager.getOkHttpClient();
        }
        JDA jda = JDABuilder.createDefault(TOKEN)
            .setHttpClient(okHttpClient)
            .addEventListeners(new DiscordBot())
            .build();

        jda.awaitReady();

        jda.updateCommands()
            .addCommands(
                Commands.slash("grow", "Вырастить член")
            )
            .queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("grow")) {
            event.reply(dickService.grow(event.getMember().getIdLong())).queue();
        }
    }
}
