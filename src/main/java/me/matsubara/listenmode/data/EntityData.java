package me.matsubara.listenmode.data;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public record EntityData(EntityType type, ChatColor color, @Nullable Double radius) {
}