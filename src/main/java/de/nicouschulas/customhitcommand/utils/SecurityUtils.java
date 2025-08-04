package de.nicouschulas.customhitcommand.utils;

import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Security utilities implementing defense-in-depth principles:
 * 1. Input validation - Never trust user input
 * 2. Rate limiting - Prevent resource abuse
 * 3. Command sanitization - Block injection attempts
 */
public class SecurityUtils {

    // Minecraft usernames: letters, numbers, underscores, 3-16 characters
    private static final Pattern SAFE_PLAYER_NAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    // Thread-safe map for tracking player cooldowns
    private static final ConcurrentHashMap<UUID, Long> lastUsage = new ConcurrentHashMap<>();

    // Default cooldown: 3 seconds (configurable)
    private static long cooldownTime = 3000;

    /**
     * Primary defense against command injection attacks.
     * Validates player names contain only safe characters.
     */
    public static boolean isPlayerNameSafe(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return SAFE_PLAYER_NAME.matcher(playerName).matches();
    }

    /**
     * Fallback sanitization - removes dangerous characters.
     * This is our safety net if validation somehow fails.
     */
    public static String sanitizePlayerName(String playerName) {
        if (playerName == null) {
            return "unknown";
        }

        String sanitized = playerName.replaceAll("[^a-zA-Z0-9_]", "");
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    /**
     * Rate limiting implementation to prevent command spam.
     * Uses UUID for permanent tracking (names can change).
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
     * Helper method to show remaining cooldown time to players.
     * Improves user experience with informative messages.
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
     * Configuration method for adjusting cooldown time.
     * Minimum 100ms to prevent complete spam.
     */
    public static void setCooldownTime(long milliseconds) {
        cooldownTime = Math.max(100, milliseconds);
    }

    /**
     * Validates command templates for obvious injection attempts.
     * Protects against compromised configuration files.
     */
    public static boolean isCommandTemplateSafe(String commandTemplate) {
        if (commandTemplate == null || commandTemplate.trim().isEmpty()) {
            return false;
        }

        // Block common command injection patterns
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

    /**
     * Memory management - prevents cooldown map from growing infinitely.
     * Should be called periodically (every 10-15 minutes).
     */
    public static void cleanupOldEntries() {
        long cutoffTime = System.currentTimeMillis() - (30 * 60 * 1000); // 30 minutes
        lastUsage.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
}