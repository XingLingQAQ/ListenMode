package me.matsubara.listenmode.listener;

import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.runnable.ListenTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class PlayerToggleSneak implements Listener {

    private final ListenModePlugin plugin;
    private final static int DELAY = 10;

    public PlayerToggleSneak(ListenModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Check if player meets conditions for the ability.
        if (!event.isSneaking() || !meetsConditions(player)) return;

        new BukkitRunnable() {
            private int count = DELAY;

            public void run() {
                if (!player.isSneaking() || !meetsConditions(player)) {
                    cancel();
                    return;
                }

                if (count == 0) {
                    cancel();

                    // Cancel old task.
                    ListenTask old = plugin.getTask(player);
                    if (old != null) old.cancel();

                    // Save task.
                    plugin.getTasks().add(new ListenTask(plugin, player));
                    return;
                }
                count--;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean meetsConditions(Player player) {
        return player.hasPermission(plugin.getRequiredPermission()) && !player.isFlying();
    }
}