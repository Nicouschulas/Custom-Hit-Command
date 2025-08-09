package de.nicouschulas.customhitcommand;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class SecurityUtils {

    private static final ConcurrentHashMap<UUID, Long> lastUsage = new ConcurrentHashMap<>();
    private static long cooldownTime = 100;

    public static boolean isCommandTemplateSafe(String commandTemplate) {
        if (commandTemplate == null || commandTemplate.trim().isEmpty()) {
            return false;
        }

        String[] dangerousPatterns = {
                ";", "&&", "||", "|", "`", "$(",
                "rm ", "del ", "format ", "shutdown", "stop"
        };

        String lowerCommand = commandTemplate.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerCommand.contains(pattern)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canPlayerUseCommand(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        Long lastUse = lastUsage.get(playerUUID);

        if (lastUse == null || (currentTime - lastUse) >= cooldownTime) {
            lastUsage.put(playerUUID, currentTime);
            return true;
        }
        return false;
    }

    public static long getRemainingCooldown(UUID playerUUID) {
        Long lastUse = lastUsage.get(playerUUID);
        if (lastUse == null) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = cooldownTime - elapsed;
        return Math.max(0, remaining);
    }

    public static void setCooldownTime(long milliseconds) {
        cooldownTime = Math.max(100, milliseconds);
    }

    public static void cleanupOldEntries() {
        long cutoffTime = System.currentTimeMillis() - (30 * 60 * 1000);
        lastUsage.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
}