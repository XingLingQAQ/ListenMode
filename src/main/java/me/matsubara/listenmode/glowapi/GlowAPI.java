package me.matsubara.listenmode.glowapi;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.matsubara.listenmode.data.GlowData;
import me.matsubara.listenmode.packetwrapper.WrapperPlayServerEntityMetadata;
import me.matsubara.listenmode.packetwrapper.WrapperPlayServerScoreboardTeam;
import me.matsubara.listenmode.packetwrapper.WrapperPlayServerScoreboardTeam.Mode;
import me.matsubara.listenmode.packetwrapper.WrapperPlayServerScoreboardTeam.NameTagVisibility;
import me.matsubara.listenmode.packetwrapper.WrapperPlayServerScoreboardTeam.TeamPush;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class GlowAPI {

    public static final byte ENTITY_GLOWING_EFFECT = (byte) 0x40;
    private static final NameTagVisibility DEFAULT_NAME_TAG_VISIBILITY = NameTagVisibility.ALWAYS;
    private static final TeamPush DEFAULT_TEAM_PUSH = TeamPush.ALWAYS;

    private static final Map<UUID, GlowData> DATA_MAP = new ConcurrentHashMap<>();

    /**
     * Team Colors
     */
    public enum Color {
        BLACK(ChatColor.BLACK),
        DARK_BLUE(ChatColor.DARK_BLUE),
        DARK_GREEN(ChatColor.DARK_GREEN),
        DARK_AQUA(ChatColor.DARK_AQUA),
        DARK_RED(ChatColor.DARK_RED),
        DARK_PURPLE(ChatColor.DARK_PURPLE),
        GOLD(ChatColor.GOLD),
        GRAY(ChatColor.GRAY),
        DARK_GRAY(ChatColor.DARK_GRAY),
        BLUE(ChatColor.BLUE),
        GREEN(ChatColor.GREEN),
        AQUA(ChatColor.AQUA),
        RED(ChatColor.RED),
        @Deprecated PURPLE(ChatColor.LIGHT_PURPLE),
        LIGHT_PURPLE(ChatColor.LIGHT_PURPLE),
        YELLOW(ChatColor.YELLOW),
        WHITE(ChatColor.WHITE),
        NONE(ChatColor.RESET);

        private final ChatColor chatColor;

        Color(ChatColor chatColor) {
            this.chatColor = chatColor;
        }

        public String getTeamName() {
            String name = "GAPI#" + name();
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            return name;
        }

        public ChatColor getColor() {
            return chatColor;
        }
    }

    public static GlowAPI.Color getGlowColor(Entity entity, Player player) {
        UUID entityUniqueId = entity.getUniqueId();
        GlowData data = DATA_MAP.get(entityUniqueId);
        if (data == null) return null;
        return data.colorMap.get(player.getUniqueId());
    }

    public static void initTeams(Player player) {
        initTeamsAsync(player, DEFAULT_NAME_TAG_VISIBILITY, DEFAULT_TEAM_PUSH);
    }

    public static void initTeamsAsync(Player player, NameTagVisibility tagVisibility, TeamPush push) {
        for (GlowAPI.Color color : GlowAPI.Color.values()) {
            GlowAPI.sendTeamCreatedPacket(color, tagVisibility, push, player);
        }
    }

    public static void sendTeamCreatedPacket(GlowAPI.Color color, NameTagVisibility tagVisibility, TeamPush push, Player player) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
        WrapperPlayServerScoreboardTeam scoreboardTeamWrapper = new WrapperPlayServerScoreboardTeam(packet);

        String teamName = color.getTeamName();

        scoreboardTeamWrapper.setPacketMode(Mode.TEAM_CREATED);
        scoreboardTeamWrapper.setName(teamName);
        scoreboardTeamWrapper.setNameTagVisibility(tagVisibility);
        scoreboardTeamWrapper.setTeamPush(push);
        scoreboardTeamWrapper.setTeamColor(color.getColor());
        scoreboardTeamWrapper.setPrefix(color.getColor().toString());
        scoreboardTeamWrapper.setDisplayName(teamName);
        scoreboardTeamWrapper.setSuffix("");
        scoreboardTeamWrapper.setAllowFriendlyFire(true);
        scoreboardTeamWrapper.setCanSeeFriendlyInvisibles(false);
        scoreboardTeamWrapper.sendPacket(player);
    }

    public static boolean isGlowing(Entity entity, Player player) {
        return getGlowColor(entity, player) != null;
    }

    public static boolean isGlowing(Entity entity, Collection<? extends Player> players, boolean checkAll) {
        if (checkAll) {
            boolean glowing = true;
            for (Player receiver : players) {
                if (!isGlowing(entity, receiver)) {
                    glowing = false;
                }
            }
            return glowing;
        } else {
            for (Player receiver : players) {
                if (isGlowing(entity, receiver)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void setGlowing(Entity entity, GlowAPI.Color color, NameTagVisibility tagVisibility, TeamPush push, Player player) {
        setGlowingAsync(entity, color, tagVisibility, push, player).join();
    }

    public static void setGlowing(Entity entity, GlowAPI.Color color, Player player) {
        setGlowingAsync(entity, color, player).join();
    }

    public static void setGlowing(Entity entity, boolean glowing, Player player) {
        setGlowingAsync(entity, glowing, player).join();
    }

    public static void setGlowing(Entity entity, boolean glowing, Collection<? extends Player> players) {
        setGlowingAsync(entity, glowing, players).join();
    }

    public static void setGlowing(Entity entity, GlowAPI.Color color, Collection<? extends Player> players) {
        setGlowingAsync(entity, color, players).join();
    }

    public static void setGlowing(Collection<? extends Entity> entities, GlowAPI.Color color, Player player) {
        setGlowingAsync(entities, color, player).join();
    }

    public static void setGlowing(Collection<? extends Entity> entities, GlowAPI.Color color, Collection<? extends Player> players) {
        setGlowingAsync(entities, color, players).join();
    }

    public static CompletableFuture<Void> setGlowingAsync(Entity entity, GlowAPI.Color color, NameTagVisibility tagVisibility, TeamPush push, Player player) {
        Collection<Entity> entities = Collections.singletonList(entity);
        return setGlowingAsync(entities, color, tagVisibility, push, player);
    }

    public static CompletableFuture<Void> setGlowingAsync(Entity entity, GlowAPI.Color color, Player player) {
        Collection<Entity> entities = Collections.singletonList(entity);
        return setGlowingAsync(entities, color, DEFAULT_NAME_TAG_VISIBILITY, DEFAULT_TEAM_PUSH, player);
    }


    public static CompletableFuture<Void> setGlowingAsync(Entity entity, boolean glowing, Player player) {
        return setGlowingAsync(entity, glowing ? GlowAPI.Color.NONE : null, player);
    }

    public static CompletableFuture<Void> setGlowingAsync(Entity entity, boolean glowing, Collection<? extends Player> players) {
        return CompletableFuture.allOf(players
                .parallelStream()
                .map(player -> setGlowingAsync(entity, glowing, player))
                .toArray(CompletableFuture[]::new));
    }

    public static CompletableFuture<Void> setGlowingAsync(Entity entity, GlowAPI.Color color, Collection<? extends Player> players) {
        return CompletableFuture.allOf(players
                .parallelStream()
                .map(player -> setGlowingAsync(entity, color, player))
                .toArray(CompletableFuture[]::new));
    }

    public static CompletableFuture<Void> setGlowingAsync(Collection<? extends Entity> entities, GlowAPI.Color color, Player player) {
        return setGlowingAsync(entities, color, DEFAULT_NAME_TAG_VISIBILITY, DEFAULT_TEAM_PUSH, player);
    }

    public static CompletableFuture<Void> setGlowingAsync(Collection<? extends Entity> entities, GlowAPI.Color color, Collection<? extends Player> players) {
        return CompletableFuture.allOf(players
                .parallelStream()
                .map(player -> setGlowingAsync(entities, color, player))
                .toArray(CompletableFuture[]::new));
    }

    public static CompletableFuture<Void> setGlowingAsync(Collection<? extends Entity> entities, Color color, NameTagVisibility nameTagVisibility, TeamPush teamPush, Player player) {
        Map<Color, Collection<Entity>> removeFromTeam = new ConcurrentHashMap<>();
        Collection<Entity> addToTeam = ConcurrentHashMap.newKeySet();

        CompletableFuture<Void> future = CompletableFuture.allOf(entities
                .parallelStream()
                .map(entity -> {
                    boolean glowing = color != null;
                    if (entity == null) glowing = false;
                    if (entity instanceof OfflinePlayer) {
                        if (!((OfflinePlayer) entity).isOnline()) glowing = false;
                    }

                    UUID entityUniqueId = null;
                    if (entity != null) entityUniqueId = entity.getUniqueId();

                    boolean wasGlowing = DATA_MAP.containsKey(entityUniqueId);

                    GlowData glowData;
                    if (wasGlowing && entity != null) glowData = DATA_MAP.get(entityUniqueId);
                    else glowData = new GlowData();

                    UUID playerUniqueId = player.getUniqueId();

                    Color oldColor = wasGlowing ? glowData.colorMap.get(playerUniqueId) : null;

                    if (glowing) glowData.colorMap.put(playerUniqueId, color);
                    else glowData.colorMap.remove(playerUniqueId);

                    if (glowData.colorMap.isEmpty()) DATA_MAP.remove(entityUniqueId);
                    else if (entity != null) DATA_MAP.put(entityUniqueId, glowData);

                    if (color != null && oldColor == color) return null;
                    if (entity == null) return null;
                    if (entity instanceof OfflinePlayer) {
                        if (!((OfflinePlayer) entity).isOnline()) return null;
                    }
                    if (!player.isOnline()) return null;

                    if (glowing) addToTeam.add(entity);

                    if (oldColor != null) {
                        if (oldColor != Color.NONE) {
                            if (!removeFromTeam.containsKey(oldColor)) {
                                removeFromTeam.putIfAbsent(oldColor, ConcurrentHashMap.newKeySet());
                            }
                            Collection<Entity> teamEntities = removeFromTeam.get(oldColor);
                            teamEntities.add(entity);
                        }
                    }

                    if (glowing) {
                        addToTeam.add(entity);
                    }

                    return GlowAPI.sendGlowPacketAsync(entity, glowing, player);
                })
                .filter(Objects::nonNull)
                .toArray(CompletableFuture[]::new));

        future.thenRun(() -> removeFromTeam
                .entrySet()
                .parallelStream()
                .forEach(entry -> future.thenRun(() -> {
                    Collection<Entity> removeEntities = entry.getValue();
                    Color removeColor = entry.getKey();
                    GlowAPI.sendTeamPacketAsync(removeEntities, removeColor, Mode.PLAYERS_REMOVED,
                            nameTagVisibility, teamPush, player).join();
                })));

        future.thenRun(() -> {
            if (color != null && !addToTeam.isEmpty()) {
                Mode packetMode = (color != Color.NONE) ? Mode.PLAYERS_ADDED : Mode.PLAYERS_REMOVED;
                future.thenRun(() -> GlowAPI.sendTeamPacketAsync(addToTeam, color, packetMode,
                        nameTagVisibility, teamPush, player).join());
            }
        });

        return future;
    }

    private static CompletableFuture<Void> sendGlowPacketAsync(Entity entity, boolean glowing, Player player) {
        return CompletableFuture.runAsync(() -> {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            WrapperPlayServerEntityMetadata wrappedPacket = new WrapperPlayServerEntityMetadata(packet);
            WrappedDataWatcherObject dataWatcherObject = new WrappedDataWatcherObject(0, Registry.get(Byte.class));

            int invertedEntityId = -entity.getEntityId();

            WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(entity);
            List<WrappedWatchableObject> dataWatcherObjects = dataWatcher.getWatchableObjects();

            byte entityByte = 0x00;
            if (!dataWatcherObjects.isEmpty()) entityByte = (byte) dataWatcherObjects.get(0).getValue();
            if (glowing) entityByte = (byte) (entityByte | GlowAPI.ENTITY_GLOWING_EFFECT);
            else entityByte = (byte) (entityByte & ~GlowAPI.ENTITY_GLOWING_EFFECT);

            WrappedWatchableObject wrappedMetadata = new WrappedWatchableObject(dataWatcherObject, entityByte);
            List<WrappedWatchableObject> metadata = Collections.singletonList(wrappedMetadata);

            wrappedPacket.setEntityId(invertedEntityId);
            wrappedPacket.setMetadata(metadata);

            wrappedPacket.sendPacket(player);
        });
    }

    public static CompletableFuture<Void> sendTeamPacketAsync(Collection<? extends Entity> entities, GlowAPI.Color color, Mode packetMode, NameTagVisibility nameTagVisibility, TeamPush teamPush, Player player) {
        return CompletableFuture.runAsync(() -> {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
            WrapperPlayServerScoreboardTeam wrappedPacket = new WrapperPlayServerScoreboardTeam(packet);
            wrappedPacket.setNameTagVisibility(nameTagVisibility);
            wrappedPacket.setPacketMode(packetMode);
            wrappedPacket.setTeamPush(teamPush);

            String teamName = color.getTeamName();
            wrappedPacket.setName(teamName);

            Collection<String> entries = wrappedPacket.getEntries();
            entities
                    .parallelStream()
                    .map(entity -> {
                        if (entity instanceof OfflinePlayer) return entity.getName();
                        else return entity.getUniqueId().toString();
                    })
                    .forEach(entries::add);

            wrappedPacket.sendPacket(player);
        });
    }
}