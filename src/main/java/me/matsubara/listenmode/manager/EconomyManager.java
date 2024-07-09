package me.matsubara.listenmode.manager;

import lombok.Getter;
import me.matsubara.listenmode.ListenModePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import static org.bukkit.Bukkit.getServer;

@Getter
public final class EconomyManager {

    private Economy economy;

    public EconomyManager(ListenModePlugin plugin) {
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            plugin.getLogger().info("No economy provider found.");
            return;
        }

        plugin.getLogger().info("Using " + provider.getPlugin().getName() + " as the economy provider.");
        economy = provider.getProvider();
    }

    public boolean isValid() {
        return economy != null;
    }
}