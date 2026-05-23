package com.litovskiy.entity;

import com.litovskiy.log.PlayerArchetype;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
class PlayerBehaviorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "aggressive_actions")
    private int aggressiveActions;

    @Column(name = "support_actions")
    private int supportActions;

    @Column(name = "risk_actions")
    private int riskActions;

    @Column(name = "defensive_actions")
    private int defensiveActions;

    @Column(name = "crits")
    private int crits;

    @Column(name = "fails")
    private int fails;

    // изменение размера последние n ростов
    @Column(name = "average_growth")
    private double averageGrowth;

    @Enumerated(EnumType.STRING)
    @Column(name = "archetype")
    private PlayerArchetype archetype;
}
