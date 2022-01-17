package me.matsubara.listenmode.util;

import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;

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
        Validate.notNull(message, "Message can't be null.");

        if (MINOR_VERSION < 16) return oldTranslate(message);

        Matcher matcher = PATTERN.matcher(oldTranslate(message));
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1)).toString());
        }

        return matcher.appendTail(buffer).toString();
    }

    public static List<String> translate(List<String> messages) {
        Validate.notNull(messages, "Messages can't be null.");

        messages.replaceAll(PluginUtils::translate);
        return messages;
    }

    private static String oldTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static boolean containsAny(String base, String... strings) {
        for (String string : strings) {
            if (base.contains(string)) return true;
        }
        return false;
    }
}