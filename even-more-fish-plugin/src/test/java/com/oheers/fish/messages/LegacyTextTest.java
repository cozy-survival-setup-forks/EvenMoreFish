package com.oheers.fish.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTextTest {

    private static String plain(String miniMessage) {
        Component component = MiniMessage.miniMessage().deserialize(miniMessage);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static List<TextColor> colours(String miniMessage) {
        List<TextColor> found = new ArrayList<>();
        collect(MiniMessage.miniMessage().deserialize(miniMessage), found);
        return found;
    }

    private static void collect(Component component, List<TextColor> into) {
        if (component.color() != null) into.add(component.color());
        component.children().forEach(child -> collect(child, into));
    }

    @Test
    void hexCodesNextToTagsBecomeColours() {
        // The exact case from a fish named "&#FFD9B3<icon> Amber Puffer" inside a <gray> rarity format.
        String converted = LegacyText.toMiniMessage("<gray>&#FFD9B3☁ Amber Puffer");
        assertEquals("☁ Amber Puffer", plain(converted));
        assertTrue(colours(converted).contains(TextColor.fromHexString("#FFD9B3")));
    }

    @Test
    void normalCodesAreConverted() {
        String converted = LegacyText.toMiniMessage("<bold>&cRed &lBold");
        assertEquals("Red Bold", plain(converted));
        assertFalse(converted.contains("&"));
        assertTrue(colours(converted).contains(NamedTextColor.RED));
    }

    @Test
    void spigotHexIsConverted() {
        assertEquals("Hi", plain(LegacyText.toMiniMessage("<gray>§x§f§f§d§9§b§3Hi")));
    }

    @Test
    void textWithoutTagsIsLeftForTheLegacyReader() {
        assertEquals("&#FFD9B3Amber &7Puffer", LegacyText.toMiniMessage("&#FFD9B3Amber &7Puffer"));
        assertEquals("Salt&Pepper", LegacyText.toMiniMessage("Salt&Pepper"));
    }

    @Test
    void ampersandsThatAreNotCodesStay() {
        assertEquals("<gray>Salt&Pepper", LegacyText.toMiniMessage("<gray>Salt&Pepper"));
    }

    @Test
    void listsAreConverted() {
        assertEquals(List.of("<gray><red>Hi"), LegacyText.toMiniMessage(List.of("<gray>&cHi")));
    }
}
