package com.oheers.fish.progression;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionMathTest {

    private final LevelCurve curve = new LevelCurve(100, 1.2);

    @Test
    void firstLevelCostsTheBase() {
        assertEquals(100, curve.costOf(1));
        assertEquals(120, curve.costOf(2));
    }

    @Test
    void levelBoundariesAreExact() {
        assertEquals(0, curve.levelFor(0));
        assertEquals(0, curve.levelFor(99));
        assertEquals(1, curve.levelFor(100));
        assertEquals(1, curve.levelFor(219));
        assertEquals(2, curve.levelFor(220));
        assertEquals(0, curve.levelFor(-50));
    }

    @Test
    void progressBarMatchesTheLevel() {
        long xp = 150;
        assertEquals(1, curve.levelFor(xp));
        assertEquals(50, curve.xpIntoLevel(xp));
        assertEquals(120, curve.xpForNextLevel(xp));
    }

    @Test
    void totalForRoundTripsThroughLevelFor() {
        for (int level = 0; level < 40; level++) {
            assertEquals(level, curve.levelFor(curve.totalFor(level)));
        }
    }

    @Test
    void nonsenseSettingsStillGiveACurve() {
        LevelCurve odd = new LevelCurve(0, 0.1);
        assertTrue(odd.costOf(5) >= 1);
        assertTrue(odd.levelFor(10) >= 0);
    }

    @Test
    void sameWindowPicksTheSameFish() {
        for (long window = 0; window < 50; window++) {
            assertEquals(SpecialFish.indexFor(window, 17), SpecialFish.indexFor(window, 17));
            int index = SpecialFish.indexFor(window, 17);
            assertTrue(index >= 0 && index < 17);
        }
        assertEquals(-1, SpecialFish.indexFor(3, 0));
    }

    @Test
    void windowsChangeOnTheResetInterval() {
        long hour = TimeUnit.HOURS.toMillis(1);
        assertEquals(SpecialFish.windowOf(0, 60), SpecialFish.windowOf(hour - 1, 60));
        assertEquals(SpecialFish.windowOf(0, 60) + 1, SpecialFish.windowOf(hour, 60));
    }

    @Test
    void minutesLeftIsNeverZero() {
        long hour = TimeUnit.HOURS.toMillis(1);
        assertEquals(60, SpecialFish.minutesLeft(0, 60));
        assertEquals(1, SpecialFish.minutesLeft(hour - 1, 60));
        assertEquals(1, SpecialFish.minutesLeft(hour - 30_000, 60));
    }
}
