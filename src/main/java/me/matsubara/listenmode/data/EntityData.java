package me.matsubara.listenmode.data;

import me.matsubara.listenmode.glowapi.GlowAPI;
import org.bukkit.entity.EntityType;

public final class EntityData {

    private final EntityType type;
    private final GlowAPI.Color color;
    private final double radius;

    public EntityData(EntityType type, GlowAPI.Color color, double radius) {
        this.type = type;
        this.color = color;
        this.radius = radius;
    }

    public EntityType getType() {
        return type;
    }

    public GlowAPI.Color getColor() {
        return color;
    }

    public double getRadius() {
        return radius;
    }
}