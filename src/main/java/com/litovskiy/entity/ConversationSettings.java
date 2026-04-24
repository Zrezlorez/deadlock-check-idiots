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
    name = "conversation_settings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_conversation_settings_platform_scope", columnNames = {"platform", "scope_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ConversationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    private Platform platform;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "growth_style", nullable = false, length = 64)
    private GrowthStyle growthStyle;

    @Column(name = "manager_profile_id")
    private Long managerProfileId;

    public ConversationSettings(Platform platform, Long scopeId) {
        this.platform = platform;
        this.scopeId = scopeId;
        this.growthStyle = GrowthStyle.DICK;
    }
}
