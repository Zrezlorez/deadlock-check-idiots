package com.litovskiy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "link_codes")
@Getter
@Setter
@NoArgsConstructor
public class LinkCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform", nullable = false, length = 16)
    private Platform sourcePlatform;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public LinkCode(String code, Long playerId, Platform sourcePlatform, LocalDateTime expiresAt) {
        this.code = code;
        this.playerId = playerId;
        this.sourcePlatform = sourcePlatform;
        this.expiresAt = expiresAt;
    }
}
