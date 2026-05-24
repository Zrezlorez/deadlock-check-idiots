package com.litovskiy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PlayerGrowthStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "average_growth")
    private double averageGrowth;

    @Column(name = "current_lucky_streak")
    private int currentLuckyStreak;

    @Column(name = "current_fail_streak")
    private int currentFailStreak;

    @Column(name = "current_normal_streak")
    private int currentNormalStreak;

    @Column(name = "max_lucky_streak")
    private int maxLuckyStreak;

    @Column(name = "max_fail_streak")
    private int maxFailStreak;

    @Column(name = "max_normal_streak")
    private int maxNormalStreak;

    @Column(name = "total_crits")
    private int totalCrits;

    @Column(name = "total_fails")
    private int totalFails;

    @Column(name = "total_normal_growths")
    private int totalNormalGrowths;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public PlayerGrowthStats(Long playerId) {
        this.playerId = playerId;
    }

    public void increaseCurrentLuckyStreak() {
        currentLuckyStreak++;
    }

    public void increaseCurrentFailStreak() {
        currentFailStreak++;
    }

    public void increaseCurrentNormalStreak() {
        currentNormalStreak++;
    }

    public void increaseTotalCrits() {
        totalCrits++;
    }

    public void increaseTotalFails() {
        totalFails++;
    }

    public void increaseTotalNormalGrowths() {
        totalNormalGrowths++;
    }
}
