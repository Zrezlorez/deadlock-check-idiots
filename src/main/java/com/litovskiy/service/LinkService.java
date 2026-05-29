package com.litovskiy.service;

import com.litovskiy.entity.LinkCode;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.repository.LinkCodeRepository;
import com.litovskiy.service.data.ActivityStatService;
import com.litovskiy.service.data.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final PlayerService playerService;
    private final LinkCodeRepository linkCodeRepository;
    private final PlayerAccountService playerAccountService;
    private final ActivityStatService activityStatService;

    private final SecureRandom random = new SecureRandom();
    private final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public String createCode(Platform platform, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        int minutes = 10;
        LocalDateTime now = LocalDateTime.now();

        linkCodeRepository.deleteLinkCodesByExpiresAtBefore(now);
        LinkCode existingCode = linkCodeRepository.findByPlayerId(player.getId());
        if (existingCode != null) {
            return formatLinkCodeMessage(existingCode, true);
        }

        LinkCode linkCode = new LinkCode(
            nextCode(),
            player.getId(),
            platform,
            now.plusMinutes(minutes)
        );
        linkCodeRepository.save(linkCode);

        return formatLinkCodeMessage(linkCode, false);
    }

    public String linkProfile(Platform platform, long profileId, String rawCode) {
        String code = normalizeCode(rawCode);
        LocalDateTime now = LocalDateTime.now();

        linkCodeRepository.deleteLinkCodesByExpiresAtBefore(now);
        LinkCode linkCode = linkCodeRepository.findByCode(code);
        if (linkCode == null) {
            return "Код привязки не найден или уже истек.";
        }

        if (linkCode.getSourcePlatform() == platform) {
            return "Этот код нужно вводить в другом боте.";
        }

        Player currentPlayer = playerService.findById(linkCode.getPlayerId());
        Player targetPlayer = playerAccountService.resolveOrCreate(platform, profileId);
        if (currentPlayer == null) {
            linkCodeRepository.delete(linkCode);
            return "Аккаунт для привязки не найден. Сгенерируйте новый код.";
        }

        if (currentPlayer.getId().equals(targetPlayer.getId())) {
            linkCodeRepository.delete(linkCode);
            return "Этот профиль уже привязан к тому же аккаунту.";
        }

        String conflictMessage = getConflictMessage(currentPlayer, targetPlayer, platform, profileId);
        if (conflictMessage != null) {
            return conflictMessage;
        }

        currentPlayer.setDiscordUserId(targetPlayer.getDiscordUserId());

        playerService.delete(targetPlayer);
        activityStatService.deleteByPlayerId(targetPlayer.getId());
        linkCodeRepository.delete(linkCode);
        return "Профили объединены. Теперь Telegram и Discord используют один аккаунт.";
    }

    private String getConflictMessage(Player currentPlayer, Player targetPlayer, Platform currentPlatform, long profileId) {
        Long targetProfileId = currentPlatform.getProfileId(targetPlayer);
        if (targetProfileId != null && !targetProfileId.equals(profileId)) {
            return "К этому аккаунту уже привязан другой " + currentPlatform.displayName() + "-профиль.";
        }

        if (hasConflict(targetPlayer.getTelegramChatId(), currentPlayer.getTelegramChatId())
            || hasConflict(targetPlayer.getDiscordUserId(), currentPlayer.getDiscordUserId())) {
            return "Оба аккаунта уже привязаны к разным профилям. Такое объединение нужно разруливать вручную. Обратитесь к администратору - @zrezlorez";
        }

        return null;
    }

    private boolean hasConflict(Long targetValue, Long sourceValue) {
        return targetValue != null && sourceValue != null && !targetValue.equals(sourceValue);
    }

    private String normalizeCode(String rawCode) {
        return rawCode == null ? "" : rawCode.trim().toUpperCase();
    }

    private String nextCode() {
        int codeLength = 6;
        StringBuilder builder = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String formatLinkCodeMessage(LinkCode linkCode, boolean existing) {
        String prefix = existing ? "У вас уже есть активный код привязки." : "Код привязки создан.";
        return String.format(
            "%s%nКод привязки: %s%nОтправьте его в другом боте командой /link %s%nКод действует до %s " +
                "\n Обратите внимание! Аккаунт, который находится на платформе, где вы вводите код, будет удален",
            prefix,
            linkCode.getCode(),
            linkCode.getCode(),
            linkCode.getExpiresAt().format(formatter)
        );
    }
}
