package com.oheers.fish.progression;

/**
 * Turns a running XP total into a level. Reaching level 1 takes {@code base} XP and every further
 * level takes {@code growth} times more than the one before, so early levels come quickly and late
 * ones need real dedication.
 */
public final class LevelCurve {

    private static final int LEVEL_CAP = 1000;

    private final long base;
    private final double growth;

    public LevelCurve(long base, double growth) {
        this.base = Math.max(1, base);
        this.growth = Math.max(1.0, growth);
    }

    /** The XP needed to go from {@code level - 1} up to {@code level}. */
    public long costOf(int level) {
        if (level < 1) {
            return 0;
        }
        return Math.max(1, Math.round(base * Math.pow(growth, level - 1)));
    }

    /** The total XP a player needs to be at exactly this level. */
    public long totalFor(int level) {
        long total = 0;
        for (int i = 1; i <= Math.min(level, LEVEL_CAP); i++) {
            total += costOf(i);
        }
        return total;
    }

    public int levelFor(long xp) {
        long remaining = Math.max(0, xp);
        int level = 0;
        while (level < LEVEL_CAP && remaining >= costOf(level + 1)) {
            remaining -= costOf(level + 1);
            level++;
        }
        return level;
    }

    /** How much of the current level's bar the player has filled. */
    public long xpIntoLevel(long xp) {
        return Math.max(0, xp) - totalFor(levelFor(xp));
    }

    /** How much the current level's bar holds in total, that is what the next level costs. */
    public long xpForNextLevel(long xp) {
        return costOf(levelFor(xp) + 1);
    }
}
