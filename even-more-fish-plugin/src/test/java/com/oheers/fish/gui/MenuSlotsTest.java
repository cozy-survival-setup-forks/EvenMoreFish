package com.oheers.fish.gui;

import com.oheers.fish.config.gui.SlotLayout;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
