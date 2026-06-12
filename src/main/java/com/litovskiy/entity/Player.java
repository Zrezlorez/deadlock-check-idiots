package com.litovskiy.entity;

import com.litovskiy.service.ability.PlayerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "pending_fail_chance_modifier", nullable = false, columnDefinition = "double precision default 0")
    private double pendingFailChanceModifier;

    @Column(name = "pending_crit_chance_modifier", nullable = false, columnDefinition = "double precision default 0")
    private double pendingCritChanceModifier;

    @Column(name = "pending_growth_modifier", nullable = false, columnDefinition = "double precision default 0")
    private double pendingGrowthModifier;

    // можно будет в отдельную сущность перенести при новых эффектах
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PlayerStatus status = PlayerStatus.NONE;

    @Column(name = "status_until")
    private LocalDateTime statusUntil;

    public Player(double size) {
        this.size = size;
        this.lastGrowTime = null;
        this.lastAbilityTime = null;
        this.pendingFailChanceModifier = 0.0;
        this.pendingCritChanceModifier = 0.0;
        this.pendingGrowthModifier = 0.0;
    }

    public void addPendingGrowthModifier(double value) {
        pendingGrowthModifier += value;
    }
}
