package me.matsubara.listenmode.listener;

import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.gui.LevelGUI;
import me.matsubara.listenmode.util.PluginUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class InventoryClick implements Listener {

    private final ListenModePlugin plugin;

    public InventoryClick(ListenModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        Inventory inventory = event.getClickedInventory();

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(inventory.getHolder() instanceof LevelGUI)) return;

        event.setCancelled(true);

        int level = event.getRawSlot() + 1;

        if (plugin.getDataManager().isLevelUnlocked(player, level)) {
            player.sendMessage(PluginUtils
                    .translate(plugin.getConfig().getString("messages.already-unlocked"))
                    .replace("%level%", String.valueOf(level)));
        } else {
            if (plugin.substractMoney(player, plugin.getLevelPrice(level))) {
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getLevelUpSound()), 1.0f, 1.0f);
                player.sendMessage(PluginUtils
                        .translate(plugin.getConfig().getString("messages.unlocked"))
                        .replace("%level%", String.valueOf(level)));
                plugin.getDataManager().unlockLevel(player, level);
            } else {
                player.sendMessage(PluginUtils.translate(plugin.getConfig().getString("messages.not-enough")));
            }
        }

        plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
    }
}