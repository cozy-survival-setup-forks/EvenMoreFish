package com.oheers.fish.gui;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds menu items whose text is written in a GUI file and filled in from code, for the menus that
 * show live numbers such as stats and prices. Text is MiniMessage and {placeholders} are swapped
 * for their values before it is parsed.
 */
public final class InfoItems {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private InfoItems() {
    }

    public static @NonNull String fill(@NonNull String text, @NonNull Map<String, String> values) {
        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** A line of menu text, without the italics items get by default. */
    public static @NonNull Component line(@NonNull String text) {
        return MINI.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static @NonNull List<Component> lines(@NonNull List<String> texts) {
        List<Component> components = new ArrayList<>(texts.size());
        for (String text : texts) {
            components.add(line(text));
        }
        return components;
    }

    /** The material named in a section, or a fallback when it is missing or not a real material. */
    public static @NonNull Material material(@Nullable Section section, @NonNull String fallback) {
        String name = section == null ? fallback : section.getString("material", fallback);
        return Optional.ofNullable(Material.matchMaterial(name))
            .or(() -> Optional.ofNullable(Material.matchMaterial(fallback)))
            .orElse(Material.PAPER);
    }

    public static @NonNull ItemStack build(@NonNull Material material, @NonNull String title, @NonNull List<String> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(line(title));
            meta.lore(lines(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return item;
    }
}
