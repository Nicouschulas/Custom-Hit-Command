package de.nicouschulas.customhitcommand;

import org.bukkit.NamespacedKey;

public record ParsedNbtTag(NamespacedKey key, String expectedValue) {}