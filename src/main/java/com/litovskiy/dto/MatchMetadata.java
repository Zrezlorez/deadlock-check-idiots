package com.litovskiy.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class MatchMetadata {

    private List<MatchPlayers> players;
    @SerializedName("winning_team")
    private Integer winningTeam;
    @SerializedName("duration_s")
    private Integer durationInSec;


    @Data
    public static class MatchPlayers {
        @SerializedName("account_id")
        private Long accountId;
        private Integer team;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        @SerializedName("net_worth")
        private Integer netWorth;
        @SerializedName("hero_id")
        private Integer heroId;
    }
}
