package me.matsubara.listenmode.command;

import com.google.common.collect.ImmutableList;
import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.gui.LevelGUI;
import me.matsubara.listenmode.runnable.ListenTask;
import me.matsubara.listenmode.util.PluginUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final ListenModePlugin plugin;

    private static final List<String> ARGS = ImmutableList.of("reload", "toggle", "upgrades");
    private static final List<String> HELP = Stream.of(
                    "&8----------------------------------------",
                    "&6&lListenMode &f&oCommands &c(optional) <required>",
                    "&e/lm reload &f- &7Reload configuration files.",
                    "&e/lm toggle &f- &7Toggle the state of the ability.",
                    "&e/lm upgrades &f- &7Open upgrades GUI.",
                    "&8----------------------------------------")
            .map(PluginUtils::translate)
            .toList();

    public MainCommand(ListenModePlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!command.getName().equals("listenmode")) return true;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(PluginUtils.translate(plugin.getConfig().getString("messages.from-console")));
            return true;
        }

        if (args.length != 1 || !ARGS.contains(args[0].toLowerCase())) {
            HELP.forEach(player::sendMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("listenmode.reload")) {
                player.sendMessage(PluginUtils.translate(plugin.getConfig().getString("messages.no-permission")));
                return true;
            }

            CompletableFuture.runAsync(plugin::updateMainConfig).thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getTasks().forEach(ListenTask::cancel);
                plugin.getTasks().clear();

                plugin.reloadConfig();
                plugin.getDataManager().reloadConfig();

                plugin.loadData();
                player.sendMessage(PluginUtils.translate(plugin.getConfig().getString("messages.reload")));
            }));
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            player.sendMessage(PluginUtils.translate(plugin.getConfig().getString("messages." + (plugin.getDataManager().toggleState(player) ? "enabled" : "disabled"))));
            return true;
        }

        if (args[0].equalsIgnoreCase("upgrades")) {
            if (plugin.economyProviderNotFound()) {
                sender.sendMessage(PluginUtils.translate(plugin.getConfig().getString("messages.feature-disabled")));
                return true;
            }
            new LevelGUI(plugin, player, plugin.getDataManager().getLevel(player));
            return true;
        }

        player.sendMessage(PluginUtils.translate(plugin.getConfig().getString("messages.invalid")));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equals("listenmode")) return null;
        if (args.length == 1) return StringUtil.copyPartialMatches(args[0], ARGS, new ArrayList<>());
        return Collections.emptyList();
    }
}