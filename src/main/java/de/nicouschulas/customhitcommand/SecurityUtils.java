package de.nicouschulas.customhitcommand;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class SecurityUtils {

    // Thread-safe map for tracking player cooldowns
    private static final ConcurrentHashMap<UUID, Long> lastUsage = new ConcurrentHashMap<>();

    // Default cooldown time in milliseconds (configurable)
    private static long cooldownTime = 100;

    /**
     * Rate limiting implementation to prevent command spam.
     * Checks if a player can use a command based on a cooldown.
     *
     * @param playerUUID The UUID of the player.
     * @return true if the player can use the command, false otherwise.
     */
    public static boolean canPlayerUseCommand(UUID playerUUID) {
        long currentTime = System.currentTimeMillis();
        Long lastUse = lastUsage.get(playerUUID);

        if (lastUse == null || (currentTime - lastUse) >= cooldownTime) {
            lastUsage.put(playerUUID, currentTime);
            return true;
        }
        return false;
    }

    /**
     * Helper method to calculate remaining cooldown time for a player.
     *
     * @param playerUUID The UUID of the player.
     * @return The remaining cooldown time in milliseconds.
     */
    public static long getRemainingCooldown(UUID playerUUID) {
        Long lastUse = lastUsage.get(playerUUID);
        if (lastUse == null) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = cooldownTime - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Sets the cooldown time for rate limiting.
     * Minimum 100ms to prevent instant spam.
     *
     * @param milliseconds The new cooldown time in milliseconds.
     */
    public static void setCooldownTime(long milliseconds) {
        cooldownTime = Math.max(100, milliseconds);
    }

    /**
     * Memory management to prevent the cooldown map from growing indefinitely.
     * Removes entries that haven't been used in a long time.
     */
    public static void cleanupOldEntries() {
        long cutoffTime = System.currentTimeMillis() - (30 * 60 * 1000); // 30 minutes
        lastUsage.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
}