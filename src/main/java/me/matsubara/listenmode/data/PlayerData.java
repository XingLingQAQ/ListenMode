package me.matsubara.listenmode.data;

import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private boolean enabled;
    private int level;

    public PlayerData(UUID uuid, boolean enabled, int level) {
        this.uuid = uuid;
        this.enabled = enabled;
        this.level = level;
    }

    public UUID getUUID() {
        return uuid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}