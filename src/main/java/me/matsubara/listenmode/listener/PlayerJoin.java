package me.matsubara.listenmode.listener;

import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.glowapi.GlowAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PlayerJoin implements Listener {

    // May be needed in a future.
    @SuppressWarnings("all")
    private final ListenModePlugin plugin;

    public PlayerJoin(ListenModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        GlowAPI.initTeams(player);

        // Set default walk speed.
        if (player.getWalkSpeed() != 0.2f) player.setWalkSpeed(0.2f);

        PotionEffect effect = player.getPotionEffect(PotionEffectType.JUMP);
        if (effect == null) return;

        // If a server crashes while a player is listening, hey may have infinite jump effect.
        if (effect.getAmplifier() == -10) player.removePotionEffect(PotionEffectType.JUMP);
    }
}