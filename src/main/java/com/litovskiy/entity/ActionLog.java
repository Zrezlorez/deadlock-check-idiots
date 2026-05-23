package com.litovskiy.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.litovskiy.log.Action;
import com.litovskiy.log.LogTag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private Action action;

    @Column(name = "old_actor_size")
    private Double oldActorSize;

    @Column(name = "new_actor_size")
    private Double newActorSize;

    @Column(name = "old_target_size")
    private Double oldTargetSize;

    @Column(name = "new_target_size")
    private Double newTargetSize;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false)
    private Set<LogTag> tags = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}