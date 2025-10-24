package me.matsubara.listenmode;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.google.common.collect.ImmutableList;
import com.tchristofferson.configupdater.ConfigUpdater;
import fr.skytasul.glowingentities.GlowingEntities;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import me.matsubara.listenmode.command.MainCommand;
import me.matsubara.listenmode.data.EntityData;
import me.matsubara.listenmode.listener.InventoryClick;
import me.matsubara.listenmode.listener.PlayerJoin;
import me.matsubara.listenmode.listener.PlayerQuit;
import me.matsubara.listenmode.listener.PlayerToggleSneak;
import me.matsubara.listenmode.manager.DataManager;
import me.matsubara.listenmode.manager.DatabaseManager;
import me.matsubara.listenmode.manager.EconomyManager;
import me.matsubara.listenmode.runnable.ListenTask;
import me.matsubara.listenmode.util.PluginUtils;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public final class ListenModePlugin extends JavaPlugin {

    private Set<EntityData> entityData;
    private Set<ListenTask> tasks;

    private DatabaseManager databaseManager;
    private DataManager dataManager;
    private EconomyManager economyManager;

    private GlowingEntities glowingEntities;

    private static final List<String> SPECIAL_SECTIONS = List.of(
            "entities",
            "level-up-gui.locked",
            "level-up-gui.unlocked");

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PluginManager manager = getServer().getPluginManager();

        // Disable the plugin if the server version is older than 1.17.
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_17)) {
            getLogger().info("This plugin only works from 1.17 and up, disabling...");
            manager.disablePlugin(this);
            return;
        }

        if (!manager.isPluginEnabled("packetevents")) {
            getLogger().warning("You must install PacketEvents to use this plugin, disabling...");
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
        updateMainConfig();

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
        PacketEvents.getAPI().getEventManager().registerListener(new SimplePacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketPlaySend(@NotNull PacketPlaySendEvent event) {
                if (!(event.getPlayer() instanceof Player player)) return;
                PacketType.Play.Server type = event.getPacketType();

                if (type == PacketType.Play.Server.SOUND_EFFECT) {
                    WrapperPlayServerSoundEffect wrapper = new WrapperPlayServerSoundEffect(event);
                    handle(player, wrapper.getSound(), () -> wrapper.setVolume(0.1f));
                } else if (type == PacketType.Play.Server.ENTITY_SOUND_EFFECT) {
                    WrapperPlayServerEntitySoundEffect wrapper = new WrapperPlayServerEntitySoundEffect(event);
                    handle(player, wrapper.getSound(), () -> wrapper.setVolume(0.1f));
                }
            }

            private void handle(Player player, com.github.retrooper.packetevents.protocol.sound.Sound sound, Runnable runnable) {
                if (sound == null || !reduceSoundVolume() || !isListening(player)) return;

                try {
                    Sound playing = Registry.SOUNDS.get(NamespacedKey.minecraft(sound.getSoundId().getKey()));

                    // Don't reduce heart-beat sound.
                    Sound heartBeatSound = PluginUtils.getOrNull(Sound.class, getHeartBeatSound());
                    if (isHeartBeatEnabled() && heartBeatSound == playing) return;

                    // Reduce volume.
                    runnable.run();
                } catch (Exception ignored) {

                }
            }
        });

        PluginCommand command = getCommand("listenmode");
        if (command == null) return;

        MainCommand main = new MainCommand(this);
        command.setExecutor(main);
        command.setTabCompleter(main);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        if (glowingEntities != null) glowingEntities.disable();
        if (tasks != null) tasks.forEach(ListenTask::stop);
    }

    public void updateMainConfig() {
        updateConfig(
                getDataFolder().getPath(),
                "config.yml",
                file -> reloadConfig(),
                file -> saveDefaultConfig(),
                config -> SPECIAL_SECTIONS.stream().filter(config::contains).toList(),
                Collections.emptyList());
    }

    public void updateConfig(String folderName,
                             String fileName,
                             Consumer<File> reloadAfterUpdating,
                             Consumer<File> resetConfiguration,
                             Function<FileConfiguration, List<String>> ignoreSection,
                             List<ConfigChanges> changes) {
        File file = new File(folderName, fileName);

        FileConfiguration config = PluginUtils.reloadConfig(this, file, resetConfiguration);
        if (config == null) {
            getLogger().severe("Can't find {" + file.getName() + "}!");
            return;
        }

        for (ConfigChanges change : changes) {
            handleConfigChanges(file, config, change.predicate(), change.consumer(), change.newVersion());
        }

        try {
            ConfigUpdater.update(
                    this,
                    fileName,
                    file,
                    ignoreSection.apply(config));
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        reloadAfterUpdating.accept(file);
    }

    private void handleConfigChanges(@NotNull File file, FileConfiguration config, @NotNull Predicate<FileConfiguration> predicate, Consumer<FileConfiguration> consumer, int newVersion) {
        if (!predicate.test(config)) return;

        int previousVersion = config.getInt("config-version", 0);
        getLogger().info("Updated {%s} config to v{%s} (from v{%s})".formatted(file.getName(), newVersion, previousVersion));

        consumer.accept(config);
        config.set("config-version", newVersion);

        try {
            config.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public record ConfigChanges(Predicate<FileConfiguration> predicate,
                                Consumer<FileConfiguration> consumer,
                                int newVersion) {

        public static @NotNull Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private final List<ConfigChanges> changes = new ArrayList<>();

            public Builder addChange(Predicate<FileConfiguration> predicate,
                                     Consumer<FileConfiguration> consumer,
                                     int newVersion) {
                changes.add(new ConfigChanges(predicate, consumer, newVersion));
                return this;
            }

            public List<ConfigChanges> build() {
                return ImmutableList.copyOf(changes);
            }
        }
    }

    public void loadData() {
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

            // If colour is none, will use the colour based on his behaviour.
            String colorString = getConfig().getString("entities." + path + ".color", "NONE");

            ChatColor color;
            try {
                color = ChatColor.valueOf(colorString);
            } catch (IllegalArgumentException exception) {
                color = null;
            }

            double radius = getConfig().getDouble("entities." + path + ".radius", Double.MIN_VALUE);

            // No need to save.
            if (color == null && radius == Double.MIN_VALUE) continue;
            entityData.add(new EntityData(type, color, radius == Double.MIN_VALUE ? null : radius));
        }
    }

    public @NotNull EntityData getDataByType(EntityType type) {
        for (EntityData data : this.entityData) {
            if (data.type() == type) return data;
        }
        return new EntityData(type, null, null);
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

    public double getLevelRange(Player player) {
        return getLevelRange(dataManager.getLevel(player));
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

    public boolean economyProviderNotFound() {
        return economyManager == null;
    }

    public boolean substractMoney(Player player, double money) {
        if (economyProviderNotFound()) return false;
        if (!economyManager.getEconomy().has(player, money)) return false;
        return economyManager.getEconomy().withdrawPlayer(player, money).transactionSuccess();
    }
}