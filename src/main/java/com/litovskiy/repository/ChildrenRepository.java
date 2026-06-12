package com.litovskiy.repository;

import com.litovskiy.entity.Children;
import com.litovskiy.service.children.ChildrenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChildrenRepository extends JpaRepository<Children, Long> {

    Optional<Children> findByScopeIdAndFirstPlayerAndSecondPlayer(
        long scopeId,
        long firstPlayerId,
        long secondPlayerId
    );

    List<Children> findByStatus(ChildrenStatus status);
}
