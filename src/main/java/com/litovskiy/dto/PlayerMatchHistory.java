package com.litovskiy.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PlayerMatchHistory {

    @SerializedName("account_id")
    private Long accountId;

    @SerializedName("match_id")
    private Long matchId;

    @SerializedName("hero_id")
    private Long heroId;

    @SerializedName("hero_level")
    private Long heroLevel;

    @SerializedName("start_time")
    private Long startTime;

    @SerializedName("game_mode")
    private Long gameMode;

    @SerializedName("match_mode")
    private Long match_Mode;

    @SerializedName("player_team")
    private Long playerTeam;

    @SerializedName("player_kills")
    private Long playerKills;

    @SerializedName("player_deaths")
    private Long playerDeaths;

    @SerializedName("player_assists")
    private Long playerAssists;

    private Long denies;

    @SerializedName("net_worth")
    private Long netWorth;

    @SerializedName("last_hits")
    private Long lastHits;

    @SerializedName("team_abandoned")
    private Boolean teamAbandoned;

    @SerializedName("abandoned_time_s")
    private Long abandonedTimeInSec;

    @SerializedName("match_duration_s")
    private Long matchDurationInSec;

    @SerializedName("match_result")
    private Long matchResult;

    @SerializedName("objectives_mask_team0")
    private Long objectivesMaskTeam0;

    @SerializedName("objectives_mask_team1")
    private Long objectivesMaskTeam1;

    @SerializedName("brawl_score_team0")
    private Long brawlScoreTeam0;

    @SerializedName("brawl_score_team1")
    private Long brawlScoreTeam1;

    @SerializedName("brawl_avg_round_time_s")
    private Long brawlAvgRoundTimeInSec;
}
