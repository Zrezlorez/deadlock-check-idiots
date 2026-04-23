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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Setter
public class TgBot implements LongPollingSingleThreadUpdateConsumer {

    private static final String TOKEN = "1116496780:AAH8HZ8kDNoSQW3LNXKM8ladh434hCJfEls";
    private TelegramClient telegramClient;

    private Map<Long,Double> size = new HashMap<>();
    private Map<Long,LocalDateTime> time = new HashMap<>();
    private Random random = new Random();

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
            if (update.getMessage().getText().equals("/grow")) {
                long chat_id = update.getMessage().getChatId();

                LocalDateTime localTime = time.getOrDefault(chat_id, LocalDateTime.of(2000, 1, 1, 1, 1, 1));

                SendMessage message;
                if (ChronoUnit.MINUTES.between(localTime, LocalDateTime.now()) <= 1) {
                    double pisya = size.getOrDefault(chat_id, 2.0) * random.nextDouble(0.9, 1.5);
                    double scale = Math.pow(10, 2);
                    size.put(chat_id, Math.ceil(pisya * scale) / scale);
                    message = SendMessage
                        .builder()
                        .chatId(chat_id)
                        .text("Попробуйте снова в " + localTime.plusMinutes(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build();
                } else {
                    message = SendMessage
                        .builder()
                        .chatId(chat_id)
                        .text("Ваша пися теперь " + size.get(chat_id) + " см")
                        .build();
                    time.put(chat_id, LocalDateTime.now());
                }


                try {
                    telegramClient.execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
