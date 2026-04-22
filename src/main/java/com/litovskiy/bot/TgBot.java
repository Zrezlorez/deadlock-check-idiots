package com.litovskiy.bot;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.SneakyThrows;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Setter
public class TgBot implements LongPollingSingleThreadUpdateConsumer {

    private static final String TOKEN = "1116496780:AAH8HZ8kDNoSQW3LNXKM8ladh434hCJfEls";
    private TelegramClient telegramClient;

    @SneakyThrows
    public static void start() {
        InetSocketAddress address = new InetSocketAddress("62.60.151.13", 49429);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, address);

        OkHttpClient client = new OkHttpClient.Builder()
            .proxy(proxy)
            .proxyAuthenticator((route, response) -> {
                String credential = Credentials.basic("zrezlorez", "test");
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            })
            .build();
        TgBot tgBot = new TgBot();
        tgBot.setTelegramClient(new OkHttpTelegramClient(client, TOKEN));
        TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> client);
        botsApplication.registerBot(TOKEN, tgBot);
    }


    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getText().equals("/hello")) {
                //String message_text = update.getMessage().getText();
                long chat_id = update.getMessage().getChatId();

                SendMessage message = SendMessage
                    .builder()
                    .chatId(chat_id)
                    .text("бубубу бебебе")
                    .build();

                try {
                    telegramClient.execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
