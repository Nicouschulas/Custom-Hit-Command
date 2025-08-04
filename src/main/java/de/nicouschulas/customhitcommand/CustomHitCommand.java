package de.nicouschulas.customhitcommand;

import de.nicouschulas.customhitcommand.utils.SecurityUtils;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import org.bstats.bukkit.Metrics;

public final class CustomHitCommand extends JavaPlugin implements Listener {

    // Core components for message handling and serialization
    private Component chatPrefix;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').hexColors().build();

    // Update checking system variables
    private String latestVersion = null;

    // Particle system configuration variables
    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;

    // Security configuration variables
    private boolean enhancedSecurityLogging;

    @Override
    public void onEnable() {
        getLogger().info("CustomHitCommand starting up with enhanced security features...");

        // Initialize configuration system
        saveDefaultConfig();

        // Load all configuration sections
        loadPrefix();
        loadParticleSettings();
        loadSecuritySettings();

        // Register event listeners for both core functionality and security
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new HitListener(this), this);

        // Register command handlers
        Objects.requireNonNull(this.getCommand("chc")).setExecutor(new ReloadCommand(this));

        // Initialize analytics (bStats)
        int pluginId = 26615;
        new Metrics(this, pluginId);

        // Start background services
        checkForUpdates();
        startSecurityMaintenanceTasks();

        getLogger().info("CustomHitCommand started successfully with security enhancements enabled!");

        // Log security status for administrator awareness
        logSecurityStatus();
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomHitCommand shutting down gracefully...");

        // Perform any necessary cleanup
        SecurityUtils.cleanupOldEntries();

