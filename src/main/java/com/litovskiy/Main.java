package com.litovskiy;

import com.litovskiy.bot.DiscordBot;
import com.litovskiy.bot.TgBot;
import com.litovskiy.service.AppServices;

public class Main {

    public static void main(String[] args) {
        AppServices appServices = new AppServices();
        //DiscordBot.start(appServices);
        TgBot.start(appServices);
    }

}
