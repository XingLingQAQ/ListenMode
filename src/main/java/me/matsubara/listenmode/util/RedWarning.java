package me.matsubara.listenmode.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.matsubara.listenmode.packetwrapper.WrapperPlayServerWorldBorder;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

public final class RedWarning {

    public static void warning(Player player, boolean warning) {
        // Get default border.
        WorldBorder border = player.getWorld().getWorldBorder();

        // Since 1.17 is more simple to senda a red warning effect.
        if (PluginUtils.supports(17)) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE);
            packet.getModifier().writeDefaults();

            packet.getIntegers().write(0, (int) (warning ? border.getSize() : border.getWarningDistance()));

            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
            return;
        }

        WrapperPlayServerWorldBorder wrapperBorder = new WrapperPlayServerWorldBorder();
        wrapperBorder.setAction(EnumWrappers.WorldBorderAction.INITIALIZE);

        // Default teleport boundary.
        wrapperBorder.setPortalTeleportBoundary(29999984);
        // Set center.
        wrapperBorder.setCenterX(player.getLocation().getX());
        wrapperBorder.setCenterZ(player.getLocation().getZ());
        // Default size.
        wrapperBorder.setOldRadius(border.getSize());
        wrapperBorder.setRadius(border.getSize());
        // We set size of the border for warning, otherwise the default warning distance.
        wrapperBorder.setWarningDistance((int) (warning ? border.getSize() : border.getWarningDistance()));
        // Warning time doesn't affect anything.
        wrapperBorder.setWarningTime(0);
        // Same as before.
        wrapperBorder.setSpeed(0L);

        wrapperBorder.sendPacket(player);
    }
}