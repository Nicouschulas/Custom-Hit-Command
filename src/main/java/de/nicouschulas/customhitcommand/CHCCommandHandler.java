package de.nicouschulas.customhitcommand;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.bukkit.command.TabCompleter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public record CHCCommandHandler(CustomHitCommand plugin) implements CommandExecutor, TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> subCommands = new ArrayList<>();
        subCommands.add("reload");
        subCommands.add("sethititem");

        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getFormattedMessage("command-usage"));
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
        }

        else if (args[0].equalsIgnoreCase("sethititem")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfig().getString("messages.sethititem-player-only", "This command can only be executed by a player."));
                return true;
            }

            if (!player.hasPermission("customhitcommand.sethititem")) {
                sender.sendMessage(plugin.getFormattedMessage("no-permission"));
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                player.sendMessage(plugin.getFormattedMessage("sethititem-no-hand-item"));
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                player.sendMessage(plugin.getFormattedMessage("sethititem-no-item-meta"));
                return true;
            }

            meta.getPersistentDataContainer().set(CustomHitCommand.CUSTOM_ITEM_KEY, PersistentDataType.STRING, "true");
            item.setItemMeta(meta);

            player.sendMessage(plugin.getFormattedMessage("sethititem-success"));

            return true;
        }

        sender.sendMessage(plugin.getFormattedMessage("command-usage"));
        return true;
    }
}