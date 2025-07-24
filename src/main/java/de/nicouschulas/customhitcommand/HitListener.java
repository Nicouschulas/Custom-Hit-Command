package de.nicouschulas.customhitcommand;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.Bukkit;

public class HitListener implements Listener {

    private final CustomHitCommand plugin;

    public HitListener(CustomHitCommand plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof Player hittedPlayer)) {
            return;
        }

        ItemStack handItem = attacker.getInventory().getItemInMainHand();

        FileConfiguration config = plugin.getConfig();

        String configuredMaterialName = config.getString("hit-item", "IRON_SWORD");
        Material requiredMaterial;

        try {
            requiredMaterial = Material.valueOf(configuredMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            String warning1 = plugin.getFormattedMessage("invalid-material-warning-1").toString();
            String warning2 = plugin.getFormattedMessage("invalid-material-warning-2").toString();

            plugin.getLogger().warning(warning1.replace("%material%", configuredMaterialName));
            plugin.getLogger().warning(warning2);

            requiredMaterial = Material.IRON_SWORD;
        }

        if (handItem.getType() == requiredMaterial) {
            String command = config.getString("command-to-execute", "duel %hitted_player%");
            String finalCommand = command.replace("%hitted_player%", hittedPlayer.getName());

            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            Bukkit.dispatchCommand(console, finalCommand);

            plugin.spawnHitParticles(hittedPlayer.getLocation());

            String logMessageTemplate = plugin.getFormattedMessage("command-executed-log").toString();

            String finalLogMessage = logMessageTemplate
                    .replace("%attacker%", attacker.getName())
                    .replace("%hitted_player%", hittedPlayer.getName())
                    .replace("%item%", handItem.getType().name())
                    .replace("%command%", finalCommand);

            plugin.getLogger().info(finalLogMessage);
        }
    }
}