package de.nicouschulas.customhitcommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public record ReloadCommand(CustomHitCommand plugin) implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(plugin.getFormattedMessage("reload-usage"));
            return true;
        }

        if (!sender.hasPermission("customhitcommand.reload")) {
            sender.sendMessage(plugin.getFormattedMessage("no-permission"));
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage(plugin.getFormattedMessage("reload-success"));
        return true;
    }
}