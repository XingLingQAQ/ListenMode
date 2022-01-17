package me.matsubara.listenmode.data;

import me.matsubara.listenmode.glowapi.GlowAPI;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlowData {

    //Maps player-UUID to Color
    public final Map<UUID, GlowAPI.Color> colorMap = new ConcurrentHashMap<>();

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        GlowData glowData = (GlowData) object;
        return Objects.equals(colorMap, glowData.colorMap);
    }

    @Override
    public int hashCode() {
        return colorMap.hashCode();
    }
}