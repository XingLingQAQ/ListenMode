package me.matsubara.listenmode.runnable;

import fr.skytasul.glowingentities.GlowingEntities;
import lombok.Getter;
import me.matsubara.listenmode.ListenModePlugin;
import me.matsubara.listenmode.data.EntityData;
import me.matsubara.listenmode.util.PluginUtils;
import me.matsubara.listenmode.util.RedWarning;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class ListenTask extends BukkitRunnable {

    // Instance of the plugin.
    private final ListenModePlugin plugin;
    private final GlowingEntities glowingEntities;

    // Instance of the player who's using this ability.
    private final @Getter Player player;

    // Default listen radius.
    private final double defaultRadius;

    // Map containing the respective team of a player (if any).
    private final Map<String, Team> teams;

    // Entities with glowing.
    private final Set<Entity> glowing = new HashSet<>();

    // The breathing sound.
    private final Sound sound;

    // Heart-beat effect related.
    private int beats = 0;

    private static final int BEAT_FREQUENCE = 32;

    public ListenTask(@NotNull ListenModePlugin plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.glowingEntities = plugin.getGlowingEntities();
        this.player = player;
        this.defaultRadius = plugin.getLevelRange(player);

        if (!plugin.isSoundOnly()) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }

        this.teams = new HashMap<>();

        this.sound = Sound.valueOf(plugin.getHeartBeatSound());

        runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void run() {
        if (!player.isSneaking()) {
            cancel();
            return;
        }

        double maxRadius = plugin.getMaximumRadius() + 5.0d;

        for (Entity entity : player.getNearbyEntities(maxRadius, maxRadius, maxRadius)) {
            if (!appliesTo(entity)) continue;

            EntityData data = plugin.getDataByType(entity.getType());

            // Out of range; remove the glow for near entities (if any).
            double distance = player.getLocation().distance(entity.getLocation());
            double radius = data.radius() != null ? data.radius() : defaultRadius;
            if (distance > radius) {
                removeGlowing(entity, player);
                continue;
            }

            // Already glowing.
            if (glowing.contains(entity)) continue;

            // Handle tamed.
            if (isTamedBy(entity, player)) {
                try {
                    glowingEntities.setGlowing(entity, player, plugin.getDefaultColor("tamed"));
                    glowing.add(entity);
                } catch (ReflectiveOperationException exception) {
                    exception.printStackTrace();
                }
                continue;
            }

            // Handle players (they may be on a real team).
            if (entity instanceof Player temp) {
                Team team = getPlayerTeam(player.getScoreboard(), temp);
                if (team != null) teams.put(temp.getName(), team);
            }

            ChatColor color = data.color();
            if (color == null) color = getByType(entity);

            try {
                glowingEntities.setGlowing(entity, player, color);
                glowing.add(entity);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
        }

        // Play red warning effect if enabled.
        if (plugin.isRedWarningEnabled()) RedWarning.warning(player, true);

        // Play heart-beat effect if enabled.
        if (plugin.isHeartBeatEnabled()) playHeartBeat();
    }

    @Override
    public void cancel() {
        super.cancel();
        stop();
        plugin.getTasks().remove(this);
    }

    public void stop() {
        removeGlowing();

        // Remove red warning effect if enabled.
        if (plugin.isRedWarningEnabled()) RedWarning.warning(player, false);
    }

    private ChatColor getByType(Entity entity) {
        if (isBoss(entity)) {
            return plugin.getDefaultColor("boss");
        } else if (isMonster(entity)) {
            return plugin.getDefaultColor("monster");
        }
        return plugin.getDefaultColor("passive");
    }

    private boolean isMonster(Entity entity) {
        return entity instanceof Monster || PluginUtils.containsAny(entity.getType().name(), "GHAST", "PHANTOM");
    }

    private boolean isBoss(@NotNull Entity entity) {
        return entity.getType() == EntityType.ENDER_DRAGON || entity.getType() == EntityType.WITHER;
    }

    private @Nullable Team getPlayerTeam(@NotNull Scoreboard board, Player player) {
        for (Team team : board.getTeams()) {
            if (team.getEntries().contains(player.getName())) return team;
        }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    private boolean isTamedBy(Entity entity, Player owner) {
        if (!(entity instanceof Tameable tameable)) return false;
        AnimalTamer tamer = tameable.getOwner();
        return tameable.isTamed() && tamer != null && tamer.getUniqueId().equals(owner.getUniqueId());
    }

    public void removeGlowing() {
        for (Entity entity : glowing) {
            try {
                glowingEntities.unsetGlowing(entity, player);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
            // Update previous team status.
            if (entity instanceof Player) {
                updateTeam((Player) entity);
            }
        }
        glowing.clear();
    }

    public void removeGlowing(Entity entity, Player player) {
        if (!glowing.contains(entity)) return;

        // Remove glowing.
        try {
            glowingEntities.unsetGlowing(entity, player);
            glowing.remove(entity);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }

        // Update previous team status.
        if (entity instanceof Player) {
            updateTeam((Player) entity);
        }
    }

    private void updateTeam(@NotNull Player player) {
        String name = player.getName();

        Team team = teams.get(name);
        if (team == null) return;

        // Re-add to the team.
        team.removeEntry(name);
        team.addEntry(name);
    }

    private boolean appliesTo(@NotNull Entity entity) {
        if (entity.hasMetadata("RemoveGlow")) return false;

        // We won't add the glow of an entity who's already glowing for everyone due to potion effect.
        if (entity instanceof LivingEntity living && living.hasPotionEffect(PotionEffectType.GLOWING)) {
            return false;
        }

        boolean projectiles = !(entity instanceof Projectile) || !plugin.ignoreProjectiles();
        return !plugin.getIgnoredEntities().contains(entity.getType().name()) && projectiles;
    }

    private void playHeartBeat() {
        beats++;

        if (beats < BEAT_FREQUENCE) return;

        if (beats == BEAT_FREQUENCE) {
            playHeartBeat(player, true);
            return;
        }

        if (beats > (BEAT_FREQUENCE + BEAT_FREQUENCE / 3)) {
            playHeartBeat(player, false);
            beats = 0;
        }
    }

    private void playHeartBeat(Player player, boolean breathingEffect) {
        // Play breathing effect.
        if (breathingEffect && !plugin.isSoundOnly()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 0, false, false));
        }

        // Play breathing sound.
        if (sound == null) return;

        // Random pitch to make the effect more realistic.
        float pitch = (float) ThreadLocalRandom.current().nextDouble(-1.0d, 1.0d);

        if (plugin.isSoundGlobal()) {
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, pitch);
        } else {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        }
    }
}