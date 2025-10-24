package me.matsubara.listenmode.listener;

import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.runnable.ListenTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerQuit implements Listener {

    private final ListenModePlugin plugin;

    public PlayerQuit(ListenModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (ListenTask task : plugin.getTasks()) {
            task.removeGlowing(player, task.getPlayer());
        }

        // Cancel the task when leaving.
        ListenTask task = plugin.getTask(player);
        if (task == null) return;

        task.cancel();
        task.removeGlowing();
    }
}