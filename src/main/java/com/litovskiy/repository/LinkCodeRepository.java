package com.litovskiy.repository;

import com.litovskiy.entity.LinkCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LinkCodeRepository extends JpaRepository<LinkCode, Long> {

    LinkCode findByCode(String code);
    LinkCode findByPlayerId(Long playerId);
    LinkCode deleteLinkCodesByExpiresAtBefore(LocalDateTime expired);

}
