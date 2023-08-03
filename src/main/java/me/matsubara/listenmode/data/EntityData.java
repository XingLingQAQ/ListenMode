package me.matsubara.listenmode.data;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

@Getter
public final class EntityData {

    private final EntityType type;
    private final ChatColor color;
    private final double radius;

    public EntityData(EntityType type, ChatColor color, double radius) {
        this.type = type;
        this.color = color;
        this.radius = radius;
    }
}