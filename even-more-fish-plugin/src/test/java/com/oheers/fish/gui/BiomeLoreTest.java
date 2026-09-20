package com.oheers.fish.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeLoreTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void longBiomeListWrapsOntoSeveralLines() {
        String biomes = "Cold Ocean, Deep Cold Ocean, Deep Lukewarm Ocean, Deep Ocean, Lukewarm Ocean, Ocean, Warm Ocean";
        List<Component> lore = List.of(
            MiniMessage.miniMessage().deserialize("<gray>Tier: Common"),
            MiniMessage.miniMessage().deserialize("<gray>▸ Biomes: <red>{biomes}")
        );

        List<Component> expanded = SellInfoItems.expandBiomeLines(lore, biomes);

        assertTrue(expanded.size() > 2);
        assertEquals("Tier: Common", plain(expanded.get(0)));
        assertTrue(plain(expanded.get(1)).startsWith("▸ Biomes: Cold Ocean"));
        for (Component line : expanded.subList(1, expanded.size())) {
            assertTrue(plain(line).length() < 50, plain(line));
        }
    }

    @Test
    void shortBiomeListStaysOnOneLine() {
        List<Component> expanded = SellInfoItems.expandBiomeLines(
            List.of(MiniMessage.miniMessage().deserialize("<gray>Biomes: <red>{biomes}")), "Plains");
        assertEquals(1, expanded.size());
        assertEquals("Biomes: Plains", plain(expanded.get(0)));
    }
}
