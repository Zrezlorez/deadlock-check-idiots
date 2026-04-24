package com.litovskiy;

import com.litovskiy.bot.DiscordBot;
import com.litovskiy.bot.TgBot;

public class Main {

    public static void main(String[] args) {
        DiscordBot.start();
        TgBot.start();
    }

}