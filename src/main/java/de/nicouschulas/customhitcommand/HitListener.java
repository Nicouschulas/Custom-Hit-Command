package de.nicouschulas.customhitcommand;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

public record HitListener(CustomHitCommand plugin) implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof Player hittedPlayer)) {
            return;
        }

        if (!SecurityUtils.canPlayerUseCommand(attacker.getUniqueId())) {
            long remainingMs = SecurityUtils.getRemainingCooldown(attacker.getUniqueId());
            long remainingSeconds = (remainingMs / 1000) + 1;

            Component cooldownMessage = plugin.getFormattedMessage("cooldown-message")
                    .replaceText(builder -> builder.match("%seconds%").replacement(String.valueOf(remainingSeconds)));

            attacker.sendMessage(cooldownMessage);
            return;
        }

        ItemStack handItem = attacker.getInventory().getItemInMainHand();

        FileConfiguration config = plugin.getConfig();

        String configuredMaterialName = config.getString("hit-item", "IRON_SWORD");
        Material requiredMaterial;

        try {
            requiredMaterial = Material.valueOf(configuredMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("=== CONFIGURATION ERROR ===");
            plugin.getLogger().severe("Invalid material specified in config.yml: " + configuredMaterialName);
            plugin.getLogger().severe("Using IRON_SWORD as a temporary fallback.");
            plugin.getLogger().severe("=========================");

            requiredMaterial = Material.IRON_SWORD;
        }

        if (handItem.getType() == requiredMaterial) {
            String command = config.getString("command-to-execute", "duel %hitted_player%");

            if (!SecurityUtils.isCommandTemplateSafe(command)) {
                plugin.getLogger().severe("=== SECURITY ALERT ===");
                plugin.getLogger().severe("Unsafe command template detected: " + command);
                plugin.getLogger().severe("Command execution blocked for security reasons.");
                plugin.getLogger().severe("===================");
                return;
            }

            String finalCommand = command.replace("%hitted_player%", hittedPlayer.getName());
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

            try {
                Bukkit.dispatchCommand(console, finalCommand);
                plugin.spawnHitParticles(hittedPlayer.getLocation());

                Component logMessage = plugin.getFormattedMessage("command-executed-log")
                        .replaceText(builder -> builder.match("%attacker%").replacement(attacker.getName()))
                        .replaceText(builder -> builder.match("%hitted_player%").replacement(hittedPlayer.getName()))
                        .replaceText(builder -> builder.match("%item%").replacement(handItem.getType().name()))
                        .replaceText(builder -> builder.match("%command%").replacement(finalCommand));

                plugin.getLogger().info(LegacyComponentSerializer.legacySection().serialize(logMessage));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to execute command: " + finalCommand);
                plugin.getLogger().severe("Error details: " + e.getMessage());

                Component errorMessage = plugin.getFormattedMessage("command-execution-failed");
                attacker.sendMessage(errorMessage);
            }
        }
    }
}