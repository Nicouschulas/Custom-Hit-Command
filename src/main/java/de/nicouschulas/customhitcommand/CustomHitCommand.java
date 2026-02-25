package de.nicouschulas.customhitcommand;

import org.bukkit.*;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import org.bstats.bukkit.Metrics;

public final class CustomHitCommand extends JavaPlugin implements Listener {

    private Component chatPrefix;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').hexColors().build();
    public static NamespacedKey CUSTOM_ITEM_KEY;

    private String latestVersion = null;
    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;

    private boolean checkMaterialGroup;
    private boolean ignoreCancelledHits;
    private Material hitItemMaterial;
    private String commandToExecute;
    private String commandExecutor;

    private boolean enhancedSecurityLogging;

    public boolean isEnhancedSecurityLogging() {
        return this.enhancedSecurityLogging;
    }

    @Override
    public void onEnable() {
        getLogger().info("CustomHitCommand is starting...");

        saveDefaultConfig();
        reloadConfig();

        CUSTOM_ITEM_KEY = new NamespacedKey(this, "custom-hit-item");

        getServer().getPluginManager().registerEvents(new HitListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        CommandHandler commandHandler = new CommandHandler(this);

        Objects.requireNonNull(this.getCommand("chc")).setExecutor(commandHandler);
        Objects.requireNonNull(this.getCommand("chc")).setTabCompleter(commandHandler);

        int serviceId = 26615;
        new Metrics(this, serviceId);

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
        this.enhancedSecurityLogging = getConfig().getBoolean("security.enhanced-logging", false);
        this.getLogger().setLevel(this.enhancedSecurityLogging ? Level.FINE : Level.INFO);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        this.checkMaterialGroup = getConfig().getBoolean("check-material-group", true);
        this.ignoreCancelledHits = getConfig().getBoolean("ignore-cancelled-hits", true);
        this.commandToExecute = getConfig().getString("command-to-execute", "duel %hitted_player%");
        this.commandExecutor = getConfig().getString("command-executor", "player");

        String matName = getConfig().getString("hit-item", "IRON_SWORD");
        try {
            this.hitItemMaterial = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid material name in config.yml! Defaulting to IRON_SWORD.");
            this.hitItemMaterial = Material.IRON_SWORD;
        }

        loadSecuritySettings();
        loadPrefix();
        loadParticleSettings();

        CommandHandler commandHandler = new CommandHandler(this);
        Objects.requireNonNull(this.getCommand("chc")).setExecutor(commandHandler);
        Objects.requireNonNull(this.getCommand("chc")).setTabCompleter(commandHandler);
    }

    public Material getHitItemMaterial() { return hitItemMaterial; }
    public String getCommandToExecute() { return commandToExecute; }
    public String getCommandExecutor() { return commandExecutor; }
    public boolean shouldCheckMaterialGroup() { return checkMaterialGroup; }
    public boolean isIgnoreCancelledHits() { return ignoreCancelledHits; }

    public Component getFormattedMessage(String messageKey) {
        String message = getConfig().getString("messages." + messageKey, "Message not found: " + messageKey);
        return chatPrefix.append(legacySerializer.deserialize(message));
    }

    public void spawnHitParticles(Location location) {
        if (!particlesEnabled || location.getWorld() == null) return;
        try {
            location.getWorld().spawnParticle(particleType, location, particleCount, particleOffsetX, particleOffsetY, particleOffsetZ);
        } catch (Exception e) {
            getLogger().warning("Failed to spawn particles: " + e.getMessage());
        }
    }

    private void checkForUpdates() {
        if (!getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        final String notifyMethod = getConfig().getString("update-checker.notify-method", "both");
        final String currentVersion = getPluginMeta().getVersion();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URI("https://api.modrinth.com/v2/project/eXM4AQg2/version").toURL();
                try (InputStream inputStream = url.openStream(); Scanner scanner = new Scanner(inputStream)) {
                    String json = scanner.useDelimiter("\\A").next();

                    if (json.contains("\"version_number\":\"")) {
                        String fetchedLatestVersion = json.split("\"version_number\":\"")[1].split("\"")[0];

                        if (!currentVersion.equals(fetchedLatestVersion)) {
                            this.latestVersion = fetchedLatestVersion;

                            if (notifyMethod.equalsIgnoreCase("console") || notifyMethod.equalsIgnoreCase("both")) {
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
                getLogger().log(Level.FINER, "Update checker failed to connect to the server!", e);
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
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, SecurityUtils::cleanupOldEntries, 12000L, 12000L);
    }
}