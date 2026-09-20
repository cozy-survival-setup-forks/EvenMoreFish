package com.oheers.fish.gui;

import com.oheers.fish.config.gui.SlotLayout;
import com.oheers.fish.api.economy.Economy;
import com.oheers.fish.api.fishing.items.IFish;
import com.oheers.fish.api.fishing.items.IRarity;
import com.oheers.fish.fishing.items.Fish;
import com.oheers.fish.fishing.items.FishManager;
import com.oheers.fish.messages.LegacyText;
import com.oheers.fish.progression.SpecialFish;
import de.themoep.inventorygui.StaticGuiElement;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import uk.firedev.daisylib.messages.message.ComponentMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The price list and special fish items of the sell menu, plus the price, size and biome text the
 * fish codex shows. Everything read from a GUI file is optional, so a menu without these sections
 * simply does not get the items.
 */
public final class SellInfoItems {

    private SellInfoItems() {
    }

    public static void addTo(@NonNull ConfigGui gui, @NonNull Player player) {
        Section config = gui.getGuiConfig();
        if (config == null) {
            return;
        }
        addPrices(gui, config.getSection("prices"));
        addSpecialFish(gui, config.getSection("special-fish"), player);
    }

    private static void addPrices(ConfigGui gui, Section section) {
        if (section == null) {
            return;
        }
        List<String> lore = new ArrayList<>(section.getStringList("lines"));
        String rarityLine = section.getString("rarity-line", "{rarity}: {price}");
        for (IRarity rarity : FishManager.getInstance().getRarityMap().values()) {
            if (rarity.isDisabled() || !rarity.getShowInJournal()) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            values.put("{rarity}", MiniMessage.miniMessage().serialize(rarity.getDisplayName()));
            values.put("{price}", averagePrice(rarity));
            lore.add(InfoItems.fill(rarityLine, values));
        }
        gui.getGui().addElement(new StaticGuiElement(
            character(section, 'p'),
            InfoItems.build(InfoItems.material(section, "emerald"), section.getString("title", ""), lore)
        ));
    }

    private static void addSpecialFish(ConfigGui gui, Section section, Player player) {
        if (section == null || !SpecialFish.isEnabled()) {
            return;
        }
        IFish special = SpecialFish.current();
        if (special == null) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("{fish}", MiniMessage.miniMessage().serialize(special.getDisplayName()));
        values.put("{tier}", MiniMessage.miniMessage().serialize(special.getRarity().getDisplayName()));
        values.put("{multiplier}", trim(SpecialFish.multiplierFor(player)));
        values.put("{minutes}", Long.toString(SpecialFish.minutesUntilReset()));

        List<String> lore = section.getStringList("lines").stream().map(line -> InfoItems.fill(line, values)).toList();
        Material icon = InfoItems.material(section, "pufferfish");
        gui.getGui().addElement(new StaticGuiElement(
            character(section, 'k'),
            InfoItems.build(icon, InfoItems.fill(section.getString("title", ""), values), lore)
        ));
    }

    /** What a fish sells for, as a plain string. Fish without a fixed worth are priced at their average size. */
    public static @NonNull String priceText(@NonNull IFish fish) {
        return format(basePrice(fish.getSetWorth(), fish.getWorthMultiplier(), fish.getMinSize(), fish.getMaxSize()));
    }

    public static @NonNull String sizeText(@NonNull IFish fish) {
        if (fish.getSetSize().isPresent()) {
            return trim(fish.getSetSize().get()) + "cm";
        }
        return trim(fish.getMinSize()) + "-" + trim(fish.getMaxSize()) + "cm";
    }

    public static @NonNull String biomesText(@NonNull IFish fish) {
        List<String> biomes = new ArrayList<>(fish.getRequirement().getValues("biome"));
        biomes.addAll(fish.getRarity().getRequirement().getValues("biome"));
        if (biomes.isEmpty()) {
            return "Any";
        }
        return String.join(", ", biomes.stream().map(SellInfoItems::prettify).distinct().toList());
    }

    private static final int BIOME_LINE_WIDTH = 34;

