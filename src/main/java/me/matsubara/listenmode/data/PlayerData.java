package me.matsubara.listenmode.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public final class PlayerData {

    private final UUID uuid;
    private boolean enabled;
    private int level;

    public PlayerData(UUID uuid, boolean enabled, int level) {
        this.uuid = uuid;
        this.enabled = enabled;
        this.level = level;
    }
}