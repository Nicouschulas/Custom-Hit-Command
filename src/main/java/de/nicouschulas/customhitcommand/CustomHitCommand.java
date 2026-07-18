package de.nicouschulas.customhitcommand;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.jspecify.annotations.NullMarked;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public final class CustomHitCommand extends JavaPlugin implements Listener {

    private Component chatPrefix;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

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
    private List<String> externalNbtTags;

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

        io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager<org.bukkit.plugin.Plugin> manager = this.getLifecycleManager();

        manager.registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS, event -> {
            final io.papermc.paper.command.brigadier.Commands commands = event.registrar();

            commands.register(
                    "chc",
                    "Manages the Custom Hit Command commands (reload, sethititem).",
                    java.util.List.of("customhitcommand"),
                    new BasicCommand() {
                        @Override
                        @NullMarked
                        public void execute(CommandSourceStack source, String[] args) {
                            org.bukkit.command.PluginCommand command = CustomHitCommand.this.getCommand("chc");
                            commandHandler.onCommand(source.getSender(), Objects.requireNonNullElseGet(command, () -> new Command("chc") {
                                @Override
                                public boolean execute(CommandSender sender, String commandLabel, String[] commandArgs) {
                                    return false;
                                }
                            }), "chc", args);
                        }

                        @Override
                        @NullMarked
                        public Collection<String> suggest(CommandSourceStack source, String[] args) {
                            org.bukkit.command.PluginCommand command = CustomHitCommand.this.getCommand("chc");

                            String[] finalArgs = args.length == 0 ? new String[]{""} : args;

                            return commandHandler.onTabComplete(
                                    source.getSender(),
                                    Objects.requireNonNullElseGet(command, () -> new Command("chc") {
                                        @Override
                                        public boolean execute(CommandSender sender, String commandLabel, String[] commandArgs) {
                                            return false;
                                        }
                                    }),
                                    "chc",
                                    finalArgs
                            );
                        }

                        @Override
                        public String permission() {
                            return "customhitcommand.admin";
                        }
                    }
            );
        });

        int serviceId = 26615;
        new Metrics(this, serviceId);

        checkForUpdates();
        startSecurityMaintenanceTasks();

        getLogger().info("CustomHitCommand started successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomHitCommand is shutting down...");
        getLogger().info("CustomHitCommand shutdown successfully!");
    }

    public void loadPrefix() {
        String rawPrefix = getConfig().getString("prefix", "<gray>[<red>CHC<gray>] ");
        this.chatPrefix = parse(rawPrefix);
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
        this.particleCount = Math.clamp(getConfig().getInt("particles.count", 10), 1, 100);
        this.particleOffsetX = Math.clamp(getConfig().getDouble("particles.offset-x", 0.5), 0.0, 5.0);
        this.particleOffsetY = Math.clamp(getConfig().getDouble("particles.offset-y", 0.5), 0.0, 5.0);
        this.particleOffsetZ = Math.clamp(getConfig().getDouble("particles.offset-z", 0.5), 0.0, 5.0);
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
        this.externalNbtTags = getConfig().getStringList("external-nbt-tags");

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
    }

    public Material getHitItemMaterial() { return hitItemMaterial; }
    public String getCommandToExecute() { return commandToExecute; }
    public String getCommandExecutor() { return commandExecutor; }
    public boolean shouldCheckMaterialGroup() { return checkMaterialGroup; }
    public boolean isIgnoreCancelledHits() { return ignoreCancelledHits; }
    public List<String> getExternalNbtTags() { return externalNbtTags; }

    public Component parse(String input) {
        if (input == null) return Component.empty();

        String prepared = input
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&r", "<reset>")
                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>");

        prepared = HEX_PATTERN.matcher(prepared).replaceAll("<#$1>");

        return miniMessage.deserialize(prepared);
    }

    public Component getFormattedMessage(String messageKey) {
        String message = getConfig().getString("messages." + messageKey, "Message not found: " + messageKey);
        return chatPrefix.append(parse(message));
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
            } catch (Exception e) {
                getLogger().log(Level.FINER, "Update checker failed to process the response!", e);
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

                Component textComponent = parse("&aA new version of Custom Hit Command is available: " + this.latestVersion + " ");

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