package com.litovskiy.entity;

import com.litovskiy.service.children.ChildrenStatus;
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
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Children {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "scope_id")
    private long scopeId;

    @Column(name = "first_player")
    private long firstPlayer;

    @Column(name = "second_player")
    private long secondPlayer;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ChildrenStatus status;

    @Column(name = "health")
    private int health;

    @Column(name = "streak")
    private int streak;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Children(long scopeId, Player firstPlayer, Player secondPlayer) {
        this.scopeId = scopeId;
        if (firstPlayer.getId() < secondPlayer.getId()) {
            this.firstPlayer = firstPlayer.getId();
            this.secondPlayer = secondPlayer.getId();
        } else {
            this.firstPlayer = secondPlayer.getId();
            this.secondPlayer = firstPlayer.getId();
        }
        this.status = ChildrenStatus.ACTIVE;
        this.health = 100;
    }
}
