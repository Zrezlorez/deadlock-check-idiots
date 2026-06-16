package com.litovskiy.entity;

import com.litovskiy.bot.tg.CallbackStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelegramCallbackRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "scope_id")
    private long scopeId;

    @Column(name = "message_id")
    private long messageId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CallbackStatus status;

    // прикрутить тип если еще появятся и прокинуть через CommandMessage

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Column(name = "complete_at", nullable = false)
    private Instant completeAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
