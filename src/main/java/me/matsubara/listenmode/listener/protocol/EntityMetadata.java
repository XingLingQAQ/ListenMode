package me.matsubara.listenmode.listener.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.matsubara.listenmode.glowapi.GlowAPI;
import me.matsubara.listenmode.packetwrapper.WrapperPlayServerEntityMetadata;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class EntityMetadata extends PacketAdapter {

    public EntityMetadata(Plugin plugin) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_METADATA);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(packet);

        int entityId = metadataPacket.getEntityId();
        if (entityId < 0) {
            metadataPacket.setEntityId(-entityId);
            return;
        }

        final List<WrappedWatchableObject> metaData = metadataPacket.getMetadata();
        if (metaData == null || metaData.isEmpty()) return;

        Player player = event.getPlayer();

        Entity entity = null;
        for (Entity worldEntity : player.getWorld().getEntities()) {
            if (worldEntity.getEntityId() == entityId) entity = worldEntity;
        }

        if (entity == null) return;

        // Check if the entity is glowing.
        if (!GlowAPI.isGlowing(entity, player)) return;

        // Update the DataWatcher Item.
        final WrappedWatchableObject wrappedEntityObj = metaData.get(0);
        final Object entityObj = wrappedEntityObj.getValue();
        if (!(entityObj instanceof Byte)) return;
        byte entityByte = (byte) entityObj;
        entityByte = (byte) (entityByte | GlowAPI.ENTITY_GLOWING_EFFECT);
        wrappedEntityObj.setValue(entityByte);
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist
                .newBuilder()
                .types(PacketType.Play.Server.ENTITY_METADATA)
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
}