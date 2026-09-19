package com.oheers.fish.progression;

import com.oheers.fish.api.fishing.items.IFish;
import com.oheers.fish.api.fishing.items.IRarity;
import com.oheers.fish.fishing.items.FishManager;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * One fish at a time is the special fish and sells for a bonus. It is worked out from the clock
 * alone, so every server restart, and every server on a network, agrees on which fish it is and
 * when it changes, without anything being saved.
 */
public final class SpecialFish {

    private SpecialFish() {
    }

    /** The index of the fish for a time window. The same window always gives the same answer. */
    static int indexFor(long window, int candidates) {
        if (candidates <= 0) {
            return -1;
        }
        return new Random(window * 0x9E3779B97F4A7C15L + 0x51ED27L).nextInt(candidates);
    }

    static long windowOf(long nowMillis, long resetMinutes) {
        return Math.floorDiv(nowMillis, TimeUnit.MINUTES.toMillis(resetMinutes));
    }

    static long minutesLeft(long nowMillis, long resetMinutes) {
        long length = TimeUnit.MINUTES.toMillis(resetMinutes);
        long remaining = length - Math.floorMod(nowMillis, length);
        return Math.max(1, (remaining + 59_999) / 60_000);
    }

    public static boolean isEnabled() {
        ProgressionConfig config = ProgressionConfig.getInstance();
        return config.isEnabled() && config.isSpecialFishEnabled();
    }

    /** The fish that is special right now, or null if there are no fish to choose from. */
    public static @Nullable IFish current() {
        if (!isEnabled()) {
            return null;
        }
        List<IFish> candidates = candidates();
        int index = indexFor(windowOf(System.currentTimeMillis(), ProgressionConfig.getInstance().specialFishResetMinutes()), candidates.size());
        return index < 0 ? null : candidates.get(index);
    }

    public static long minutesUntilReset() {
        return minutesLeft(System.currentTimeMillis(), ProgressionConfig.getInstance().specialFishResetMinutes());
    }

    public static boolean isSpecial(@NonNull IFish fish) {
        IFish special = current();
        return special != null
            && special.getId().equals(fish.getId())
            && special.getRarity().getId().equals(fish.getRarity().getId());
    }

    /** The multiplier a player gets on the special fish, including their Lucky Angler style skills. */
    public static double multiplierFor(@Nullable Player player) {
        double multiplier = ProgressionConfig.getInstance().specialFishMultiplier();
        if (player != null) {
            multiplier += ProgressionManager.getInstance().getBonus(player, ProgressionConfig.Effect.SPECIAL_BOOST);
        }
        return multiplier;
    }

    private static List<IFish> candidates() {
        List<String> excluded = ProgressionConfig.getInstance().specialFishExcludedRarities().stream()
            .map(String::toLowerCase)
            .toList();
        List<IFish> fish = new ArrayList<>();
        // The rarity map is sorted by name and fish keep their file order, so the list is stable.
        for (IRarity rarity : FishManager.getInstance().getRarityMap().values()) {
            if (rarity.isDisabled() || excluded.contains(rarity.getId().toLowerCase())) {
                continue;
            }
            for (IFish candidate : rarity.getOriginalFishList()) {
                if (candidate.getShowInJournal()) {
                    fish.add(candidate);
                }
            }
        }
        return fish;
    }
}
