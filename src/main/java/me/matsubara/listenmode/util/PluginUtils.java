package me.matsubara.listenmode.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.google.common.base.Preconditions;
import me.matsubara.listenmode.ListenModePlugin;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class PluginUtils {

    private final static Pattern PATTERN = Pattern.compile("&(#[0-9a-fA-F]{6})");

    public static String translate(String message) {
        Preconditions.checkNotNull(message, "Message can't be null.");

        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_16)) {
            return oldTranslate(message);
        }

        Matcher matcher = PATTERN.matcher(oldTranslate(message));
        StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(builder, ChatColor.of(matcher.group(1)).toString());
        }

        return matcher.appendTail(builder).toString();
    }

    @Contract("_ -> param1")
    public static @NotNull List<String> translate(List<String> messages) {
        Preconditions.checkNotNull(messages, "Messages can't be null.");

        messages.replaceAll(PluginUtils::translate);
        return messages;
    }

    @Contract("_ -> new")
    private static @NotNull String oldTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Contract(pure = true)
    public static boolean containsAny(String base, String @NotNull ... strings) {
        for (String string : strings) {
            if (base.contains(string)) return true;
        }
        return false;
    }

    public static @Nullable FileConfiguration reloadConfig(ListenModePlugin plugin, @NotNull File file, @Nullable Consumer<File> error) {
        File backup = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String time = format.format(new Date(System.currentTimeMillis()));

            // When error is null, that means that the file has already regenerated, so we don't need to create a backup.
            if (error != null) {
                backup = new File(file.getParentFile(), file.getName().split("\\.")[0] + "_" + time + ".bak");
                FileUtils.copyFile(file, backup);
            }

            FileConfiguration configuration = new YamlConfiguration();
            configuration.load(file);

            if (backup != null) FileUtils.deleteQuietly(backup);

            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            Logger logger = plugin.getLogger();

            logger.severe("An error occurred while reloading the file {" + file.getName() + "}.");
            if (backup != null
                    && exception instanceof InvalidConfigurationException invalid
                    && invalid.getCause() instanceof ScannerException scanner) {
                handleScannerError(backup, scanner.getProblemMark().getLine());
                logger.severe("The file will be restarted and a copy of the old file will be saved indicating which line had an error.");
            } else {
                logger.severe("The file will be restarted and a copy of the old file will be saved.");
            }

            if (error == null) {
                exception.printStackTrace();
                return null;
            }

            // Only replace file if an exception ocurrs.
            FileUtils.deleteQuietly(file);
            error.accept(file);

            return reloadConfig(plugin, file, null);
        }
    }

    private static void handleScannerError(@NotNull File backup, int line) {
        try {
            Path path = backup.toPath();

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(line, lines.get(line) + " <--------------------< ERROR <--------------------<");

            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static <T extends Enum<T>> T getOrNull(Class<T> clazz, String name) {
        return getOrDefault(clazz, name, null);
    }

    public static <T extends Enum<T>> T getOrDefault(Class<T> clazz, String name, T defaultValue) {
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException exception) {
            return defaultValue;
        }
    }
}