package me.matsubara.listenmode;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.google.common.collect.ImmutableList;
import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.Getter;
import me.matsubara.listenmode.data.EntityData;
import me.matsubara.listenmode.gui.LevelGUI;
import me.matsubara.listenmode.listener.InventoryClick;
import me.matsubara.listenmode.listener.PlayerJoin;
import me.matsubara.listenmode.listener.PlayerQuit;
import me.matsubara.listenmode.listener.PlayerToggleSneak;
import me.matsubara.listenmode.manager.DataManager;
import me.matsubara.listenmode.manager.DatabaseManager;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public final class ListenModePlugin extends JavaPlugin {

    private Set<EntityData> entityData;
    private Set<ListenTask> tasks;

    private DatabaseManager databaseManager;
    private DataManager dataManager;
    private EconomyManager economyManager;

    private GlowingEntities glowingEntities;

    private static final List<String> ARGS = ImmutableList.of("reload", "toggle", "upgrades");
    private static final List<String> HELP = Stream.of(
                    "&8----------------------------------------",
                    "&6&lListenMode &f&oCommands &c(optional) <required>",
                    "&e/lm reload &f- &7Reload configuration files.",
                    "&e/lm toggle &f- &7Toggle the state of the ability.",
                    "&e/lm upgrades &f- &7Open upgrades GUI.",
                    "&8----------------------------------------")
            .map(PluginUtils::translate)
            .collect(Collectors.toList());

    @Override
    public void onEnable() {
        PluginManager manager = getServer().getPluginManager();

        // Disable plugin if server version is older than 1.13.
        if (!PluginUtils.supports(13)) {
            getLogger().info("This plugin only works from 1.13 and up disabling...");
            manager.disablePlugin(this);
            return;
        }

        if (!manager.isPluginEnabled("ProtocolLib")) {
            getLogger().warning("You must install ProtocolLib to use this plugin, disabling...");
            manager.disablePlugin(this);
            return;
        }

        if (manager.isPluginEnabled("Vault")) {
            economyManager = new EconomyManager(this);
            if (!economyManager.isValid()) economyManager = null;
        } else {
            economyManager = null;
            getLogger().info("Vault isn't installed, level feature disabled.");
        }

        glowingEntities = new GlowingEntities(this);

        // Config-related.
        saveDefaultConfig();

        try {
            ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"), Stream.of("entities", "level-up-gui.locked", "level-up-gui.unlocked")
                    .filter(path -> getConfig().contains(path))
                    .collect(Collectors.toList()));
            reloadConfig();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        // Initialize entities data.
        entityData = new HashSet<>();
        loadData();

        tasks = new HashSet<>();

        FileConfiguration config = getConfig();
        databaseManager = new DatabaseManager(this,
                config.getString("storage.mysql.host"),
                config.getInt("storage.mysql.port"),
                config.getString("storage.mysql.database"),
                config.getString("storage.mysql.username"),
                config.getString("storage.mysql.password"),
                config.getBoolean("storage.mysql.ssl"));

        if (config.getString("storage.type", "FLAT").equals("MYSQL") && !databaseManager.isValid()) {
            manager.disablePlugin(this);
            return;
        }

        dataManager = new DataManager(this);

        // Register bukkit listeners.
        manager.registerEvents(new InventoryClick(this), this);
        manager.registerEvents(new PlayerJoin(this), this);
        manager.registerEvents(new PlayerQuit(this), this);
        manager.registerEvents(new PlayerToggleSneak(this), this);

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

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equals("listenmode")) return null;
        if (args.length == 1) return StringUtil.copyPartialMatches(args[0], ARGS, new ArrayList<>());
        return Collections.emptyList();
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

        if (args.length != 1 || !ARGS.contains(args[0].toLowerCase())) {
            HELP.forEach(player::sendMessage);
            return true;
        }

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
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            player.sendMessage(PluginUtils.translate(getConfig().getString("messages." + (dataManager.toggleState(player) ? "enabled" : "disabled"))));
            return true;
        }

        if (args[0].equalsIgnoreCase("upgrades")) {
            if (!canWithdraw()) {
                sender.sendMessage(PluginUtils.translate(getConfig().getString("messages.feature-disabled")));
                return true;
            }
            new LevelGUI(this, player, dataManager.getLevel(player));
            return true;
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