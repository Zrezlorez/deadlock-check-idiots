package com.litovskiy;

import com.litovskiy.bot.TgBot;
import com.litovskiy.dto.ActiveMatch;
import com.litovskiy.dto.MatchMetadata;
import com.litovskiy.dto.SteamProfileInfo;
import com.litovskiy.service.Client;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {


    public static void main(String[] args) {
        TgBot.start();
        Client client = new Client();
//        var match = client.getMatchMetadata(77122846);
//        List<MatchMetadata.MatchPlayers> players = new ArrayList<>(match.getPlayers());
//        players.sort(Comparator.comparingInt(MatchMetadata.MatchPlayers::getTeam));
//
//        List<Long> ids = players.stream().map(MatchMetadata.MatchPlayers::getAccountId).toList();
//

//
//
//        for (var z : players) {
//            System.out.println("Команда " + z.getTeam());
//            System.out.println("Зовут " + parsed.get(z.getAccountId()).getName());
//            System.out.println("КДА " + z.getKills() + "/" + z.getDeaths() + "/" + z.getAssists());
//            System.out.println("Герой " + z.getHeroId());
//            System.out.println("Нетворс " + z.getNetWorth());
//            System.out.println();
//        }

//        var a = client.getActualMatch(849067804);


    }


}