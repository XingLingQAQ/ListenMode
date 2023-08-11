package me.matsubara.listenmode.gui;

import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.util.ItemBuilder;
import me.matsubara.listenmode.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class LevelGUI implements InventoryHolder {

    private final ListenModePlugin plugin;
    private final Inventory inventory;

    public LevelGUI(@NotNull ListenModePlugin plugin, Player player, int level) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, InventoryType.HOPPER, PluginUtils
                .translate(plugin.getConfig().getString("level-up-gui.title"))
                .replace("%current%", String.valueOf(plugin.getLevelRange(plugin.getDataManager().getLevel(player)))));

        for (int i = 0; i < 5; i++) {
            boolean isLocked = i + 1 > level;
            inventory.addItem(getGUIItem(isLocked ? "locked" : "unlocked", i + 1));
        }

        player.openInventory(inventory);
    }

    private ItemStack getGUIItem(String type, int level) {
        String path = "level-up-gui." + type + ".";
        return new ItemBuilder(plugin.getConfig().getString(path + "material"))
                .setDisplayName(plugin.getConfig().getString(path + "display-name", "")
                        .replace("%level%", String.valueOf(level))
                        .replace("%range%", String.valueOf(plugin.getLevelRange(level))))
                .setLore(plugin.getConfig().getStringList(path + "lore"), line -> line
                        .replace("%range%", String.valueOf(plugin.getLevelRange(level)))
                        .replace("%price%", plugin.getEconomyManager().getEconomy().format(plugin.getLevelPrice(level))))
                .build();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}