package com.litovskiy.entity;

import com.litovskiy.service.children.ChildrenAction;
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
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "children_care",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_children_care_children_date",
        columnNames = {"children_id", "care_date"}
    )
)
@NoArgsConstructor
@Getter
@Setter
public class ChildrenCare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "children_id")
    private Long childrenId;

    @Column(name = "care_date")
    private LocalDate careDate;

    @Column(name = "first_parent_action")
    @Enumerated(EnumType.STRING)
    private ChildrenAction firstParentAction;

    @Column(name = "second_parent_action")
    @Enumerated(EnumType.STRING)
    private ChildrenAction secondParentAction;

    @Column(name = "resolved")
    private boolean resolved;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "successful")
    private Boolean successful;

    @Column(name = "success_chance")
    private Double successChance;
}
