package de.nicouschulas.customhitcommand;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import org.bstats.bukkit.Metrics;

public final class CustomHitCommand extends JavaPlugin {

    private Component chatPrefix;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').hexColors().build();

    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;
    private double particleOffsetX;
    private double particleOffsetY;
    private double particleOffsetZ;

    @Override
    public void onEnable() {
        getLogger().info("CustomHitCommand started successfully!");

        saveDefaultConfig();
        loadPrefix();
        loadParticleSettings();

        getServer().getPluginManager().registerEvents(new HitListener(this), this);

        Objects.requireNonNull(this.getCommand("chc")).setExecutor(new ReloadCommand(this));

        int pluginId = 26615;
        new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomHitCommand shutdown successfully!");
    }

    public void loadPrefix() {
        String rawPrefix = getConfig().getString("prefix", "&7[&cCHC&7] ");
        this.chatPrefix = legacySerializer.deserialize(rawPrefix);
    }

    public void loadParticleSettings() {
        this.particlesEnabled = getConfig().getBoolean("particles.enabled", false);
        String particleTypeName = getConfig().getString("particles.type", "VILLAGER_HAPPY");
        try {
            this.particleType = Particle.valueOf(particleTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid particle type in config.yml: " + particleTypeName);
            this.particleType = Particle.HAPPY_VILLAGER;
        }
        this.particleCount = getConfig().getInt("particles.count", 10);
        this.particleOffsetX = getConfig().getDouble("particles.offset-x", 0.5);
        this.particleOffsetY = getConfig().getDouble("particles.offset-y", 0.5);
        this.particleOffsetZ = getConfig().getDouble("particles.offset-z", 0.5);
    }

    public Component getFormattedMessage(String messageKey) {
        String message = getConfig().getString("messages." + messageKey, "Message not found: " + messageKey);
        Component messageComponent = legacySerializer.deserialize(message);
        return chatPrefix.append(messageComponent);
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