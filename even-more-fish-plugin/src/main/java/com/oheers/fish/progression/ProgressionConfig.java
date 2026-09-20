package com.oheers.fish.progression;

import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.config.ConfigBase;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Material;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * progression.yml: fishing levels, the skills that can be bought with skill points, and the daily
 * special fish.
 */
public class ProgressionConfig extends ConfigBase {

    /** What a skill does for the player who owns it. */
    public enum Effect { SELL_BONUS, XP_BOOST, SPECIAL_BOOST, FAST_BITE, DOUBLE_CATCH }

    public record Skill(@NonNull String id, @NonNull String name, @NonNull String description,
                        @NonNull Material icon, int maxLevel, int cost, @NonNull Effect effect, double perLevel) {
    }

    private static ProgressionConfig instance;

    private ProgressionConfig() {
        super("progression.yml", "progression.yml", EvenMoreFish.getInstance(), true);
    }

    public static synchronized @NonNull ProgressionConfig getInstance() {
        if (instance == null) {
            instance = new ProgressionConfig();
        }
        return instance;
    }

    public boolean isEnabled() {
        return getConfig().getBoolean("progression.enabled", true);
    }

    /** XP for catching a fish of this rarity. */
    public long xpFor(@NonNull String rarityId) {
        long fallback = getConfig().getLong("progression.xp.default", 5L);
        return Math.max(0, getConfig().getLong("progression.xp.rarities." + rarityId.toLowerCase(Locale.ROOT), fallback));
    }

    public @NonNull LevelCurve curve() {
        return new LevelCurve(
            getConfig().getLong("progression.levels.base-xp", 100L),
            getConfig().getDouble("progression.levels.growth", 1.2D)
        );
    }

    public int skillPointsPerLevel() {
        return Math.max(0, getConfig().getInt("progression.levels.skill-points-per-level", 1));
    }

    public @NonNull String levelUpMessage() {
        return getConfig().getString("progression.level-up.message",
            "<gold>Fishing level up! You are now level <yellow><level></yellow> and have <yellow><points></yellow> skill points.");
    }

    public @NonNull List<Skill> skills() {
        List<Skill> skills = new ArrayList<>();
        Section section = getConfig().getSection("progression.skills");
        if (section == null) {
            return skills;
        }
        for (String id : section.getRoutesAsStrings(false)) {
            Section skill = section.getSection(id);
            if (skill == null) {
                continue;
            }
            Effect effect = parseEffect(skill.getString("effect"));
            if (effect == null) {
                EvenMoreFish.getInstance().getLogger().warning("progression.yml: skill '" + id + "' has an unknown effect, skipping it.");
                continue;
            }
            Material icon = Optional.ofNullable(Material.matchMaterial(skill.getString("icon", "fishing_rod"))).orElse(Material.FISHING_ROD);
            skills.add(new Skill(
                id,
                skill.getString("name", id),
                skill.getString("description", ""),
                icon,
                Math.max(1, skill.getInt("max-level", 5)),
                Math.max(1, skill.getInt("cost", 1)),
                effect,
                skill.getDouble("per-level", 1.0D)
            ));
        }
        return skills;
    }

    public boolean isSpecialFishEnabled() {
        return getConfig().getBoolean("special-fish.enabled", true);
    }

    public double specialFishMultiplier() {
        return Math.max(1.0D, getConfig().getDouble("special-fish.multiplier", 1.5D));
    }

    public @NonNull String message(@NonNull String key) {
        return getConfig().getString("progression.messages." + key, "");
    }

    public @NonNull String doubleCatchMessage() {
        return getConfig().getString("progression.double-catch-message", "");
    }

    public long specialFishResetMinutes() {
        return Math.max(1L, getConfig().getLong("special-fish.reset-minutes", 360L));
    }

    public @NonNull List<String> specialFishExcludedRarities() {
        return getConfig().getStringList("special-fish.excluded-rarities");
    }

    private static Effect parseEffect(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Effect.valueOf(raw.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
