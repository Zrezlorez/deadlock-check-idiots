package com.litovskiy.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.litovskiy.dto.ActiveMatch;
import com.litovskiy.dto.MatchMetadata;
import com.litovskiy.dto.PlayerMatchHistory;
import com.litovskiy.dto.SteamProfileInfo;
import lombok.SneakyThrows;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Client {

    private final String BASE_URL = "https://api.deadlock-api.com/v1";
    private final String STEAM_URL = "https://api.steampowered.com";

    private final String PLAYERS_URL = BASE_URL + "/players";
    private final String MATCHES_URL = BASE_URL + "/matches";

    private final long steamId64 = 76561197960265728L;
    private final String steamApiKey = "15AD84382CA7748079686171927C2062";

    private final Gson gson = new Gson();


    /**
     * Получение истории матчей игрока
     * @param id игрока
     * @return Список (List) матчей игрока
     */
    public List<PlayerMatchHistory> getPlayerMatchesHistory(long id) {
        Type type = new TypeToken<List<PlayerMatchHistory>>() {}.getType();
        HttpResponse<String> response = sendGetRequest(
            PLAYERS_URL + id + "/match-history",
            HttpResponse.BodyHandlers.ofString()
        );
        return gson.fromJson(response.body(), type);
    }

    /**
     * Получение информации по профилям стим аккаунтов
     * @param ids список id игроков
     * @return Мапа пара ключ значения, id - Профиль
     */
    public Map<Long, SteamProfileInfo> getName(List<Long> ids) {
        String ids64 = ids
            .stream()
            .map(id32 -> steamId64 + id32)
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        String url = STEAM_URL + "/ISteamUser/GetPlayerSummaries/v0002/?key=" + steamApiKey
            + "&steamids=" + ids64;
        HttpResponse<String> response = sendGetRequest(url, HttpResponse.BodyHandlers.ofString(Charset.forName("windows-1251")));

        Type type = new TypeToken<List<SteamProfileInfo>>() {}.getType();
        List<SteamProfileInfo> profiles = gson.fromJson(
            getField(response.body(), "response").getAsJsonArray("players"),
            type
        );

        return profiles
            .stream()
            .collect(Collectors.toMap(k -> Long.parseLong(k.getSteamId()) - steamId64, e -> e));

    }

    /**
     * Получение информации по завершенному матчу. Не работает на идущие матчи
     * @param id матча
     * @return Инфа
     */
    public MatchMetadata getMatchMetadata(long id) {
        HttpResponse<String> response = sendGetRequest(
            MATCHES_URL + id + "/metadata",
            HttpResponse.BodyHandlers.ofString()
        );

        return gson.fromJson(getField(response.body(), "match_info"), MatchMetadata.class);
    }

    /**
     * Получение списка игроков и айди идущего матча, в котором участвует игрок с переданным id
     * @param id игрока
     * @return Айди матча и список игроков
     */
    public ActiveMatch getActualMatch(long id) {
        HttpResponse<String> response = sendGetRequest(MATCHES_URL + "/active", HttpResponse.BodyHandlers.ofString());
        Type type = new TypeToken<List<ActiveMatch>>() {}.getType();
        List<ActiveMatch> activeMatches = gson.fromJson(response.body(), type);
        return activeMatches
            .stream()
            .filter(match -> match
                .getPlayers()
                .stream()
                .anyMatch(player -> player.getAccountId().equals(id)))
            .findFirst()
            .orElseThrow(RuntimeException::new);
    }



    @SneakyThrows
    private <T> HttpResponse<T> sendGetRequest(String url, HttpResponse.BodyHandler<T> bodyHandler) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        return client.send(request, bodyHandler);
    }

    private JsonObject getField(String json, String field) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.getAsJsonObject(field);
    }

    private JsonArray getFieldList(String json, String field) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.getAsJsonArray(field);
    }
}
