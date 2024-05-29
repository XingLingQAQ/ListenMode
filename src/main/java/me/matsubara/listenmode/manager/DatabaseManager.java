package me.matsubara.listenmode.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import me.matsubara.listenmode.ListenModePlugin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Getter
public class DatabaseManager {

    private final @Getter(value = AccessLevel.NONE) ListenModePlugin plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final DataSource source;
    private boolean issues;

    public DatabaseManager(@NotNull ListenModePlugin plugin, String host, int port, String database, String username, String password, boolean ssl) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.ssl = ssl;

        if (plugin.getConfig().getString("storage.type", "FLAT").equals("MYSQL")) {
            this.source = setupSource();
            if (this.source == null) return;

            if (!isConnected()) {
                plugin.getLogger().severe("Couldn't create connection to database! disabling...");
                issues = true;
                return;
            }

            if (!setupTable()) {
                issues = true;
                plugin.getLogger().severe("Couldn't create table! disabling...");
            }

            return;
        }

        this.source = null;
    }

    public boolean isValid() {
        return source != null && !issues;
    }

    private @Nullable DataSource setupSource() {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("useSSL", ssl);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("maintainTimeStats", false);
        config.setMaximumPoolSize(8);

        config.validate();

        try {
            return new HikariDataSource(config);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean setupTable() {
        String query = "CREATE TABLE IF NOT EXISTS player_upgrades (uuid CHAR(36) NOT NULL, level INT(10) DEFAULT 1 NOT NULL, enabled TINYINT(1) DEFAULT 1 NOT NULL, PRIMARY KEY (uuid))";
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.execute();
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private boolean isConnected() {
        try (Connection connection = source.getConnection()) {
            return connection.isValid(1000);
        } catch (SQLException exception) {
            return false;
        }
    }

    public Map<UUID, Map.Entry<Integer, Boolean>> getData() {
        String query = "SELECT uuid, level, enabled FROM player_upgrades";
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            Map<UUID, Map.Entry<Integer, Boolean>> data = new HashMap<>();

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                data.put(UUID.fromString(result.getString("uuid")), new AbstractMap.SimpleEntry<>(result.getInt("level"), result.getInt("enabled") != 0));
            }

            return data;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return Collections.emptyMap();
    }

    public void setLevel(@NotNull Player player, int level) {
        setLevel(player.getUniqueId(), level);
    }

    public void setLevel(@NotNull UUID uuid, int level) {
        String query = "UPDATE player_upgrades SET level = ? WHERE uuid = ?";
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, level);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void setEnabled(@NotNull Player player, boolean enabled) {
        setEnabled(player.getUniqueId(), enabled);
    }

    public void setEnabled(@NotNull UUID uuid, boolean enabled) {
        String query = "UPDATE player_upgrades SET enabled = ? WHERE uuid = ?";
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public boolean existsPlayer(@NotNull Player player) {
        String query = "SELECT uuid FROM player_upgrades WHERE uuid = ? LIMIT 1";
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());
            return statement.executeQuery().next();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public void createData(Player player) {
        if (existsPlayer(player)) return;

        String query = "INSERT INTO player_upgrades (uuid, level, enabled) VALUES (?, ?, ?)";
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setInt(2, 1);
            statement.setInt(3, 1);
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}