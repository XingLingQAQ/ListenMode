package me.matsubara.listenmode.listener;

import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.glowapi.GlowAPI;
import me.matsubara.listenmode.runnable.ListenTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuit implements Listener {

    private final ListenModePlugin plugin;

    public PlayerQuit(ListenModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (GlowAPI.isGlowing(player, online)) {
                GlowAPI.setGlowing(player, null, online);
            }
        }

        // Cancel task when leaving.
        ListenTask task = plugin.getTask(player);
        if (task == null) return;

        task.cancel();
        task.removeGlowing();
    }
}