package de.nicouschulas.customhitcommand;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public record ReloadCommand(CustomHitCommand plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getFormattedMessage("reload-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("customhitcommand.reload")) {
                sender.sendMessage(plugin.getFormattedMessage("no-permission"));
                return true;
            }

            plugin.reloadConfig();
            sender.sendMessage(plugin.getFormattedMessage("reload-success"));
            return true;
        } else if (args[0].equalsIgnoreCase("sethititem")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be executed by a player.");
                return true;
            }

            if (!player.hasPermission("customhitcommand.sethititem")) {
                player.sendMessage("You don't have permission to use this command.");
                return true;
            }


            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                player.sendMessage("You must hold an item in your hand to mark it!");
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                player.sendMessage("This item cannot be marked! ");
                return true;
            }

            meta.getPersistentDataContainer().set(CustomHitCommand.CUSTOM_ITEM_KEY, PersistentDataType.STRING, "true");
            item.setItemMeta(meta);

            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("&aThe item has been successfully marked as a hit item!"));
            return true;
        }

        return false;

    }
}