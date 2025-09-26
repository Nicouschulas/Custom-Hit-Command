package de.nicouschulas.customhitcommand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public record HitListener(CustomHitCommand plugin) implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player hittedPlayer)) {
            return;
        }

        ItemStack handItem = attacker.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta itemMeta = handItem.getItemMeta();
        if (itemMeta == null) {
            return;
        }

        boolean isMarkedItem = itemMeta.getPersistentDataContainer().has(CustomHitCommand.CUSTOM_ITEM_KEY, PersistentDataType.STRING);

        boolean isConfigItem = false;
        try {
            Material requiredMaterial = Material.valueOf(plugin.getConfig().getString("hit-item", "IRON_SWORD").toUpperCase());
            isConfigItem = handItem.getType() == requiredMaterial;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material name in config.yml! Defaulting to IRON_SWORD.");
        }

        if (isMarkedItem || isConfigItem) {
            if (!SecurityUtils.canPlayerUseCommand(attacker.getUniqueId())) {
                long remainingMs = SecurityUtils.getRemainingCooldown(attacker.getUniqueId());
                long remainingSeconds = (remainingMs / 1000) + 1;

                Component cooldownMessage = plugin.getFormattedMessage("cooldown-message")
                        .replaceText(builder -> builder.match("%seconds%").replacement(String.valueOf(remainingSeconds)));

                attacker.sendMessage(cooldownMessage);
                return;
            }

            FileConfiguration config = plugin.getConfig();
            String command = config.getString("command-to-execute", "duel %hitted_player%");

            if (!SecurityUtils.isCommandTemplateSafe(command)) {
                plugin.getLogger().severe("=== SECURITY ALERT ===");
                plugin.getLogger().severe("Unsafe command template detected: " + command);
                plugin.getLogger().severe("Command execution blocked for security reasons.");
                plugin.getLogger().severe("===================");
                return;
            }

            String finalCommand = command.replace("%hitted_player%", hittedPlayer.getName());
            String commandExecutor = config.getString("command-executor", "console");

            try {
                if (commandExecutor.equalsIgnoreCase("player")) {
                    attacker.chat("/" + finalCommand);
                } else {
                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    Bukkit.dispatchCommand(console, finalCommand);
                }

                plugin.spawnHitParticles(hittedPlayer.getLocation());

                if (plugin.isEnhancedSecurityLogging()) {
                    Component logMessage = plugin.getFormattedMessage("command-executed-log")
                            .replaceText(builder -> builder.match("%attacker%").replacement(attacker.getName()))
                            .replaceText(builder -> builder.match("%hitted_player%").replacement(hittedPlayer.getName()))
                            .replaceText(builder -> builder.match("%item%").replacement(handItem.getType().name()))
                            .replaceText(builder -> builder.match("%command%").replacement(finalCommand));

                    plugin.getLogger().info(LegacyComponentSerializer.legacySection().serialize(logMessage));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to execute command: " + finalCommand);
                plugin.getLogger().severe("Error details: " + e.getMessage());
                Component errorMessage = plugin.getFormattedMessage("command-execution-failed");
                attacker.sendMessage(errorMessage);
            }
        }
    }
}