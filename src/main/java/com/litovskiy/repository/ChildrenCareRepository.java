package com.litovskiy.repository;

import com.litovskiy.entity.ChildrenCare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChildrenCareRepository extends JpaRepository<ChildrenCare, Long> {
    Optional<ChildrenCare> findByChildrenIdAndCareDate(long childrenId, LocalDate careDate);

    List<ChildrenCare> findByResolvedFalseAndCareDateBefore(LocalDate careDate);
}