        getLogger().info("CustomHitCommand shutdown completed successfully!");
    }

    /**
     * Loads and validates the chat prefix from configuration.
     * The prefix is used for all plugin messages to maintain consistency.
     */
    public void loadPrefix() {
        String rawPrefix = getConfig().getString("prefix", "&7[&cCHC&7] ");
        this.chatPrefix = legacySerializer.deserialize(rawPrefix);

        if (enhancedSecurityLogging) {
            getLogger().fine("Chat prefix loaded: " + rawPrefix);
        }
    }

    /**
     * Loads particle system configuration with comprehensive validation.
     * This ensures that particle effects work reliably across different server setups.
     */
    public void loadParticleSettings() {
        this.particlesEnabled = getConfig().getBoolean("particles.enabled", false);

        // Validate and load particle type with fallback handling
        String particleTypeName = getConfig().getString("particles.type", "VILLAGER_HAPPY");
        try {
            // Handle legacy particle names that might exist in old configs
            if (particleTypeName.equalsIgnoreCase("VILLAGER_HAPPY")) {
                this.particleType = Particle.HAPPY_VILLAGER;
            } else {
                this.particleType = Particle.valueOf(particleTypeName.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid particle type in configuration: " + particleTypeName);
            getLogger().warning("Falling back to HAPPY_VILLAGER particles");
            this.particleType = Particle.HAPPY_VILLAGER;
        }

        // Load numeric settings with validation for reasonable ranges
        this.particleCount = Math.max(1, Math.min(100, getConfig().getInt("particles.count", 10)));
        this.particleOffsetX = Math.max(0.0, Math.min(5.0, getConfig().getDouble("particles.offset-x", 0.5)));
        this.particleOffsetY = Math.max(0.0, Math.min(5.0, getConfig().getDouble("particles.offset-y", 0.5)));
        this.particleOffsetZ = Math.max(0.0, Math.min(5.0, getConfig().getDouble("particles.offset-z", 0.5)));

        if (enhancedSecurityLogging) {
            getLogger().fine("Particle settings loaded - Enabled: " + particlesEnabled +
                    ", Type: " + particleType + ", Count: " + particleCount);
        }
    }

    /**
     * Loads security-related configuration settings.
     * This method initializes all security features including cooldowns and logging levels.
     */
    public void loadSecuritySettings() {
        // Configure cooldown system
        long cooldownSeconds = Math.max(1, Math.min(60, getConfig().getLong("security.cooldown-seconds", 3)));
        long cooldownMs = cooldownSeconds * 1000;
        SecurityUtils.setCooldownTime(cooldownMs);

        // Configure security logging level
        this.enhancedSecurityLogging = getConfig().getBoolean("security.enhanced-logging", false);

        if (enhancedSecurityLogging) {
            getLogger().info("Enhanced security logging is ENABLED");
        }

        getLogger().info("Security configuration loaded:");
        getLogger().info("- Command cooldown: " + cooldownSeconds + " seconds");
        getLogger().info("- Input validation: ENABLED");
        getLogger().info("- Command template validation: ENABLED");
        getLogger().info("- Memory leak prevention: ENABLED");
    }

    /**
     * Retrieves and formats plugin messages with the configured prefix.
     * This centralizes message handling and ensures consistent formatting.
     *
     * @param messageKey The configuration key for the message
     * @return A formatted Component ready for sending to players
     */
    public Component getFormattedMessage(String messageKey) {
        String message = getConfig().getString("messages." + messageKey,
                "Message not found: " + messageKey + " (please check your configuration)");
        Component messageComponent = legacySerializer.deserialize(message);
        return chatPrefix.append(messageComponent);
    }

    /**
     * Spawns particle effects at the specified location if particles are enabled.
     * This method includes safety checks to prevent errors in edge cases.
     *
     * @param location The location where particles should be spawned
     */
    public void spawnHitParticles(Location location) {
        if (!particlesEnabled) {
            return;
        }

        World world = location.getWorld();
        if (world == null) {
            if (enhancedSecurityLogging) {
                getLogger().warning("Attempted to spawn particles in null world - skipping");
            }
            return;
        }

        try {
            world.spawnParticle(
                    particleType,
                    location,
                    particleCount,
                    particleOffsetX,
                    particleOffsetY,
                    particleOffsetZ
            );

            if (enhancedSecurityLogging) {
                getLogger().fine("Particles spawned at " + location.toString());
            }
        } catch (Exception e) {
            getLogger().warning("Failed to spawn particles: " + e.getMessage());
        }
    }

    /**
     * Reloads the plugin configuration and reinitializes all systems.
     * This method ensures that all components are properly updated when config changes.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();

        // Reload all configuration sections in the correct order
        loadPrefix();
        loadParticleSettings();
        loadSecuritySettings();

        getLogger().info("Configuration reloaded successfully with all security settings updated");
    }

    /**
     * Starts background maintenance tasks for security and system health.
     * This includes memory cleanup and system monitoring tasks.
     */
    private void startSecurityMaintenanceTasks() {
        // Memory cleanup task - runs every 10 minutes (12000 ticks)
        // This prevents the cooldown map from growing indefinitely
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            SecurityUtils.cleanupOldEntries();

            if (enhancedSecurityLogging) {
                getLogger().fine("Security maintenance completed - old cooldown entries cleaned");
            }
        }, 12000L, 12000L);

        // System health monitoring task - runs every 5 minutes (6000 ticks)
        // This logs system status for administrators
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (enhancedSecurityLogging) {
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                getLogger().fine("System health check - Memory usage: " + (usedMemory / 1024 / 1024) + "MB");
            }
        }, 6000L, 6000L);
    }

    /**
     * Logs the current security status for administrator awareness.
     * This helps server administrators understand what protections are active.
     */
    private void logSecurityStatus() {
        getLogger().info("=== SECURITY STATUS REPORT ===");
        getLogger().info("✓ Command injection protection: ACTIVE");
        getLogger().info("✓ Rate limiting system: ACTIVE");
        getLogger().info("✓ Input validation: ACTIVE");
        getLogger().info("✓ Memory leak prevention: ACTIVE");
        getLogger().info("✓ Configuration validation: ACTIVE");
        getLogger().info("===============================");
    }

    /**
     * Checks for plugin updates asynchronously.
     * This method maintains the original update checking functionality
     * while adding better error handling and security considerations.
     */
    private void checkForUpdates() {
        if (!getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        String notifyMethod = getConfig().getString("update-checker.notify-method", "both");

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // Use secure HTTPS connection for update checking
                URL url = new URI("https://api.modrinth.com/v2/project/eXM4AQg2/version?loaders=paper&game_versions=1.21").toURL();

                try (InputStream inputStream = url.openStream();
                     Scanner scanner = new Scanner(inputStream)) {

                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }
                    String json = response.toString();

                    // Parse version information with basic JSON validation
                    if (json.contains("\"version_number\":\"")) {
                        String fetchedLatestVersion = json.split("\"version_number\":\"")[1].split("\"")[0];
                        String currentVersion = getPluginMeta().getVersion();

                        if (!currentVersion.equals(fetchedLatestVersion)) {
                            this.latestVersion = fetchedLatestVersion;

                            if (notifyMethod.equals("console") || notifyMethod.equals("both")) {
                                getLogger().warning("-----------------------------------------------------");
                                getLogger().warning("A new version of Custom Hit Command is available!");
                                getLogger().warning("Current version: " + currentVersion);
                                getLogger().warning("Latest version: " + this.latestVersion);
                                getLogger().warning("Download: https://modrinth.com/plugin/chc/versions");
                                getLogger().warning("-----------------------------------------------------");
                            }
                        } else {
                            if (enhancedSecurityLogging) {
                                getLogger().fine("Update check completed - plugin is up to date");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                getLogger().warning("Update checker could not connect to the update server");
                if (enhancedSecurityLogging) {
                    getLogger().fine("Update check failed with IOException: " + e.getMessage());
                }
            } catch (URISyntaxException e) {
                getLogger().severe("Update checker configuration error - invalid URL syntax");
            } catch (Exception e) {
                getLogger().warning("Unexpected error during update check: " + e.getMessage());
            }
        });
    }

    /**
     * Handles player join events for update notifications.
     * This method respects privacy and security by only notifying authorized players.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        String notifyMethod = getConfig().getString("update-checker.notify-method", "both");

        if (this.latestVersion != null) {
            Player player = event.getPlayer();

            // Only notify players with appropriate permissions
            if ((notifyMethod.equals("player") || notifyMethod.equals("both")) &&
                    player.hasPermission("customhitcommand.update")) {

                // Create user-friendly update notification
                Component textComponent = legacySerializer.deserialize(
                        "&aA new version of Custom Hit Command is available: " + this.latestVersion + " ");

                Component linkComponent = Component.text("Click to download at Modrinth", NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.openUrl("https://modrinth.com/plugin/chc/versions"));

                Component updateMessage = chatPrefix.append(textComponent).append(linkComponent);

                // Delay the message slightly to ensure the player is fully loaded
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    player.sendMessage(updateMessage);
                }, 40L); // 2 second delay
            }
        }
    }
}