package de.nicouschulas.customhitcommand;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public record HitListener(CustomHitCommand plugin) implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled() && plugin.isIgnoreCancelledHits()) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player hittedPlayer)) {
            return;
        }

        if (!attacker.hasPermission("customhitcommand.use")) {
            return;
        }

        ItemStack handItem = attacker.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta itemMeta = handItem.getItemMeta();
        boolean isMarkedItem = itemMeta != null && itemMeta.getPersistentDataContainer().has(CustomHitCommand.CUSTOM_ITEM_KEY, PersistentDataType.STRING);

        boolean isConfigItemMatch = handItem.getType() == plugin.getHitItemMaterial();

        boolean executeCommand = isMarkedItem || (plugin.shouldCheckMaterialGroup() && isConfigItemMatch);

        if (executeCommand) {
            if (!SecurityUtils.canPlayerUseCommand(attacker.getUniqueId())) {
                long remainingMs = SecurityUtils.getRemainingCooldown(attacker.getUniqueId());
                long remainingSeconds = (remainingMs / 1000) + 1;

                Component cooldownMessage = plugin.getFormattedMessage("cooldown-message")
                        .replaceText(builder -> builder.match("%seconds%").replacement(String.valueOf(remainingSeconds)));

                attacker.sendMessage(cooldownMessage);
                return;
            }

            String commandTemplate = plugin.getCommandToExecute();
            if (!SecurityUtils.isCommandTemplateSafe(commandTemplate)) {
                plugin.getLogger().severe("Blocked unsafe command template: " + commandTemplate);
                return;
            }

            String finalCommand = commandTemplate.replace("%hitted_player%", hittedPlayer.getName());
            String executorType = plugin.getCommandExecutor();

            try {
                if (executorType.equalsIgnoreCase("player")) {
                    attacker.chat("/" + finalCommand);
                } else {
                    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                    Bukkit.dispatchCommand(console, finalCommand);
                }

                plugin.spawnHitParticles(hittedPlayer.getLocation());

                if (plugin.isEnhancedSecurityLogging()) {
                    plugin.getLogger().info(attacker.getName() + " triggered hit-command on " + hittedPlayer.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing hit-command: " + e.getMessage());
                attacker.sendMessage(plugin.getFormattedMessage("command-execution-failed"));
            }
        }
    }
}