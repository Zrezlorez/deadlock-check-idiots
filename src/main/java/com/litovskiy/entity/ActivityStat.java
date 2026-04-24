package com.litovskiy.entity;

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

import java.time.LocalDate;

@Entity
@Table(
    name = "activity_stats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_activity_stats_player_platform_scope_date",
            columnNames = {"player_chat_id", "platform", "scope_id", "activity_date"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ActivityStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_chat_id", nullable = false)
    private Long playerChatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    private Platform platform;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "message_count", nullable = false)
    private long messageCount;

    @Column(name = "voice_seconds", nullable = false)
    private long voiceSeconds;

    public ActivityStat(Long playerChatId, Platform platform, Long scopeId, LocalDate activityDate) {
        this.playerChatId = playerChatId;
        this.platform = platform;
        this.scopeId = scopeId;
        this.activityDate = activityDate;
        this.messageCount = 0;
        this.voiceSeconds = 0;
    }
}
