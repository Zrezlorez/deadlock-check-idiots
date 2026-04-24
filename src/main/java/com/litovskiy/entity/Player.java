package com.litovskiy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "size")
    private double size;

    @Column(name = "last_grow_time")
    private LocalDateTime lastGrowTime;

    public Player(Long chatId, double size) {
        this.chatId = chatId;
        this.size = size;
        this.lastGrowTime = LocalDateTime.MIN;
    }
}