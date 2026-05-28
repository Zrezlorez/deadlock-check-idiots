package com.litovskiy.repository;

import com.litovskiy.entity.ActionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
    List<ActionLog> findByActorIdOrderByCreatedAtDesc(Long id, Pageable pageable);
    List<ActionLog> findByTargetIdOrderByCreatedAtDesc(Long id, Pageable pageable);
}
