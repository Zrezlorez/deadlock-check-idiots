package com.litovskiy.repository;

import com.litovskiy.entity.TelegramCallbackVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramCallbackVoteRepository extends JpaRepository<TelegramCallbackVote, Long> {
}
