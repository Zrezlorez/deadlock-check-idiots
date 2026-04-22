package com.litovskiy.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class ActiveMatch {
    @SerializedName("match_id")
    private Long matchId;
    private List<PlayerInfo> players;

    @Data
    public static class PlayerInfo {
        @SerializedName("account_id")
        private Long accountId;
        private Integer team;
        @SerializedName("hero_id")
        private Integer heroId;
    }
}
