package com.litovskiy.entity;

import com.litovskiy.log.Action;
import com.litovskiy.log.PlayerArchetype;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "player_behavior_stats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_id", columnNames = {"player_id"})
    }
)
public class PlayerBehaviorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

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

    @Column(name = "last_ability_action")
    @Enumerated(EnumType.STRING)
    private Action lastAbilityAction;

    @Column(name = "same_ability_streak")
    private int sameAbilityStreak;

    @Enumerated(EnumType.STRING)
    @Column(name = "archetype")
    private PlayerArchetype archetype;

    public PlayerBehaviorStats(Long playerId) {
        this.playerId = playerId;
    }

    public void incrementAbilityStreak() {
        sameAbilityStreak++;
    }

}
