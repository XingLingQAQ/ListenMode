package me.matsubara.listenmode;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import lombok.Getter;
import me.matsubara.listenmode.data.EntityData;
import me.matsubara.listenmode.gui.LevelGUI;
import me.matsubara.listenmode.listener.InventoryClick;
import me.matsubara.listenmode.listener.PlayerJoin;
import me.matsubara.listenmode.listener.PlayerQuit;
import me.matsubara.listenmode.listener.PlayerToggleSneak;
import me.matsubara.listenmode.manager.DataManager;
import me.matsubara.listenmode.manager.EconomyManager;
import me.matsubara.listenmode.runnable.ListenTask;
import me.matsubara.listenmode.util.GlowingEntities;
import me.matsubara.listenmode.util.PluginUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public final class ListenModePlugin extends JavaPlugin {

    private Set<EntityData> entityData;
    private Set<ListenTask> tasks;

    private DataManager dataManager;
    private EconomyManager economyManager;

    private GlowingEntities glowingEntities;

    @Override
    public void onEnable() {
        // Disable plugin if server version is older than 1.13.
        if (!PluginUtils.supports(13)) {
            getLogger().info("This plugin only works from 1.13 and up disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().warning("You must install ProtocolLib to use this plugin, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            economyManager = new EconomyManager(this);
            if (!economyManager.isValid()) economyManager = null;
        } else {
            economyManager = null;
            getLogger().info("Vault isn't installed, level feature disabled.");
        }

        glowingEntities = new GlowingEntities(this);

        // Config-related.
        saveDefaultConfig();

        // Initialize entities data.
        entityData = new HashSet<>();
        loadData();

        tasks = new HashSet<>();

        dataManager = new DataManager(this);

        // Register bukkit listeners.
        getServer().getPluginManager().registerEvents(new InventoryClick(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(this), this);
        getServer().getPluginManager().registerEvents(new PlayerToggleSneak(this), this);

        // Register protocol listeners.
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener() {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (!reduceSoundVolume()) return;

                // Player isn't listening.
                if (!isListening(event.getPlayer())) return;

                Sound playing = event.getPacket().getSoundEffects().readSafely(0);

                // Don't reduce heart-beat sound.
                Sound sound = Sound.valueOf(getHeartBeatSound());
                if (isHeartBeatEnabled() && sound == playing) return;

                // Reduce volume.
                event.getPacket().getFloat().write(0, 0.1f);
            }

            @Override
            public void onPacketReceiving(PacketEvent event) {

            }

            @SuppressWarnings("deprecation")
            @Override
            public ListeningWhitelist getSendingWhitelist() {
                return ListeningWhitelist
                        .newBuilder()
                        .types(PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.CUSTOM_SOUND_EFFECT)
                        .priority(ListenerPriority.HIGHEST)
                        .gamePhase(GamePhase.PLAYING)
                        .monitor()
                        .build();
            }

            @Override
            public ListeningWhitelist getReceivingWhitelist() {
                return ListeningWhitelist
                        .newBuilder()
                        .build();
            }

            @Override
            public Plugin getPlugin() {
                return ListenModePlugin.this;
            }
        });

        PluginCommand command = getCommand("listenmode");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public void onDisable() {
        glowingEntities.disable();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!command.getName().equals("listenmode")) return true;

        if (!(sender instanceof Player)) {
            sender.sendMessage(PluginUtils.translate(getConfig().getString("messages.from-console")));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (!canWithdraw()) {
                sender.sendMessage(PluginUtils.translate(getConfig().getString("messages.feature-disabled")));
                return true;
            }
            new LevelGUI(this, player, dataManager.getLevel(player));
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("listenmode.reload")) {
                    player.sendMessage(PluginUtils.translate(getConfig().getString("messages.no-permission")));
                    return true;
                }

                for (ListenTask task : tasks) {
                    // Cancel task.
                    task.cancel();

                    // Remove glow.
                    task.removeGlowing();
                }

                tasks.clear();

                reloadConfig();
                dataManager.reloadConfig();

                loadData();
                player.sendMessage(PluginUtils.translate(getConfig().getString("messages.reload")));
                return true;
            } else if (args[0].equalsIgnoreCase("toggle")) {
                boolean enabled = dataManager.toggleState(player);
                if (enabled) {
                    player.sendMessage(PluginUtils.translate(getConfig().getString("messages.enabled")));
                } else {
                    player.sendMessage(PluginUtils.translate(getConfig().getString("messages.disabled")));
                }
                return true;
            }
        }

        player.sendMessage(PluginUtils.translate(getConfig().getString("messages.invalid")));
        return true;
    }

    private void loadData() {
        entityData.clear();

        ConfigurationSection entitiesSection = getConfig().getConfigurationSection("entities");
        if (entitiesSection == null) return;

        for (String path : entitiesSection.getKeys(false)) {
            EntityType type;
            try {
                type = EntityType.valueOf(path);
            } catch (IllegalArgumentException exception) {
                getLogger().info("Invalid entity type: " + path);
                continue;
            }

            // If color is none, will use the color based on his behaviour.
            String colorString = getConfig().getString("entities." + path + ".color", "NONE");

            ChatColor color;
            try {
                color = ChatColor.valueOf(colorString);
            } catch (IllegalArgumentException exception) {
                color = null;
            }

            double radius = getConfig().getDouble("entities." + path + ".radius", getMaximumRadius());

            // No need to save.
            if (color == null && radius == getMaximumRadius()) continue;
            entityData.add(new EntityData(type, color, radius));
        }
    }

    public @NotNull EntityData getDataByType(EntityType type) {
        for (EntityData data : this.entityData) {
            if (data.getType() == type) return data;
        }
        return new EntityData(type, null, getMaximumRadius());
    }

    public boolean isListening(Player player) {
        return getTask(player) != null;
    }

    public @Nullable ListenTask getTask(Player player) {
        for (ListenTask task : tasks) {
            if (task.getPlayer().equals(player)) return task;
        }
        return null;
    }

    public double getLevelRange(int price) {
        if (price == 0) return 5.0d;
        return getConfig().getDouble("levels.level-" + price + ".range");
    }

    public double getLevelPrice(int price) {
        return getConfig().getDouble("levels.level-" + price + ".price");
    }

    public String getLevelUpSound() {
        return getConfig().getString("level-up-sound");
    }

    public double getMaximumRadius() {
        return getConfig().getDouble("maximum-radius");
    }

    public String getRequiredPermission() {
        return getConfig().getString("required-permission");
    }

    public ChatColor getDefaultColor(String type) {
        String colorString = getConfig().getString("default-colors." + type);
        try {
            return ChatColor.valueOf(colorString);
        } catch (IllegalArgumentException exception) {
            return ChatColor.WHITE;
        }
    }

    public boolean isRedWarningEnabled() {
        return getConfig().getBoolean("red-warning");
    }

    public boolean isFreezeEnabled() {
        return getConfig().getBoolean("freeze-effect.enabled");
    }

    public float getWalkSpeed() {
        return (float) getConfig().getDouble("freeze-effect.walk-speed");
    }

    public boolean preventJump() {
        return getConfig().getBoolean("freeze-effect.prevent-jump");
    }

    public boolean isHeartBeatEnabled() {
        return getConfig().getBoolean("heart-beat-effect.enabled");
    }

    public String getHeartBeatSound() {
        return getConfig().getString("heart-beat-effect.sound");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSoundOnly() {
        return isHeartBeatEnabled() && getConfig().getBoolean("heart-beat-effect.sound-only");
    }

    public boolean isSoundGlobal() {
        return getConfig().getBoolean("heart-beat-effect.sound-global");
    }

    public boolean reduceSoundVolume() {
        return getConfig().getBoolean("heart-beat-effect.reduce-sound-volume");
    }

    public @NotNull List<String> getIgnoredEntities() {
        return getConfig().getStringList("ignored-entities");
    }

    public boolean ignoreProjectiles() {
        return getConfig().getBoolean("ignore-projectiles");
    }

    public boolean canWithdraw() {
        return economyManager != null;
    }

    public boolean substractMoney(Player player, double money) {
        if (!canWithdraw()) return false;
        if (!economyManager.getEconomy().has(player, money)) return false;
        return economyManager.getEconomy().withdrawPlayer(player, money).transactionSuccess();
    }
}