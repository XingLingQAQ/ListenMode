package me.matsubara.listenmode.packetwrapper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.matsubara.listenmode.util.PluginUtils;
import org.bukkit.ChatColor;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class WrapperPlayServerScoreboardTeam extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Server.SCOREBOARD_TEAM;

    public static final Class<?> ENUM_CHAT_FORMAT = MinecraftReflection.getMinecraftClass("EnumChatFormat");

    // Bytes for pack option data.
    private static final byte ALLOW_FRIENDLY_FIRE = 0x01;
    private static final byte CAN_SEE_FRIENDLY_INVISIBLES = 0x02;

    // For 1.12 or older support.
    private static final boolean isLegacy = !PluginUtils.supports(13);

    // For 1.17+ support.
    private static final boolean isModern = PluginUtils.supports(17);

    private final InternalStructure internalPacket;

    public WrapperPlayServerScoreboardTeam() {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();

        if (isModern) {
            Optional<InternalStructure> info = getHandle().getOptionalStructures().readSafely(0);
            internalPacket = info.orElse(null);
        } else {
            internalPacket = null;
        }
    }

    public WrapperPlayServerScoreboardTeam(PacketContainer packet) {
        super(packet, TYPE);

        if (isModern) {
            Optional<InternalStructure> info = getHandle().getOptionalStructures().readSafely(0);
            internalPacket = info.orElse(null);
        } else {
            internalPacket = null;
        }
    }

    /**
     * Enumeration of all the known packet modes.
     */
    public enum Mode {
        TEAM_CREATED(0),
        TEAM_REMOVED(1),
        TEAM_UPDATED(2),
        PLAYERS_ADDED(3),
        PLAYERS_REMOVED(4);

        private final int mode;

        Mode(int mode) {
            this.mode = mode;
        }

        public static Mode valueOf(int value) {
            for (Mode mode : Mode.values()) {
                if (mode.getMode() == value) return mode;
            }
            return null;
        }

        public int getMode() {
            return mode;
        }
    }

    public enum TeamPush {
        ALWAYS("always"),
        PUSH_OTHER_TEAMS("pushOtherTeams"),
        PUSH_OWN_TEAM("pushOwnTeam"),
        NEVER("never");

        final String collisionRule;

        TeamPush(String collisionRule) {
            this.collisionRule = collisionRule;
        }
    }

    public enum NameTagVisibility {
        ALWAYS("always"),
        HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
        HIDE_FOR_OWN_TEAM("hideForOwnTeam"),
        NEVER("never");

        final String nameTagVisibility;

        NameTagVisibility(String nameTagVisibility) {
            this.nameTagVisibility = nameTagVisibility;
        }
    }

    /**
     * Retrieve a unique name for the team. (Shared with scoreboard).
     *
     * @return The current Team Name
     */
    public String getName() {
        return handle.getStrings().read(0);
    }

    /**
     * Set a unique name for the team. (Shared with scoreboard).
     *
     * @param value - new value.
     */
    public void setName(String value) {
        handle.getStrings().write(0, value);
    }

    /**
     * Retrieve the current packet {@link Mode}.
     * <p>
     * This determines whether team information is added or removed.
     *
     * @return The current packet mode.
     */
    public Mode getPacketMode() {
        return Mode.valueOf(handle.getIntegers().read(isLegacy ? 1 : 0));
    }

    /**
     * Set the current packet {@link Mode}.
     * <p>
     * This determines whether team information is added or removed.
     *
     * @param value - new value.
     */
    public void setPacketMode(Mode value) {
        handle.getIntegers().write(isLegacy ? 1 : 0, value.mode);
    }

    /**
     * Retrieve the team display name.
     * <p>
     * A team must be created or updated.
     *
     * @return The current display name.
     */
    public String getTeamDisplayName() {
        if (!isLegacy) {
            return (isModern ? internalPacket : handle).getChatComponents().read(0).toString();
        } else return handle.getStrings().read(1);
    }

    /**
     * Set the team display name.
     * <p>
     * A team must be created or updated.
     *
     * @param value - new value.
     */
    public void setDisplayName(String value) {
        if (!isLegacy) {
            (isModern ? internalPacket : handle).getChatComponents().write(0, WrappedChatComponent.fromText(value));
        } else handle.getStrings().write(1, value);
    }

    /**
     * Retrieve the team prefix. This will be inserted before the name of each team member.
     * <p>
     * A team must be created or updated.
     *
     * @return The current Team Prefix
     */
    public String getPrefix() {
        if (!isLegacy) {
            return (isModern ? internalPacket : handle).getChatComponents().read(1).toString();
        } else return handle.getStrings().read(2);
    }

    /**
     * Set the team prefix. This will be inserted before the name of each team member.
     * <p>
     * A team must be created or updated.
     *
     * @param value - new value.
     */
    public void setPrefix(String value) {
        if (!isLegacy) {
            (isModern ? internalPacket : handle).getChatComponents().write(1, WrappedChatComponent.fromText(value));
        } else handle.getStrings().write(2, value);
    }

    /**
     * Retrieve the team suffix. This will be inserted after the name of each team member.
     * <p>
     * A team must be created or updated.
     *
     * @return The current Team Suffix
     */
    public String getSuffix() {
        if (!isLegacy) {
            return handle.getChatComponents().read(2).toString();
        } else return handle.getStrings().read(3);
    }

    /**
     * Set the team suffix. This will be inserted after the name of each team member.
     * <p>
     * A team must be created or updated.
     *
     * @param value - new value.
     */
    public void setSuffix(String value) {
        if (!isLegacy) {
            WrappedChatComponent chatComponent = WrappedChatComponent.fromText(value);
            (isModern ? internalPacket : handle).getChatComponents().write(2, chatComponent);
        } else handle.getStrings().write(3, value);
    }

    /**
     * Retrieve whether friendly fire is enabled.
     * <p>
     * A team must be created or updated.
     *
     * @return The current Friendly fire
     */
    public boolean getAllowFriendlyFire() {
        final byte packOptionData = (isModern ? internalPacket : handle).getIntegers().read(isLegacy ? 2 : 0).byteValue();
        final int allowFriendlyFire = packOptionData & ALLOW_FRIENDLY_FIRE;
        return allowFriendlyFire != 0;
    }

    /**
     * Set whether friendly fire is enabled.
     * <p>
     * A team must be created or updated.
     *
     * @param value - new value.
     */
    public void setAllowFriendlyFire(boolean value) {
        int packOptionData = (isModern ? internalPacket : handle).getIntegers().read(isLegacy ? 2 : 0);
        if (value) packOptionData = packOptionData | ALLOW_FRIENDLY_FIRE;
        else packOptionData = packOptionData & ~ALLOW_FRIENDLY_FIRE;
        (isModern ? internalPacket : handle).getIntegers().write(isLegacy ? 2 : 0, packOptionData);
    }

    /**
     * Retrieve whether friendly invisibles can be seen.
     * <p>
     * A team must be created or updated.
     *
     * @return The current Friendly fire
     */
    public boolean getCanSeeFriendlyInvisibles() {
        final byte packOptionData = (isModern ? internalPacket : handle).getIntegers().read(isLegacy ? 2 : 0).byteValue();
        final int canSeeFriendlyInvisibles = packOptionData & CAN_SEE_FRIENDLY_INVISIBLES;
        return canSeeFriendlyInvisibles != 0;
    }

    /**
     * Set whether friendly invisibles can be seen.
     * <p>
     * A team must be created or updated.
     *
     * @param value - new value.
     */
    public void setCanSeeFriendlyInvisibles(boolean value) {
        int packOptionData = (isModern ? internalPacket : handle).getIntegers().read(isLegacy ? 2 : 0);
        if (value) packOptionData = packOptionData | CAN_SEE_FRIENDLY_INVISIBLES;
        else packOptionData = packOptionData & ~CAN_SEE_FRIENDLY_INVISIBLES;
        (isModern ? internalPacket : handle).getIntegers().write(isLegacy ? 2 : 0, packOptionData);
    }

    /**
     * Retrieve the list of entries.
     * <p>
     * Packet mode must be one of the following for this to be valid:
     * <ul>
     *  <li>{@link Mode#TEAM_CREATED}</li>
     *  <li>{@link Mode#PLAYERS_ADDED}</li>
     *  <li>{@link Mode#PLAYERS_REMOVED}</li>
     * </ul>
     *
     * @return A list of entries.
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getEntries() {
        return handle.getSpecificModifier(Collection.class).read(0);
    }

    /**
     * Set the list of entries.
     * <p>
     * Packet mode must be one of the following for this to be valid:
     * <ul>
     *  <li>{@link Mode#TEAM_CREATED}</li>
     *  <li>{@link Mode#PLAYERS_ADDED}</li>
     *  <li>{@link Mode#PLAYERS_REMOVED}</li>
     * </ul>
     *
     * @param entries - A list of entries.
     */
    public void setEntries(Collection<String> entries) {
        handle.getSpecificModifier(Collection.class).write(0, entries);
    }

    /**
     * Retrieve the color of a team
     * <p>
     * A team must be created or updated.
     *
     * @return The current color
     */
    @SuppressWarnings({"unused", "unchecked"})
    public ChatColor getTeamColor() {
        if (!isLegacy) {
            return (isModern ? internalPacket : handle).getEnumModifier(ChatColor.class, ENUM_CHAT_FORMAT).read(0);
        } else {
            int id = handle.getIntegers().read(0);
            try {
                Field byidField = ChatColor.class.getDeclaredField("BY_ID");
                byidField.setAccessible(true);
                Map<Integer, ChatColor> colorsById = (Map<Integer, ChatColor>) byidField.get(null);
                return colorsById.getOrDefault(id, null);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Sets the color of a team.
     * <p>
     * A team must be created or updated.
     *
     * @param value - new value.
     */
    public void setTeamColor(ChatColor value) {
        if (!isLegacy) {
            (isModern ? internalPacket : handle).getEnumModifier(ChatColor.class, ENUM_CHAT_FORMAT).write(0, value);
        } else {
            try {
                Field codeField = ChatColor.class.getDeclaredField("intCode");
                codeField.setAccessible(true);

                int intCode = (int) codeField.get(value);
                handle.getIntegers().write(0, intCode);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
        }
    }

    public NameTagVisibility getNameTagVisibility() {
        return NameTagVisibility.valueOf((isModern ? internalPacket : handle).getStrings().read(isLegacy ? 4 : 0));
    }

    public void setNameTagVisibility(NameTagVisibility value) {
        (isModern ? internalPacket : handle).getStrings().write(isLegacy ? 4 : 0, value.toString());
    }

    public TeamPush getTeamPush() {
        return TeamPush.valueOf((isModern ? internalPacket : handle).getStrings().read(isLegacy ? 5 : 1));
    }

    public void setTeamPush(TeamPush value) {
        (isModern ? internalPacket : handle).getStrings().write(isLegacy ? 5 : 1, value.toString());
    }
}