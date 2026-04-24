package com.litovskiy.service;

import com.litovskiy.dao.LinkCodeDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.LinkCode;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LinkService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";

    private final PlayerDao playerDao;
    private final LinkCodeDao linkCodeDao;
    private final PlayerAccountService playerAccountService;
    private final GameConfigService gameConfigService;
    private final SecureRandom random;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public LinkService(PlayerDao playerDao,
                       LinkCodeDao linkCodeDao,
                       PlayerAccountService playerAccountService,
                       GameConfigService gameConfigService) {
        this(playerDao, linkCodeDao, playerAccountService, gameConfigService, new SecureRandom());
    }

    public LinkService(PlayerDao playerDao,
                       LinkCodeDao linkCodeDao,
                       PlayerAccountService playerAccountService,
                       GameConfigService gameConfigService,
                       SecureRandom random) {
        this.playerDao = playerDao;
        this.linkCodeDao = linkCodeDao;
        this.playerAccountService = playerAccountService;
        this.gameConfigService = gameConfigService;
        this.random = random;
    }

    public String createCode(Platform platform, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        LocalDateTime now = LocalDateTime.now();

        linkCodeDao.deleteExpired(now);
        LinkCode existingCode = linkCodeDao.findByPlayerChatId(player.getChatId());
        if (existingCode != null) {
            return formatLinkCodeMessage(existingCode, true);
        }

        LinkCode linkCode = new LinkCode(
            nextCode(),
            player.getChatId(),
            platform,
            now.plusMinutes(gameConfigService.getInt(GameSetting.LINK_CODE_LIFETIME_MINUTES))
        );
        linkCodeDao.save(linkCode);

        return formatLinkCodeMessage(linkCode, false);
    }

    public String linkProfile(Platform platform, long profileId, String rawCode) {
        String code = normalizeCode(rawCode);
        LocalDateTime now = LocalDateTime.now();

        linkCodeDao.deleteExpired(now);
        LinkCode linkCode = linkCodeDao.findByCode(code);
        if (linkCode == null) {
            return "Код привязки не найден или уже истек.";
        }

        if (linkCode.getSourcePlatform() == platform) {
            return "Этот код нужно вводить в другом боте.";
        }

        Player currentPlayer = playerAccountService.resolveOrCreate(platform, profileId);
        Player targetPlayer = playerDao.find(linkCode.getPlayerChatId());
        if (targetPlayer == null) {
            linkCodeDao.delete(linkCode);
            return "Аккаунт для привязки не найден. Сгенерируйте новый код.";
        }

        if (currentPlayer.getChatId().equals(targetPlayer.getChatId())) {
            linkCodeDao.delete(linkCode);
            return "Этот профиль уже привязан к тому же аккаунту.";
        }

        String conflictMessage = getConflictMessage(currentPlayer, targetPlayer, platform, profileId);
        if (conflictMessage != null) {
            return conflictMessage;
        }

        mergePlayers(targetPlayer, currentPlayer);
        playerDao.mergeAndDeleteSource(targetPlayer, currentPlayer);

        linkCodeDao.delete(linkCode);
        return "Профили объединены. Теперь Telegram и Discord используют один аккаунт.";
    }

    private void mergePlayers(Player targetPlayer, Player sourcePlayer) {
        targetPlayer.setSize(Math.max(targetPlayer.getSize(), sourcePlayer.getSize()));

        LocalDateTime targetGrowTime = targetPlayer.getLastGrowTime();
        LocalDateTime sourceGrowTime = sourcePlayer.getLastGrowTime();
        if (sourceGrowTime != null && (targetGrowTime == null || sourceGrowTime.isAfter(targetGrowTime))) {
            targetPlayer.setLastGrowTime(sourceGrowTime);
        }

        if (targetPlayer.getTelegramChatId() == null) {
            targetPlayer.setTelegramChatId(sourcePlayer.getTelegramChatId());
        }
        if (targetPlayer.getDiscordUserId() == null) {
            targetPlayer.setDiscordUserId(sourcePlayer.getDiscordUserId());
        }
    }

    private String getConflictMessage(Player currentPlayer, Player targetPlayer, Platform currentPlatform, long profileId) {
        Long targetProfileId = currentPlatform.getProfileId(targetPlayer);
        if (targetProfileId != null && !targetProfileId.equals(profileId)) {
            return "К этому аккаунту уже привязан другой " + currentPlatform.displayName() + "-профиль.";
        }

        if (hasConflict(targetPlayer.getTelegramChatId(), currentPlayer.getTelegramChatId())
            || hasConflict(targetPlayer.getDiscordUserId(), currentPlayer.getDiscordUserId())) {
            return "Оба аккаунта уже привязаны к разным профилям. Такое объединение нужно разруливать вручную.";
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
        int codeLength = gameConfigService.getInt(GameSetting.LINK_CODE_LENGTH);
        StringBuilder builder = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String formatLinkCodeMessage(LinkCode linkCode, boolean existing) {
        String prefix = existing ? "У вас уже есть активный код привязки." : "Код привязки создан.";
        return String.format(
            "%s%nКод привязки: %s%nОтправьте его в другом боте командой /link %s%nКод действует до %s",
            prefix,
            linkCode.getCode(),
            linkCode.getCode(),
            linkCode.getExpiresAt().format(formatter)
        );
    }
}
