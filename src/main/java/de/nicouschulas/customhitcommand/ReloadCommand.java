package de.nicouschulas.customhitcommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record ReloadCommand(CustomHitCommand plugin) implements CommandExecutor, TabCompleter {

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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("customhitcommand.reload")) {
            return null;
        }

        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                return Collections.singletonList("reload");
            }
        }

        return null;
    }
}