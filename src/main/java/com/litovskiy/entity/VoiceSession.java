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

import java.time.LocalDateTime;

@Entity
@Table(
    name = "voice_sessions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_voice_sessions_player_platform", columnNames = {"player_id", "platform"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class VoiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    private Platform platform;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    public VoiceSession(Long playerId, Platform platform, Long scopeId, LocalDateTime startedAt) {
        this.playerId = playerId;
        this.platform = platform;
        this.scopeId = scopeId;
        this.startedAt = startedAt;
    }
}
