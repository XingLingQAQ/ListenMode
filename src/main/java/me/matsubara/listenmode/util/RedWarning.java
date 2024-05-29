package me.matsubara.listenmode.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayWorldBorderWarningReach;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RedWarning {

    public static void warning(@NotNull Player player, boolean warning) {
        // Get default border.
        WorldBorder border = player.getWorld().getWorldBorder();

        double size = border.getSize();
        int warningBlocks = (int) (warning ? size : border.getWarningDistance());
        Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
        ProtocolManager manager = PacketEvents.getAPI().getProtocolManager();

        // Since 1.17 is more simple to send a red warning effect.
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            WrapperPlayWorldBorderWarningReach warningReach = new WrapperPlayWorldBorderWarningReach(warningBlocks);
            manager.sendPacket(channel, warningReach);
            return;
        }

        WrapperPlayServerWorldBorder wrapperBorder = new WrapperPlayServerWorldBorder(
                player.getLocation().getX(),
                player.getLocation().getZ(),
                size,
                size,
                0L,
                29999984,
                0,
                warningBlocks);

        manager.sendPacket(channel, wrapperBorder);
    }
}