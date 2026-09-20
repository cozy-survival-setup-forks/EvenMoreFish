package com.oheers.fish.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every default menu file must have layout rows nine slots wide, or the menu will not open. */
class MenuLayoutTest {

    private static final Pattern ROW = Pattern.compile("^\s+-\s+\"(.*)\"\s*$");

    @Test
    void layoutRowsAreNineWide() throws IOException {
        Path root = Path.of("src/main/resources/gui");
        int checked = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".yml")).toList()) {
                List<String> lines = Files.readAllLines(file);
                int start = lines.indexOf("layout:");
                if (start < 0) {
                    continue;
                }
                for (int i = start + 1; i < lines.size(); i++) {
                    Matcher matcher = ROW.matcher(lines.get(i));
                    if (!matcher.matches()) {
                        break;
                    }
                    assertEquals(9, matcher.group(1).length(), file + " row " + (i - start) + ": " + matcher.group(1));
                    checked++;
                }
            }
        }
        assertTrue(checked > 0);
    }
}
