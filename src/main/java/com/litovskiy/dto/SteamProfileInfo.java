package com.litovskiy.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class SteamProfileInfo {
    @SerializedName("steamid")
    private String steamId;
    @SerializedName("personaname")
    private String name;
}