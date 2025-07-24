package de.nicouschulas.customhitcommand;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomHitCommand extends JavaPlugin {

    private String chatPrefix;
    private MiniMessage miniMessage;

    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;

    @Override
    public void onEnable() {
        getLogger().info("CustomHitCommand wurde gestartet!");

        this.miniMessage = MiniMessage.miniMessage();

        saveDefaultConfig();
        loadPrefix();
        loadParticleSettings();

        getServer().getPluginManager().registerEvents(new HitListener(this), this);

        Objects.requireNonNull(this.getCommand("chc")).setExecutor(new ReloadCommand(this));

        int pluginId = 26615;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomHitCommand wurde beendet!");
    }

    public void loadPrefix() {
        this.chatPrefix = getConfig().getString("prefix", "&7[&cCHC&7] ");
    }

    public void loadParticleSettings() {
        this.particlesEnabled = getConfig().getBoolean("particles.enabled", false);
        String particleTypeName = getConfig().getString("particles.type", "VILLAGER_HAPPY");
        try {
            this.particleType = Particle.valueOf(particleTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Ungültiger Partikel-Typ in der config.yml: " + particleTypeName + ". Verwende stattdessen VILLAGER_HAPPY.");
            this.particleType = Particle.HAPPY_VILLAGER;
        }
        this.particleCount = getConfig().getInt("particles.count", 10);
        this.particleOffsetX = getConfig().getDouble("particles.offset-x", 0.5);
        this.particleOffsetY = getConfig().getDouble("particles.offset-y", 0.5);
        this.particleOffsetZ = getConfig().getDouble("particles.offset-z", 0.5);
    }

    public Component getFormattedMessage(String messageKey) {
        String message = getConfig().getString("messages." + messageKey, "Nachricht nicht gefunden: " + messageKey);
        String fullMessage = chatPrefix + message;
        return miniMessage.deserialize(fullMessage);
    }

    public void spawnHitParticles(Location location) {
        if (!particlesEnabled) {
            return;
        }

        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(
                particleType,
                location,
                particleCount,
                particleOffsetX,
                particleOffsetY,
                particleOffsetZ
        );
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadPrefix();
        loadParticleSettings();
    }
}