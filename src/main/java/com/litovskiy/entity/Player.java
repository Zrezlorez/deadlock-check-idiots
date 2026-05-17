package com.litovskiy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "size")
    private double size;

    @Column(name = "last_grow_time")
    private LocalDateTime lastGrowTime;

    @Column(name = "last_ability_time")
    private LocalDateTime lastAbilityTime;

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

    @Column(name = "pending_fail_chance_penalty", nullable = false, columnDefinition = "double precision default 0")
    private double pendingFailChancePenalty;

    @Column(name = "pending_crit_chance_bonus", nullable = false, columnDefinition = "double precision default 0")
    private double pendingCritChanceBonus;

    @Column(name = "pending_growth_penalty", nullable = false, columnDefinition = "double precision default 0")
    private double pendingGrowthPenalty;

    @Column(name = "pending_growth_bonus", nullable = false, columnDefinition = "double precision default 0")
    private double pendingGrowthBonus;

    public Player(double size) {
        this.size = size;
        this.lastGrowTime = null;
        this.lastAbilityTime = null;
        this.pendingFailChancePenalty = 0.0;
        this.pendingCritChanceBonus = 0.0;
        this.pendingGrowthPenalty = 0.0;
        this.pendingGrowthBonus = 0.0;
    }
}
