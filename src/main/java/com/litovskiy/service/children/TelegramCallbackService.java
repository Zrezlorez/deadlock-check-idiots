package com.litovskiy.service.children;

import com.litovskiy.bot.ButtonSpec;
import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.KeyboardSpec;
import com.litovskiy.bot.tg.CallbackResult;
import com.litovskiy.bot.tg.CallbackStatus;
import com.litovskiy.bot.tg.PlayerDecision;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.TelegramCallbackRequest;
import com.litovskiy.entity.TelegramCallbackVote;
import com.litovskiy.repository.TelegramCallbackRequestRepository;
import com.litovskiy.repository.TelegramCallbackVoteRepository;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.util.TelegramCallbackMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramCallbackService {
    private final TelegramCallbackRequestRepository requestRepository;
    private final TelegramCallbackVoteRepository voteRepository;
    private final PlayerService playerService;
    private final ChildrenService childrenService;
    private final Clock clock;

    @Transactional
    public CommandMessage registerNewCallback(long scopeId, Player father, Player mother) {
        TelegramCallbackRequest request = new TelegramCallbackRequest();
        request.setScopeId(scopeId);
        request.setStatus(CallbackStatus.ACTIVE);
        request.setExpiredAt(Instant.now(clock).plusSeconds(6 * 60 * 60));

        request = requestRepository.save(request);

        TelegramCallbackVote fatherVote = new TelegramCallbackVote();
        fatherVote.setPlayerId(father.getId());
        fatherVote.setRequestId(request.getId());
        fatherVote.setMother(false);

        TelegramCallbackVote motherVote = new TelegramCallbackVote();
        motherVote.setPlayerId(mother.getId());
        motherVote.setRequestId(request.getId());
        motherVote.setMother(true);

        voteRepository.save(fatherVote);
        voteRepository.save(motherVote);

        return CommandMessage.broadcast(
            getBirthText(mother, null, father, null),
            birthKeyboard(),
            request
        );
    }

    public void setMessageCallback(TelegramCallbackRequest request, long messageId) {
        request.setMessageId(messageId);
        requestRepository.save(request);
    }

    @Transactional
    public CallbackResult handleCallback(long scopeId, long messageId, Long actorId, String callback) {
        if (ChildrenCareCallback.matches(callback)) {
            return handleCareCallback(scopeId, messageId, actorId, callback);
        }

        return handleBirthCallback(scopeId, messageId, actorId, callback);
    }

    private CallbackResult handleBirthCallback(long scopeId, long messageId, Long actorId, String callback) {
        Player actor = playerService.findByPlatform(Platform.TELEGRAM, actorId);
        if (actor == null) {
            return CallbackResult.answer("Игрок не найден");
        }

        var request = requestRepository.findByScopeIdAndMessageId(scopeId, messageId);
        if (request == null || request.getStatus() != CallbackStatus.ACTIVE) {
            return CallbackResult.answer("Запрос уже неактивен");
        }

        var vote = voteRepository.findByRequestIdAndPlayerId(request.getId(), actor.getId());
        if (vote == null) {
            return CallbackResult.answer("Ты не участвуешь в этом голосовании");
        }

        if (vote.getPlayerDecision() != null) {
            return CallbackResult.answer("Ты уже проголосовал");
        }

        TelegramCallbackMessage message;
        try {
            message = TelegramCallbackMessage.fromMessage(callback);
        } catch (IllegalArgumentException e) {
            return CallbackResult.answer("Некорректное действие");
        }

        switch (message) {
            case ACCEPT -> vote.setPlayerDecision(PlayerDecision.ACCEPT);
            case ABORT -> vote.setPlayerDecision(PlayerDecision.DECLINE);
        }
        vote.setVotedAt(Instant.now(clock));
        voteRepository.save(vote);

        List<TelegramCallbackVote> votes = voteRepository.findByRequestId(request.getId());
        TelegramCallbackVote motherVote = votes.stream()
            .filter(TelegramCallbackVote::isMother)
            .findFirst()
            .orElseThrow();

        TelegramCallbackVote fatherVote = votes.stream()
            .filter(v -> !v.isMother())
            .findFirst()
            .orElseThrow();

        Player mother = playerService.findById(motherVote.getPlayerId());
        Player father = playerService.findById(fatherVote.getPlayerId());

        boolean allVoted = votes.stream()
            .allMatch(v -> v.getPlayerDecision() != null);

        boolean allAccepted = votes.stream()
            .allMatch(v -> v.getPlayerDecision() == PlayerDecision.ACCEPT);

        if (allVoted) {
            request.setStatus(allAccepted ? CallbackStatus.APPROVED : CallbackStatus.CANCELLED);
            request.setCompleteAt(Instant.now(clock));
            requestRepository.save(request);

            if (allAccepted) {
                childrenService.registry(scopeId, mother, father);
                return CallbackResult.edit(getSuccessText(), null);
            }

            return CallbackResult.edit(getDeclineText(mother, father, motherVote, fatherVote), null);
        }

        return CallbackResult.edit(
            getBirthText(
                mother,
                motherVote.getPlayerDecision(),
                father,
                fatherVote.getPlayerDecision()
            ),
            birthKeyboard()
        );
    }

    private CallbackResult handleCareCallback(long scopeId, long messageId, Long actorTelegramId, String callback) {
        Optional<ChildrenCareCallback> callbackData = ChildrenCareCallback.decode(callback);
        if (callbackData.isEmpty()) {
            return CallbackResult.answer("Некорректное действие ухода");
        }

        ChildrenCareCallback careCallback = callbackData.get();
        ChildrenService.CareActionResult result = childrenService.applyCare(
            scopeId,
            actorTelegramId,
            careCallback.childrenId(),
            careCallback.careDate(),
            careCallback.action(),
            messageId
        );

        if (!result.accepted()) {
            return CallbackResult.answer(result.text());
        }

        return CallbackResult.edit(
            result.text(),
            result.resolved()
                ? null
                : ChildrenKeyboardFactory.care(result.childrenId(), result.careDate())
        );
    }

    private KeyboardSpec birthKeyboard() {
        return KeyboardSpec.row(
            new ButtonSpec("Оставить", TelegramCallbackMessage.ACCEPT.getMessage()),
            new ButtonSpec("Аборт", TelegramCallbackMessage.ABORT.getMessage())
        );
    }

    private String getBirthText(
        Player mother,
        PlayerDecision motherDecision,
        Player father,
        PlayerDecision fatherDecision
    ) {
        return "Ой-ой... Кажется, вы слишком часто занимались /fuck, и теперь игрок " +
            mother.getTelegramDisplayName() +
            " залетел!\n\n" +
            "Новоиспечённым родителям нужно вместе решить — оставлять ребёнка или нет. " +
            "К сожалению, он не может расти в неполной семье, так что требуется согласие обеих сторон.\n\n" +
            "Если откажетесь... что ж, тогда вас ждёт божья кара за детоубийство.\n\n" +
            "Решения игроков:\n" +
            formatDecision(mother, motherDecision) +
            "\n" +
            formatDecision(father, fatherDecision);
    }

    private String formatDecision(Player player, PlayerDecision decision) {
        String decisionText = decision == null
            ? "ещё не выбрал"
            : decision.getText();

        return player.getTelegramDisplayName() + " — " + decisionText;
    }

    private String getSuccessText() {
        return """
            Роды завершились!

            Оба игрока согласились оставить ребёнка. Что же теперь с ним делать дальше?
            Думаю вы это поймете завтра в 12:00 МСК
            """;
    }

    private String getDeclineText(
        Player mother,
        Player father,
        TelegramCallbackVote motherVote,
        TelegramCallbackVote fatherVote
    ) {
        return """
            К вашему счастью, роды даже не начались!
            Но несоответствие традиционным ценностям несет за собой наказание.
            У обоих родителей уменьшен следующий рост на 50%% и повышен шанс неудачи на 25%%

            Решения игроков:
            %s — %s
            %s — %s
            """.formatted(
            mother.getTelegramDisplayName(),
            motherVote.getPlayerDecision().getText(),
            father.getTelegramDisplayName(),
            fatherVote.getPlayerDecision().getText()
        );
    }
}
