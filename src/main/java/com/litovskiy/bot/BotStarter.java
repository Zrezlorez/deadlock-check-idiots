package com.litovskiy.bot;

import com.litovskiy.bot.ds.DiscordBot;
import com.litovskiy.bot.tg.TgBot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotStarter {
    private final TgBot tgBot;
    private final DiscordBot discordBot;

    @PostConstruct
    public void run() {
        tgBot.start();
        discordBot.start();
    }
}
