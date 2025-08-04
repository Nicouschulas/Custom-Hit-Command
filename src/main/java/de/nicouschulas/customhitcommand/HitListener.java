package de.nicouschulas.customhitcommand;

import de.nicouschulas.customhitcommand.utils.SecurityUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.Bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HitListener implements Listener {

    private final CustomHitCommand plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').build();

    public HitListener(CustomHitCommand plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Performance optimization: check cheapest conditions first
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof Player hittedPlayer)) {
            return;
        }

        // SECURITY LAYER 1: Rate limiting check
        if (!SecurityUtils.canPlayerUseCommand(attacker.getUniqueId())) {
            // User-friendly cooldown message
            long remainingMs = SecurityUtils.getRemainingCooldown(attacker.getUniqueId());
            long remainingSeconds = (remainingMs / 1000) + 1;

            Component cooldownMessage = plugin.getFormattedMessage("cooldown-message")
                    .replaceText(builder -> builder.match("%seconds%").replacement(String.valueOf(remainingSeconds)));

            attacker.sendMessage(cooldownMessage);
            return;
        }

        // Item validation with improved error handling
        ItemStack handItem = attacker.getInventory().getItemInMainHand();
        FileConfiguration config = plugin.getConfig();

        String configuredMaterialName = config.getString("hit-item", "IRON_SWORD");
        Material requiredMaterial;

        try {
            requiredMaterial = Material.valueOf(configuredMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Enhanced error logging for administrators
            plugin.getLogger().severe("=== CONFIGURATION ERROR ===");
            plugin.getLogger().severe("Invalid material specified: " + configuredMaterialName);
            plugin.getLogger().severe("Please check your config.yml file!");
            plugin.getLogger().severe("Using IRON_SWORD as temporary fallback.");
            plugin.getLogger().severe("=========================");

            requiredMaterial = Material.IRON_SWORD;
        }

        // Early return if wrong item
        if (handItem.getType() != requiredMaterial) {
            return;
        }

        // Get command template
        String commandTemplate = config.getString("command-to-execute", "duel %hitted_player%");

        // SECURITY LAYER 2: Command template validation
        if (!SecurityUtils.isCommandTemplateSafe(commandTemplate)) {
            plugin.getLogger().severe("=== SECURITY ALERT ===");
            plugin.getLogger().severe("Unsafe command template detected: " + commandTemplate);
            plugin.getLogger().severe("This could indicate a compromised configuration file!");
            plugin.getLogger().severe("Command execution blocked for security.");
            plugin.getLogger().severe("===================");
            return;
        }

        // SECURITY LAYER 3: Player name validation and sanitization
        String victimName = hittedPlayer.getName();
        if (!SecurityUtils.isPlayerNameSafe(victimName)) {
            plugin.getLogger().warning("=== POTENTIAL ATTACK DETECTED ===");
            plugin.getLogger().warning("Unsafe player name: " + victimName);
            plugin.getLogger().warning("This could be a command injection attempt!");

            // Apply sanitization as fallback
            String originalName = victimName;
            victimName = SecurityUtils.sanitizePlayerName(victimName);

            plugin.getLogger().warning("Original name: " + originalName);
            plugin.getLogger().warning("Sanitized to: " + victimName);
            plugin.getLogger().warning("===============================");
        }

        // Safe command construction and execution
        String finalCommand = commandTemplate.replace("%hitted_player%", victimName);

        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        try {
            Bukkit.dispatchCommand(console, finalCommand);

            // Spawn visual effects
            plugin.spawnHitParticles(hittedPlayer.getLocation());

            // Security audit logging
            plugin.getLogger().info(String.format(
                    "[COMMAND EXECUTED] %s -> %s | Item: %s | Command: %s",
                    attacker.getName(),
                    victimName,
                    handItem.getType().name(),
                    finalCommand
            ));

        } catch (Exception e) {
            // Handle command execution errors gracefully
            plugin.getLogger().severe("Failed to execute command: " + finalCommand);
            plugin.getLogger().severe("Error: " + e.getMessage());

            // Optionally notify the attacker that something went wrong
            Component errorMessage = plugin.getFormattedMessage("command-execution-failed");
            attacker.sendMessage(errorMessage);
        }
    }
}