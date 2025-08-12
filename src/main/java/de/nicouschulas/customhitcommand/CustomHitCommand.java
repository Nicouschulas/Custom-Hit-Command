package de.nicouschulas.customhitcommand;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.logging.Level;

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

    private Component chatPrefix;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').hexColors().build();

    private String latestVersion = null;
    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;

    private boolean enhancedSecurityLogging;

    public boolean isEnhancedSecurityLogging() {
        return this.enhancedSecurityLogging;
    }

    @Override
    public void onEnable() {
        getLogger().info("CustomHitCommand is starting...");

        saveDefaultConfig();
        reloadConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new HitListener(this), this);

        Objects.requireNonNull(this.getCommand("chc")).setExecutor(new ReloadCommand(this));

        int pluginId = 26615;
        new Metrics(this, pluginId);

        checkForUpdates();

        startSecurityMaintenanceTasks();

        getLogger().info("CustomHitCommand started successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomHitCommand shutdown successfully!");
    }

    public void loadPrefix() {
        String rawPrefix = getConfig().getString("prefix", "&7[&cCHC&7] ");
        this.chatPrefix = legacySerializer.deserialize(rawPrefix);

        if (enhancedSecurityLogging) {
            getLogger().fine("Chat prefix loaded: " + rawPrefix);
        }
    }

    public void loadParticleSettings() {
        this.particlesEnabled = getConfig().getBoolean("particles.enabled", false);
        String particleTypeName = getConfig().getString("particles.type", "VILLAGER_HAPPY");
        try {
            if (particleTypeName.equalsIgnoreCase("VILLAGER_HAPPY")) {
                this.particleType = Particle.HAPPY_VILLAGER;
            } else {
                this.particleType = Particle.valueOf(particleTypeName.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid particle type in config.yml: " + particleTypeName);
            getLogger().warning("Falling back to HAPPY_VILLAGER.");
            this.particleType = Particle.HAPPY_VILLAGER;
        }
        this.particleCount = Math.max(1, Math.min(100, getConfig().getInt("particles.count", 10)));
        this.particleOffsetX = Math.max(0.0, Math.min(5.0, getConfig().getDouble("particles.offset-x", 0.5)));
        this.particleOffsetY = Math.max(0.0, Math.min(5.0, getConfig().getDouble("particles.offset-y", 0.5)));
        this.particleOffsetZ = Math.max(0.0, Math.min(5.0, getConfig().getDouble("particles.offset-z", 0.5)));
    }

    public void loadSecuritySettings() {
        long cooldownMs = getConfig().getLong("security.cooldown-milliseconds", 3000);
        SecurityUtils.setCooldownTime(cooldownMs);
        getLogger().info("Security configuration loaded: Command cooldown is " + (cooldownMs / 1000.0) + " seconds.");

        this.enhancedSecurityLogging = getConfig().getBoolean("security.enhanced-logging", false);
        if (this.enhancedSecurityLogging) {
            getLogger().info("Enhanced security logging is ENABLED.");
            this.getLogger().setLevel(Level.FINE);
        } else {
            this.getLogger().setLevel(Level.INFO);
        }
    }

    public Component getFormattedMessage(String messageKey) {
        String message = getConfig().getString("messages." + messageKey, "Message not found: " + messageKey);
        Component messageComponent = legacySerializer.deserialize(message);
        return chatPrefix.append(messageComponent);
    }

    public void spawnHitParticles(Location location) {
        if (!particlesEnabled) {
            return;
        }

        World world = location.getWorld();
        if (world == null) {
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
        } catch (Exception e) {
            getLogger().warning("Failed to spawn particles: " + e.getMessage());
        }

        if (enhancedSecurityLogging) {
            getLogger().fine("Particles spawned at " + location);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadSecuritySettings();
        loadPrefix();
        loadParticleSettings();
    }

    private void checkForUpdates() {
        if (!getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        String notifyMethod = getConfig().getString("update-checker.notify-method", "both");

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URI("https://api.modrinth.com/v2/project/eXM4AQg2/version?loaders=paper&game_versions=1.21").toURL();

                try (InputStream inputStream = url.openStream(); Scanner scanner = new Scanner(inputStream)) {
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }
                    String json = response.toString();

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
                                getLogger().warning("Download it here: https://modrinth.com/plugin/chc/versions");
                                getLogger().warning("-----------------------------------------------------");
                            }
                        }
                    }
                }
            } catch (IOException | URISyntaxException e) {
                getLogger().warning("Update checker failed to connect to the server!");
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        String notifyMethod = getConfig().getString("update-checker.notify-method", "both");

        if (this.latestVersion != null) {
            Player player = event.getPlayer();
            if ((notifyMethod.equals("player") || notifyMethod.equals("both")) && player.hasPermission("customhitcommand.update")) {

                Component textComponent = legacySerializer.deserialize("&aA new version of Custom Hit Command is available: " + this.latestVersion + " ");

                Component linkComponent = Component.text("Click to download it at Modrinth", NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.openUrl("https://modrinth.com/plugin/chc/versions"));

                Component updateMessage = chatPrefix.append(textComponent).append(linkComponent);

                player.sendMessage(updateMessage);
            }
        }
    }

    private void startSecurityMaintenanceTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            SecurityUtils.cleanupOldEntries();
            if (enhancedSecurityLogging) {
                getLogger().fine("Security maintenance: old cooldown entries cleaned.");
            }
        }, 12000L, 12000L);
    }
}