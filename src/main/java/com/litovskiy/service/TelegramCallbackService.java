package com.litovskiy.service;

import com.litovskiy.bot.tg.CallbackStatus;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.TelegramCallbackRequest;
import com.litovskiy.entity.TelegramCallbackVote;
import com.litovskiy.repository.TelegramCallbackRequestRepository;
import com.litovskiy.repository.TelegramCallbackVoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TelegramCallbackService {

    private final TelegramCallbackRequestRepository requestRepository;
    private final TelegramCallbackVoteRepository voteRepository;

    @Transactional
    public void registerNewCallback(long scopeId, Player actor, Player target) {
        TelegramCallbackRequest request = new TelegramCallbackRequest();
        request.setScopeId(scopeId);
        request.setStatus(CallbackStatus.ACTIVE);
        request.setExpiredAt(Instant.now().plus(6, ChronoUnit.HOURS));

        request = requestRepository.save(request);

        TelegramCallbackVote actorVote = new TelegramCallbackVote();
        actorVote.setPlayerId(actor.getId());
        actorVote.setRequestId(request.getId());

        TelegramCallbackVote targetVote = new TelegramCallbackVote();
        targetVote.setPlayerId(target.getId());
        targetVote.setRequestId(request.getId());

        voteRepository.save(actorVote);
        voteRepository.save(targetVote);

    }
}
