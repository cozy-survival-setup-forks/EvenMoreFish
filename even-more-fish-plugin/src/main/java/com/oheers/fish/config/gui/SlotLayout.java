package com.oheers.fish.config.gui;

import com.oheers.fish.FishUtils;
import com.oheers.fish.config.GuiFillerConfig;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lets menu files place items by slot number instead of a drawn layout:
 * <pre>
 * rows: 6
 * sell-item:
 *   slot: 49
 * bait-slots: ["10-16", 19]
 * fillers:
 *   gray_stained_glass_pane: ["0-8", 45]
 * </pre>
 * Slots start at 0 in the top left. A slot entry can be a number, a range like {@code 10-16}, a
 * comma separated string, or a list of those. Files that still have a {@code layout} list keep
 * working the old way.
 * <p>
 * The inventory library places items by character, so each slotted entry is given a character
 * here. The same file always gives the same characters, so every lookup agrees.
 */
public final class SlotLayout {

    private static final String POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String FILLER_POOL = "123456789";

    private final String[] rows;
    private final Map<String, Character> characters = new HashMap<>();

    private SlotLayout(@NonNull Section root) {
        Map<String, List<Integer>> slots = new LinkedHashMap<>();
        for (String key : root.getRoutesAsStrings(false)) {
            if (key.equals("fillers")) {
                continue;
            }
            Object value = root.get(key);
            if (value instanceof Section section) {
                Object raw = section.contains("slots") ? section.get("slots") : section.get("slot");
                if (raw != null) {
                    slots.put(key, parse(raw));
                }
            } else if (key.endsWith("-slots") && value != null) {
                slots.put(key, parse(value));
            }
        }

        int highest = 0;
        for (List<Integer> list : slots.values()) {
            for (int slot : list) {
                highest = Math.max(highest, slot);
            }
        }
        Section fillers = root.getSection("fillers");
        Map<String, List<Integer>> fillerSlots = new LinkedHashMap<>();
        if (fillers != null) {
            for (String name : fillers.getRoutesAsStrings(false)) {
                List<Integer> list = parse(fillers.get(name));
                fillerSlots.put(name, list);
                for (int slot : list) {
                    highest = Math.max(highest, slot);
                }
            }
        }

        int rowCount = root.getInt("rows", 0);
        if (rowCount < 1 || rowCount > 6) {
            rowCount = Math.min(6, highest / 9 + 1);
        }
        char[][] grid = new char[rowCount][9];
        for (char[] row : grid) {
            Arrays.fill(row, ' ');
        }

        // Fillers go down first so real items always win a shared slot.
        Map<String, Character> fillerChars = fillerCharacters();
        fillerSlots.forEach((name, list) -> {
            Character character = fillerChars.get(name.toLowerCase(Locale.ROOT));
            if (character != null) {
                place(grid, list, character);
            }
        });

        int next = 0;
        for (Map.Entry<String, List<Integer>> entry : slots.entrySet()) {
            char character = POOL.charAt(Math.min(next++, POOL.length() - 1));
            characters.put(entry.getKey(), character);
            place(grid, entry.getValue(), character);
        }

        this.rows = new String[rowCount];
        for (int i = 0; i < rowCount; i++) {
            rows[i] = new String(grid[i]);
        }
    }

    private static void place(char[][] grid, List<Integer> slots, char character) {
        for (int slot : slots) {
            if (slot >= 0 && slot < grid.length * 9) {
                grid[slot / 9][slot % 9] = character;
            }
        }
    }

    public static boolean isSlotBased(@NonNull Section root) {
        return !root.contains("layout");
    }

    public static @NonNull SlotLayout of(@NonNull Section root) {
        return new SlotLayout(root);
    }

    public @NonNull String @NonNull [] getRows() {
        return rows.clone();
    }

    /** The character for an item section. Old layout files keep using their own "character" key. */
    public static char itemCharacter(@NonNull Section section, char fallback) {
        Section root = section.getRoot();
        if (!isSlotBased(root)) {
            return FishUtils.getCharFromString(section.getString("character", String.valueOf(fallback)), fallback);
        }
        Character found = of(root).characters.get(section.getRouteAsString());
        return found == null ? fallback : found;
    }

    /** The character for a group of slots such as "bait-slots", with "bait-character" as the old name. */
    public static char groupCharacter(@NonNull Section section, @NonNull String legacyKey, @NonNull String slotsKey, char fallback) {
        Section root = section.getRoot();
        if (!isSlotBased(root)) {
            return FishUtils.getCharFromString(section.getString(legacyKey, String.valueOf(fallback)), fallback);
        }
        Character found = of(root).characters.get(slotsKey);
        return found == null ? fallback : found;
    }

    /** The character used for a decorative filler. Fillers can name their own, or get 1, 2, 3 in file order. */
    public static char fillerCharacter(@NonNull Section fillerSection) {
        String own = fillerSection.getString("character");
        if (own != null && !own.isEmpty()) {
            return own.charAt(0);
        }
        String route = fillerSection.getRouteAsString();
        Character found = fillerCharacters().get(route == null ? "" : route.replaceFirst("-filler$", "").toLowerCase(Locale.ROOT));
        return found == null ? '#' : found;
    }

    private static Map<String, Character> fillerCharacters() {
        Map<String, Character> result = new HashMap<>();
        GuiFillerConfig config = GuiFillerConfig.getInstance();
        if (config == null) {
            return result;
        }
        int next = 0;
        for (String key : config.getConfig().getRoutesAsStrings(false)) {
            Section section = config.getConfig().getSection(key);
            if (section == null || !section.contains("item")) {
                continue;
            }
            String own = section.getString("character");
            char character = (own != null && !own.isEmpty()) ? own.charAt(0) : FILLER_POOL.charAt(Math.min(next++, FILLER_POOL.length() - 1));
            result.put(key.replaceFirst("-filler$", "").toLowerCase(Locale.ROOT), character);
        }
        return result;
    }

    public static List<Integer> parse(Object raw) {
        List<Integer> slots = new ArrayList<>();
        collect(raw, slots);
        return slots;
    }

    private static void collect(Object raw, List<Integer> into) {
        if (raw instanceof Number number) {
            into.add(number.intValue());
        } else if (raw instanceof List<?> list) {
            list.forEach(entry -> collect(entry, into));
        } else if (raw != null) {
            for (String part : raw.toString().split(",")) {
                String text = part.trim();
                if (text.isEmpty()) {
                    continue;
                }
                try {
                    int dash = text.indexOf('-', 1);
                    if (dash > 0) {
                        int from = Integer.parseInt(text.substring(0, dash).trim());
                        int to = Integer.parseInt(text.substring(dash + 1).trim());
                        for (int slot = Math.min(from, to); slot <= Math.max(from, to); slot++) {
                            into.add(slot);
                        }
                    } else {
                        into.add(Integer.parseInt(text));
                    }
                } catch (NumberFormatException ignored) {
                    // Not a slot, skip it.
                }
            }
        }
    }
}