    /**
     * Swaps a lore line holding {biomes} for as many lines as the biome list needs, so a long list
     * wraps instead of running off the screen. Other lines are left alone.
     */
    public static @NonNull List<Component> expandBiomeLines(@NonNull List<Component> lore, @NonNull IFish fish) {
        return expandBiomeLines(lore, biomesText(fish));
    }

    static @NonNull List<Component> expandBiomeLines(@NonNull List<Component> lore, @NonNull String biomes) {
        MiniMessage mini = MiniMessage.miniMessage();
        List<Component> out = new ArrayList<>(lore.size());
        for (Component component : lore) {
            String line = mini.serialize(component);
            int at = line.indexOf("{biomes}");
            if (at < 0) {
                out.add(component);
                continue;
            }
            String prefix = line.substring(0, at);
            String suffix = line.substring(at + "{biomes}".length());
            Matcher tags = Pattern.compile("(<[^<>]+>)+$").matcher(prefix);
            String carry = tags.find() ? tags.group() : "";

            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String biome : biomes.split(", ")) {
                if (!current.isEmpty() && current.length() + biome.length() + 2 > BIOME_LINE_WIDTH) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                current.append(current.isEmpty() ? "" : ", ").append(biome);
            }
            lines.add(current.toString());

            for (int i = 0; i < lines.size(); i++) {
                String text = lines.get(i) + (i < lines.size() - 1 ? "," : "");
                out.add(mini.deserialize((i == 0 ? prefix : carry + "  ") + text + suffix));
            }
        }
        return out;
    }

    private static final String FISH_LORE = "{fish-lore}";

    /**
     * Puts the fish's own lore (the lore-override in its rarity file, for example its description) into
     * the codex tooltip. It goes where a line says {fish-lore}, or at the top if no line does.
     */
    public static @NonNull List<Component> expandFishLore(@NonNull List<Component> lore, @NonNull IFish fish) {
        if (!(fish instanceof Fish configured)) {
            return lore;
        }
        MiniMessage mini = MiniMessage.miniMessage();
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

        List<Component> own = new ArrayList<>();
        for (String line : configured.getLoreOverride()) {
            // These are filled in when a fish is caught, there is nothing to show for them here.
            if (line.contains("{fisherman_lore}") || line.contains("{length_lore}") || line.contains("{fish_lore}")) {
                continue;
            }
            own.add(ComponentMessage.componentMessage(LegacyText.toMiniMessage(line)).get().decoration(TextDecoration.ITALIC, false));
        }

        boolean hasToken = lore.stream().anyMatch(line -> plain.serialize(line).contains(FISH_LORE));
        List<Component> out = new ArrayList<>(lore.size() + own.size() + 1);
        if (!hasToken && !own.isEmpty()) {
            out.addAll(own);
            out.add(Component.empty());
        }
        for (Component line : lore) {
            if (plain.serialize(line).contains(FISH_LORE)) {
                out.addAll(own);
            } else {
                out.add(line);
            }
        }
        return out;
    }

    private static String averagePrice(IRarity rarity) {
        List<? extends IFish> fish = rarity.getOriginalFishList();
        if (fish.isEmpty()) {
            return format(basePrice(rarity.getSetWorth(), rarity.getWorthMultiplier(), rarity.getMinSize(), rarity.getMaxSize()));
        }
        double total = 0;
        for (IFish entry : fish) {
            total += basePrice(entry.getSetWorth(), entry.getWorthMultiplier(), entry.getMinSize(), entry.getMaxSize());
        }
        return format(total / fish.size());
    }

    private static double basePrice(double setWorth, double multiplier, double minSize, double maxSize) {
        if (setWorth > 0) {
            return setWorth;
        }
        return multiplier * ((minSize + maxSize) / 2.0D);
    }

    private static String format(double value) {
        return PlainTextComponentSerializer.plainText().serialize(Economy.getInstance().getWorthFormat(value, true));
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String prettify(String key) {
        String name = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        StringBuilder out = new StringBuilder();
        for (String word : name.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static char character(Section section, char fallback) {
        return SlotLayout.itemCharacter(section, fallback);
    }
}
