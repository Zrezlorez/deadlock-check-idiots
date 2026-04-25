package com.litovskiy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "players",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_players_telegram_chat_id", columnNames = "telegram_chat_id"),
        @UniqueConstraint(name = "uk_players_discord_user_id", columnNames = "discord_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "size")
    private double size;

    @Column(name = "last_grow_time")
    private LocalDateTime lastGrowTime;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "discord_user_id")
    private Long discordUserId;

    @Column(name = "telegram_display_name")
    private String telegramDisplayName;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "discord_tag")
    private String discordTag;

    public Player(Long chatId, double size) {
        this.chatId = chatId;
        this.size = size;
        this.lastGrowTime = null;
    }
}
