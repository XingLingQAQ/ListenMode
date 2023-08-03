package me.matsubara.listenmode.util;

import com.google.common.base.Preconditions;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class PluginUtils {

    private final static String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    public final static int MINOR_VERSION = Integer.parseInt(VERSION.substring(1).split("_")[1]);

    private final static Pattern PATTERN = Pattern.compile("&(#[0-9a-fA-F]{6})");

    public static boolean supports(int version) {
        return MINOR_VERSION >= version;
    }

    public static String translate(String message) {
        Preconditions.checkNotNull(message, "Message can't be null.");

        if (MINOR_VERSION < 16) return oldTranslate(message);

        Matcher matcher = PATTERN.matcher(oldTranslate(message));
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1)).toString());
        }

        return matcher.appendTail(buffer).toString();
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
}