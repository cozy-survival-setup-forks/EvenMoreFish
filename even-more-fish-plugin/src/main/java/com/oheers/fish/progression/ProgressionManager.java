package com.oheers.fish.progression;

import com.oheers.fish.EvenMoreFish;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.Optional;

/**
 * Keeps each player's fishing XP and skill levels. Everything is stored on the player itself, so
 * it needs no database table and survives restarts and database resets.
 */
public final class ProgressionManager {

    public enum BuyResult { BOUGHT, UNKNOWN_SKILL, MAXED, NOT_ENOUGH_POINTS }

    private static final ProgressionManager INSTANCE = new ProgressionManager();

    public static @NonNull ProgressionManager getInstance() {
        return INSTANCE;
    }

    private ProgressionManager() {
    }

    private static NamespacedKey xpKey() {
        return new NamespacedKey(EvenMoreFish.getInstance(), "fishing_xp");
    }

    private static NamespacedKey skillKey(@NonNull String id) {
        return new NamespacedKey(EvenMoreFish.getInstance(), "skill_" + id.toLowerCase(Locale.ROOT));
    }

    public long getXp(@NonNull Player player) {
        return Optional.ofNullable(player.getPersistentDataContainer().get(xpKey(), PersistentDataType.LONG)).orElse(0L);
    }

    public int getLevel(@NonNull Player player) {
        return ProgressionConfig.getInstance().curve().levelFor(getXp(player));
    }

    public int getSkillLevel(@NonNull Player player, @NonNull String skillId) {
        return Optional.ofNullable(player.getPersistentDataContainer().get(skillKey(skillId), PersistentDataType.INTEGER)).orElse(0);
    }

    /** Skill points already spent, from what each skill level cost. */
    public int getSpentPoints(@NonNull Player player) {
        int spent = 0;
        for (ProgressionConfig.Skill skill : ProgressionConfig.getInstance().skills()) {
            spent += getSkillLevel(player, skill.id()) * skill.cost();
        }
        return spent;
    }

    public int getAvailablePoints(@NonNull Player player) {
        int earned = getLevel(player) * ProgressionConfig.getInstance().skillPointsPerLevel();
        return Math.max(0, earned - getSpentPoints(player));
    }

    /** The total effect of the player's skills of one kind, in the units the skill is set up in. */
    public double getBonus(@NonNull Player player, ProgressionConfig.@NonNull Effect effect) {
        double bonus = 0;
        for (ProgressionConfig.Skill skill : ProgressionConfig.getInstance().skills()) {
            if (skill.effect() == effect) {
                bonus += skill.perLevel() * getSkillLevel(player, skill.id());
            }
        }
        return bonus;
    }

    /** Adds XP for a catch, after the player's XP boost, and announces any level gained. */
    public void addXp(@NonNull Player player, long baseXp) {
        ProgressionConfig config = ProgressionConfig.getInstance();
        if (!config.isEnabled() || baseXp <= 0) {
            return;
        }

        long gained = Math.round(baseXp * (1.0D + getBonus(player, ProgressionConfig.Effect.XP_BOOST) / 100.0D));
        long before = getXp(player);
        long after = before + Math.max(1, gained);
        player.getPersistentDataContainer().set(xpKey(), PersistentDataType.LONG, after);

        LevelCurve curve = config.curve();
        int oldLevel = curve.levelFor(before);
        int newLevel = curve.levelFor(after);
        if (newLevel > oldLevel) {
            announceLevelUp(player, newLevel);
        }
    }

    public @NonNull BuyResult buy(@NonNull Player player, @NonNull String skillId) {
        ProgressionConfig.Skill skill = ProgressionConfig.getInstance().skills().stream()
            .filter(candidate -> candidate.id().equalsIgnoreCase(skillId))
            .findFirst()
            .orElse(null);
        if (skill == null) {
            return BuyResult.UNKNOWN_SKILL;
        }
        int current = getSkillLevel(player, skill.id());
        if (current >= skill.maxLevel()) {
            return BuyResult.MAXED;
        }
        if (getAvailablePoints(player) < skill.cost()) {
            return BuyResult.NOT_ENOUGH_POINTS;
        }
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(skillKey(skill.id()), PersistentDataType.INTEGER, current + 1);
        return BuyResult.BOUGHT;
    }

    /** Clears a player's XP and skills. */
    public void reset(@NonNull Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(xpKey());
        ProgressionConfig.getInstance().skills().forEach(skill -> data.remove(skillKey(skill.id())));
    }

    private void announceLevelUp(@NonNull Player player, int level) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(
            ProgressionConfig.getInstance().levelUpMessage(),
            Placeholder.unparsed("level", Integer.toString(level)),
            Placeholder.unparsed("points", Integer.toString(getAvailablePoints(player)))
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }
}
