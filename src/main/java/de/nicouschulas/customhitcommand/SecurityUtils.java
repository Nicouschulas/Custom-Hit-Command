package de.nicouschulas.customhitcommand;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class SecurityUtils {

    private static final ConcurrentHashMap<UUID, Long> LAST_USAGE = new ConcurrentHashMap<>();
    private static long cooldownTime = 3000;

    private static final long CLEANUP_CUTOFF_TIME = 30 * 60 * 1000;

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "[;&|`]|\\$\\(" +
                    "|\\b(rm|del|format|shutdown|stop)\\b" +
                    "|[\\n\\r\\t]",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean isCommandTemplateSafe(String commandTemplate) {
        if (commandTemplate == null || commandTemplate.isBlank()) {
            return false;
        }
        return !DANGEROUS_PATTERN.matcher(commandTemplate).find();
    }

    public static boolean canPlayerUseCommand(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        Long lastUse = LAST_USAGE.get(playerUUID);

        if (lastUse == null || (currentTime - lastUse) >= cooldownTime) {
            LAST_USAGE.put(playerUUID, currentTime);
            return true;
        }
        return false;
    }

    public static long getRemainingCooldown(UUID playerUUID) {
        Long lastUse = LAST_USAGE.get(playerUUID);
        if (lastUse == null) {
            return 0;
        }

        long remaining = (lastUse + cooldownTime) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public static void setCooldownTime(long milliseconds) {
        cooldownTime = Math.max(100, milliseconds);
    }

    public static void cleanupOldEntries() {
        if (LAST_USAGE.isEmpty()) return;

        long cutoffTime = System.currentTimeMillis() - CLEANUP_CUTOFF_TIME;
        LAST_USAGE.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
}