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

@Entity
@Table(
    name = "conversation_participants",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_conversation_participants_player_platform_scope",
            columnNames = {"player_id", "platform", "scope_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ConversationParticipant {

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

    public ConversationParticipant(Long playerId, Platform platform, Long scopeId) {
        this.playerId = playerId;
        this.platform = platform;
        this.scopeId = scopeId;
    }
}
