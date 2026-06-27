package com.litovskiy.service.children;

import com.litovskiy.bot.KeyboardSpec;
import com.litovskiy.config.properties.ChildrenProperties;
import com.litovskiy.entity.Children;
import com.litovskiy.entity.ChildrenCare;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.repository.ChildrenCareRepository;
import com.litovskiy.repository.ChildrenRepository;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.util.StringUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class ChildrenService {
    private final ChildrenRepository childrenRepository;
    private final ChildrenCareRepository careRepository;
    private final ChildrenProperties childrenProperties;
    private final PlayerService playerService;
    private final Clock clock;
    private final Random random;

    @Transactional
    public void registry(long scopeId, Player firstPlayer, Player secondPlayer) {
        long firstPlayerId = Math.min(firstPlayer.getId(), secondPlayer.getId());
        long secondPlayerId = Math.max(firstPlayer.getId(), secondPlayer.getId());

        childrenRepository.findByScopeIdAndFirstPlayerAndSecondPlayer(scopeId, firstPlayerId, secondPlayerId)
            .orElseGet(() -> {
                Children children = new Children(scopeId, firstPlayer, secondPlayer);
                children.setHealth(childrenProperties.getMaxHealth());
                children.setStreak(0);
                return childrenRepository.save(children);
            });
    }

    @Transactional
    public DailyCareDispatch prepareDailyCareDispatch() {
        LocalDate today = LocalDate.now(clock);
        List<CareMessageEdit> edits = resolveOverdueCare(today);
        List<DailyCareMessage> messages = new ArrayList<>();

        for (Children children : childrenRepository.findByStatus(ChildrenStatus.ACTIVE)) {
            ChildrenCare care = getOrCreateCare(children, today);
            if (care.isResolved() || care.getMessageId() != null) {
                continue;
            }

            messages.add(new DailyCareMessage(
                care.getId(),
                children.getScopeId(),
                children.getId(),
                care.getCareDate(),
                buildCareText(children, care, null),
                ChildrenKeyboardFactory.care(children.getId(), care.getCareDate())
            ));
        }

        return new DailyCareDispatch(messages, edits);
    }

    @Transactional
    public void setCareMessageId(long careId, long messageId) {
        careRepository.findById(careId).ifPresent(care -> {
            care.setMessageId(messageId);
            careRepository.save(care);
        });
    }

    @Transactional
    public CareActionResult applyCare(
        long scopeId,
        long actorTelegramId,
        long childrenId,
        LocalDate careDate,
        ChildrenAction action,
        long messageId
    ) {
        LocalDate today = LocalDate.now(clock);
        if (!careDate.equals(today)) {
            return CareActionResult.error("Этот день ухода уже закрыт.");
        }

        Player actor = playerService.findByPlatform(Platform.TELEGRAM, actorTelegramId);
        if (actor == null) {
            return CareActionResult.error("Игрок не найден.");
        }

        Optional<Children> childrenOptional = childrenRepository.findById(childrenId);
        if (childrenOptional.isEmpty()) {
            return CareActionResult.error("Ребенок не найден.");
        }

        Children children = childrenOptional.get();
        if (children.getScopeId() != scopeId || children.getStatus() != ChildrenStatus.ACTIVE) {
            return CareActionResult.error("Ребенок уже неактивен.");
        }

        boolean firstParent = actor.getId().equals(children.getFirstPlayer());
        boolean secondParent = actor.getId().equals(children.getSecondPlayer());
        if (!firstParent && !secondParent) {
            return CareActionResult.error("Ты не родитель этого ребенка.");
        }

        ChildrenCare care = getOrCreateCare(children, careDate);
        if (care.isResolved()) {
            return CareActionResult.error("Сегодня за ребенком уже ухаживали.");
        }

        care.setMessageId(messageId);
        ChildrenAction currentAction = firstParent
            ? care.getFirstParentAction()
            : care.getSecondParentAction();

        if (currentAction != null) {
            return CareActionResult.error("Ты уже выбрал действие на сегодня.");
        }

        if (firstParent) {
            care.setFirstParentAction(action);
        } else {
            care.setSecondParentAction(action);
        }

        boolean resolved = care.getFirstParentAction() != null && care.getSecondParentAction() != null;
        if (resolved) {
            resolveCare(children, care);
        }

        careRepository.save(care);
        childrenRepository.save(children);

        String eventText = "";
        if (resolved) {
            eventText += "\n\n" + buildResolvedEventText(care);
        }

        return CareActionResult.success(
            resolved,
            children.getId(),
            care.getCareDate(),
            buildCareText(children, care, eventText)
        );
    }

    private ChildrenCare getOrCreateCare(Children children, LocalDate careDate) {
        return careRepository.findByChildrenIdAndCareDate(children.getId(), careDate)
            .orElseGet(() -> {
                ChildrenCare care = new ChildrenCare();
                care.setChildrenId(children.getId());
                care.setCareDate(careDate);
                return careRepository.save(care);
            });
    }

    private List<CareMessageEdit> resolveOverdueCare(LocalDate today) {
        List<CareMessageEdit> edits = new ArrayList<>();

        for (ChildrenCare care : careRepository.findByResolvedFalseAndCareDateBefore(today)) {
            Optional<Children> childrenOptional = childrenRepository.findById(care.getChildrenId());
            if (childrenOptional.isEmpty()) {
                continue;
            }
            Children children = childrenOptional.get();

            if (children.getStatus() == ChildrenStatus.ACTIVE) {
                resolveCare(children, care);
                childrenRepository.save(children);

                if (care.getMessageId() != null) {
                    edits.add(new CareMessageEdit(
                        children.getScopeId(),
                        care.getMessageId(),
                        buildCareText(
                            children,
                            care,
                            "Событие завершено автоматически.\n" + buildResolvedEventText(care)
                        )
                    ));
                }
            }

            careRepository.save(care);
        }

        return edits;
    }

    private void resolveCare(Children children, ChildrenCare care) {
        double successChance = getSuccessChance(care);
        boolean success = random.nextDouble() < successChance;
        care.setSuccessChance(successChance);
        care.setSuccessful(success);

        int maxHealth = childrenProperties.getMaxHealth();
        int successHealthDelta = childrenProperties.getSuccessHealthDelta();
        int failHealthDelta = childrenProperties.getFailHealthDelta();

        if (success) {
            children.setHealth(Math.min(maxHealth, children.getHealth() + successHealthDelta));
            children.setStreak(children.getStreak() + 1);
            giveParentsBuff(children);
        } else {
            children.setHealth(Math.max(0, children.getHealth() - failHealthDelta));
            children.setStreak(0);
            giveParentsDebuff(children);
        }

        if (children.getHealth() <= 0) {
            children.setStatus(ChildrenStatus.DEAD);
        }

        care.setResolved(true);
        care.setResolvedAt(Instant.now(clock));
    }

    private double getSuccessChance(ChildrenCare care) {
        int actionsCount = countActions(care);
        if (actionsCount == 0) {
            return 0.0;
        }

        if (actionsCount == 1) {
            return childrenProperties.getSingleSuccessChance();
        }

        if (care.getFirstParentAction() == care.getSecondParentAction()) {
            return childrenProperties.getSameActionSuccessChance();
        }

        return childrenProperties.getBothSuccessChance();
    }

    private int countActions(ChildrenCare care) {
        int count = 0;
        if (care.getFirstParentAction() != null) {
            count++;
        }
        if (care.getSecondParentAction() != null) {
            count++;
        }
        return count;
    }

    // TODO: забалансить
    private void giveParentsBuff(Children children) {
        Player firstPlayer = playerService.findById(children.getFirstPlayer());
        Player secondPlayer = playerService.findById(children.getSecondPlayer());

        firstPlayer.addPendingGrowthModifier(childrenProperties.getGrowthParentBuff());
        secondPlayer.addPendingGrowthModifier(childrenProperties.getGrowthParentBuff());
    }

    private void giveParentsDebuff(Children children) {
        Player firstPlayer = playerService.findById(children.getFirstPlayer());
        Player secondPlayer = playerService.findById(children.getSecondPlayer());

        firstPlayer.addPendingGrowthModifier(childrenProperties.getGrowthParentDebuff());
        secondPlayer.addPendingGrowthModifier(childrenProperties.getGrowthParentDebuff());
    }

    private String buildCareText(Children children, ChildrenCare care, String eventText) {
        String eventBlock = eventText == null || eventText.isBlank()
            ? ""
            : "\n━━━━━━━━━━━━━\n" + eventText;

        Player firstPlayer = playerService.findById(children.getFirstPlayer());
        Player secondPlayer = playerService.findById(children.getSecondPlayer());
        return """
            %s, %s
            
            Ваш ребенок плачет!

            ❤️ Здоровье - %d
            🔥 Серия ухода - %d

            👨 %s — %s
            👩 %s — %s
            
            %s
            """.formatted(
            StringUtil.formatTelegramPlayer(firstPlayer, null),
            StringUtil.formatTelegramPlayer(secondPlayer, null),
            children.getHealth(),
            children.getStreak(),
            firstPlayer.getTelegramDisplayName(),
            formatAction(care.getFirstParentAction()),
            secondPlayer.getTelegramDisplayName(),
            formatAction(care.getSecondParentAction()),
            eventBlock
        ).trim();
    }

    private String buildResolvedEventText(ChildrenCare care) {
        return care.getSuccessful()
            ? "Ребенок перестал плакать. Вы получили +25% усиления следующего роста"
            : "Ребенок продолжил кричать и наблевал вам на одежду. Родители получили -25% к следующему росту";
    }

    private String formatAction(ChildrenAction action) {
        if (action == null) {
            return "ещё не выбрал";
        }

        return switch (action) {
            case EAT -> "покормить";
            case SLEEP -> "уложить спать";
            case PLAY -> "поиграть";
        };
    }

    public record DailyCareDispatch(
        List<DailyCareMessage> messages,
        List<CareMessageEdit> edits
    ) {
    }

    public record DailyCareMessage(
        long careId,
        long scopeId,
        long childrenId,
        LocalDate careDate,
        String text,
        KeyboardSpec keyboard
    ) {
    }

    public record CareMessageEdit(
        long scopeId,
        long messageId,
        String text
    ) {
    }

    public record CareActionResult(
        boolean accepted,
        boolean resolved,
        Long childrenId,
        LocalDate careDate,
        String text
    ) {
        private static CareActionResult success(boolean resolved, long childrenId, LocalDate careDate, String text) {
            return new CareActionResult(true, resolved, childrenId, careDate, text);
        }

        private static CareActionResult error(String text) {
            return new CareActionResult(false, false, null, null, text);
        }
    }
}
