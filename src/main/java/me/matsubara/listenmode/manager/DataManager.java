package me.matsubara.listenmode.manager;

import com.comphenix.protocol.wrappers.Pair;
import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.data.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public final class DataManager {

    private final ListenModePlugin plugin;
    private final Set<PlayerData> data;

    private File file;
    private FileConfiguration configuration;

    public DataManager(ListenModePlugin plugin) {
        this.plugin = plugin;
        this.data = new HashSet<>();
        load();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void load() {
        // Load file or create it.
        file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        // Load configuration.
        configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            update();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    private void update() {
        data.clear();

        // Load from database instead of YAML.
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        if (databaseManager.isValid()) {
            for (Map.Entry<UUID, Pair<Integer, Boolean>> entry : databaseManager.getData().entrySet()) {
                Pair<Integer, Boolean> pair = entry.getValue();
                data.add(new PlayerData(entry.getKey(), pair.getSecond(), pair.getFirst()));
            }
            return;
        }

        this.data.addAll(getDataFromConfig());
    }

    public @NotNull Set<PlayerData> getDataFromConfig() {
        Set<PlayerData> data = new HashSet<>();

        ConfigurationSection section = configuration.getConfigurationSection("data");
        if (section == null) return data;

        for (String path : section.getKeys(false)) {
            UUID uuid = UUID.fromString(path);
            boolean enabled = configuration.getBoolean("data." + path + ".enabled");
            int level = configuration.getInt("data." + path + ".level");
            data.add(new PlayerData(uuid, enabled, level));
        }

        return data;
    }

    public boolean isEnabled(Player player) {
        PlayerData data = getPlayerData(player);
        if (data != null) return data.isEnabled();
        return true;
    }

    public int getLevel(Player player) {
        PlayerData data = getPlayerData(player);
        if (data != null) return data.getLevel();
        return 1;
    }

    public boolean isLevelUnlocked(Player player, int level) {
        return getLevel(player) >= level;
    }

    public void unlockLevel(Player player, int level) {
        PlayerData data = getPlayerData(player);

        if (data == null) {
            this.data.add(data = new PlayerData(player.getUniqueId(), true, level));
        }

        data.setLevel(level);
        saveData(player);
    }

    public boolean toggleState(Player player) {
        boolean state = !isEnabled(player);
        setEnabled(player, state);
        return state;
    }

    public void setEnabled(Player player, boolean enabled) {
        PlayerData data = getPlayerData(player);

        if (data == null) {
            this.data.add(data = new PlayerData(player.getUniqueId(), enabled, 1));
        }

        data.setEnabled(enabled);
        saveData(player);
    }

    private @Nullable PlayerData getPlayerData(Player player) {
        for (PlayerData data : data) {
            if (data.getUuid().equals(player.getUniqueId())) return data;
        }
        return null;
    }

    private void saveData(@NotNull Player player) {
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        if (databaseManager.isValid()) {
            databaseManager.setEnabled(player, isEnabled(player));
            databaseManager.setLevel(player, getLevel(player));
            return;
        }

        configuration.set("data." + player.getUniqueId() + ".enabled", isEnabled(player));
        configuration.set("data." + player.getUniqueId() + ".level", getLevel(player));
        saveConfig();
    }

    public void reloadConfig() {
        try {
            configuration = new YamlConfiguration();
            configuration.load(file);
            update();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    private void saveConfig() {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}