package com.oheers.fish.gui;

import com.oheers.fish.config.gui.SlotLayout;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The default menu files must be slot based and every slot must fit in the menu. */
class MenuSlotsTest {

    @Test
    void everyDefaultMenuFitsItsRows() throws IOException {
        Path root = Path.of("src/main/resources/gui");
        int checked = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".yml")).toList()) {
                try (InputStream in = Files.newInputStream(file)) {
                    YamlDocument doc = YamlDocument.create(in);
                    assertTrue(SlotLayout.isSlotBased(doc), file + " still has a layout list");
                    String[] rows = SlotLayout.of(doc).getRows();
                    assertTrue(rows.length >= 1 && rows.length <= 6, file + " has " + rows.length + " rows");
                    for (String row : rows) {
                        assertEquals(9, row.length(), file.toString());
                    }
                    assertEquals(doc.getInt("rows"), rows.length, file.toString());
                    assertTrue(String.join("", rows).chars().anyMatch(c -> c != ' '), file + " has no items");
                    checked++;
                }
            }
        }
        assertTrue(checked >= 9);
    }

    @Test
    void noSlotIsUsedByTwoEntries() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources/gui"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".yml")).toList()) {
                try (InputStream in = Files.newInputStream(file)) {
                    YamlDocument doc = YamlDocument.create(in);
                    Map<Integer, String> owners = new HashMap<>();
                    for (String key : doc.getRoutesAsStrings(false)) {
                        if (key.equals("fillers")) {
                            continue;
                        }
                        Object value = doc.get(key);
                        Object raw = null;
                        if (value instanceof Section section) {
                            raw = section.contains("slots") ? section.get("slots") : section.get("slot");
                        } else if (key.endsWith("-slots")) {
                            raw = value;
                        }
                        for (int slot : SlotLayout.parse(raw)) {
                            String previous = owners.put(slot, key);
                            assertNull(previous, file + ": slot " + slot + " is used by both " + previous + " and " + key);
                        }
                    }
                }
            }
        }
    }

    @Test
    void slotsSupportNumbersRangesAndLists() throws IOException {
        String yaml = """
            rows: 3
            a:
              item:
                material: stone
              slot: 4
            b-slots: ["0-2", 9, "20, 22"]
            """;
        YamlDocument doc = YamlDocument.create(new java.io.ByteArrayInputStream(yaml.getBytes()));
        String[] rows = SlotLayout.of(doc).getRows();
        assertEquals(3, rows.length);
        char a = rows[0].charAt(4);
        char b = rows[0].charAt(0);
        assertFalse(a == ' ' || b == ' ' || a == b);
        assertEquals("" + b + b + b, rows[0].substring(0, 3));
        assertEquals(b, rows[1].charAt(0));
        assertEquals(b, rows[2].charAt(2));
        assertEquals(' ', rows[2].charAt(3));
        assertEquals(b, rows[2].charAt(4));
    }

    @Test
    void oldLayoutFilesAreStillRecognised() throws IOException {
        YamlDocument doc = YamlDocument.create(new java.io.ByteArrayInputStream("layout:\n  - \"         \"\n".getBytes()));
        assertFalse(SlotLayout.isSlotBased(doc));
    }
}
