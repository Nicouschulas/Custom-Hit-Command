package de.nicouschulas.customhitcommand;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record CommandHandler(CustomHitCommand plugin) implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("reload", "sethititem");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> availableSubcommands = SUB_COMMANDS.stream()
                    .filter(sub -> sender.hasPermission("customhitcommand." + sub))
                    .toList();

            StringUtil.copyPartialMatches(args[0], availableSubcommands, completions);
            Collections.sort(completions);
            return completions;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getFormattedMessage("command-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "sethititem" -> handleSetHitItem(sender);
            default -> sender.sendMessage(plugin.getFormattedMessage("command-usage"));
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("customhitcommand.reload")) {
            sender.sendMessage(plugin.getFormattedMessage("no-permission"));
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage(plugin.getFormattedMessage("reload-success"));
    }

    private void handleSetHitItem(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getFormattedMessage("sethititem-player-only"));
            return;
        }

        if (!player.hasPermission("customhitcommand.sethititem")) {
            player.sendMessage(plugin.getFormattedMessage("no-permission"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(plugin.getFormattedMessage("sethititem-no-hand-item"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(plugin.getFormattedMessage("sethititem-no-item-meta"));
            return;
        }

        meta.getPersistentDataContainer().set(CustomHitCommand.CUSTOM_ITEM_KEY, PersistentDataType.STRING, "true");
        item.setItemMeta(meta);

        player.sendMessage(plugin.getFormattedMessage("sethititem-success"));
    }
}