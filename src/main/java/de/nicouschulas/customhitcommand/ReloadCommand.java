package de.nicouschulas.customhitcommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final CustomHitCommand plugin;

    public ReloadCommand(CustomHitCommand plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("customhitcommand.reload")) {
            sender.sendMessage(plugin.getFormattedMessage("no-permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.loadPrefix();
            plugin.getLogger().info("&aCustom Hit Command reloaded successfully!");
            sender.sendMessage(plugin.getFormattedMessage("reload-success"));
            return true;
        }

        sender.sendMessage(plugin.getFormattedMessage("reload-usage"));
        return false;
    }
}