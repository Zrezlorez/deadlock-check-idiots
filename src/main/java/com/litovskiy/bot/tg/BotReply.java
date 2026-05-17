package com.litovskiy.bot.tg;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BotReply {
    private String text;
    private boolean html;
}
