package com.litovskiy.bot;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.litovskiy.dao.GenericDao;
import com.litovskiy.entity.Player;
import com.litovskiy.service.DickService;
import com.litovskiy.util.PropsManager;
import com.litovskiy.util.ProxyManager;
import lombok.Setter;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Setter
public class TgBot implements LongPollingSingleThreadUpdateConsumer {

    private static final String TOKEN = "1116496780:AAH8HZ8kDNoSQW3LNXKM8ladh434hCJfEls";
    private TelegramClient telegramClient;
    private DickService dickService = new DickService(new GenericDao<>(Player.class));

    @SneakyThrows
    public static void start() {
        TgBot tgBot = new TgBot();

        TelegramBotsLongPollingApplication botsApplication;

        String isEnabledProxy = PropsManager.getProps().getProperty("proxy.isEnabled");
        if (Boolean.parseBoolean(isEnabledProxy)) {
            OkHttpClient okHttpClient = ProxyManager.getOkHttpClient();
            tgBot.setTelegramClient(new OkHttpTelegramClient(okHttpClient, TOKEN));
            botsApplication = new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> okHttpClient);
        } else {
            OkHttpClient okHttpClient = new OkHttpClient();
            tgBot.setTelegramClient(new OkHttpTelegramClient(okHttpClient, TOKEN));
            botsApplication = new TelegramBotsLongPollingApplication();
        }

        botsApplication.registerBot(TOKEN, tgBot);
    }


    @Override
    public void consume(Update update) {
        if (!update.hasMessage() && update.getMessage().hasText()) {
            return;
        }

        if (!update.getMessage().getText().equals("/grow")) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        SendMessage message = SendMessage
            .builder()
            .chatId(chatId)
            .text(dickService.grow(chatId))
            .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
