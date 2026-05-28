package com.litovskiy.entity;

import com.litovskiy.bot.PlayerDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class TelegramCallbackVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "request_id")
    private long requestId;

    @Column(name = "player_id")
    private long playerId;

    @Column(name = "player_decision")
    private PlayerDecision playerDecision;

    @Column(name = "voted_dt")
    private Instant votedAt;

    @Column(name = "auto_selected")
    private boolean autoSelected;
}
