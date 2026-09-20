package com.oheers.fish.messages;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text that mixes MiniMessage tags with old style colour codes, such as {@code <gray>&#FFD9B3Nemo},
 * is read as MiniMessage only, so the codes would show up as plain text. This turns the codes into
 * MiniMessage tags first. Text without any MiniMessage tags is left alone, as it is read as old style
 * text already.
 */
public final class LegacyText {

    private static final Pattern HEX = Pattern.compile("[&§]#([0-9a-fA-F]{6})");
    private static final Pattern SPIGOT_HEX = Pattern.compile("[&§]x([&§][0-9a-fA-F]){6}");
    private static final Pattern CODE = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");

    private static final Map<Character, String> TAGS = Map.ofEntries(
        Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"), Map.entry('3', "dark_aqua"),
        Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"), Map.entry('6', "gold"), Map.entry('7', "gray"),
        Map.entry('8', "dark_gray"), Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
        Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"), Map.entry('f', "white"),
        Map.entry('k', "obfuscated"), Map.entry('l', "bold"), Map.entry('m', "strikethrough"),
        Map.entry('n', "underlined"), Map.entry('o', "italic"), Map.entry('r', "reset")
    );

    private LegacyText() {
    }

    public static @NonNull String toMiniMessage(@NonNull String input) {
        if (input.indexOf('&') < 0 && input.indexOf('§') < 0) {
            return input;
        }
        // Without tags the text is already read as old style text.
        if (MiniMessage.miniMessage().stripTags(input).equals(input)) {
            return input;
        }

        String result = SPIGOT_HEX.matcher(input).replaceAll(match -> {
            String hex = match.group().replaceAll("[&§x]", "");
            return Matcher.quoteReplacement("<#" + hex + ">");
        });
        result = HEX.matcher(result).replaceAll("<#$1>");
        return CODE.matcher(result).replaceAll(match -> {
            String tag = TAGS.get(Character.toLowerCase(match.group(1).charAt(0)));
            return Matcher.quoteReplacement("<" + tag + ">");
        });
    }

    public static @NonNull List<String> toMiniMessage(@NonNull List<String> input) {
        return input.stream().map(LegacyText::toMiniMessage).toList();
    }
}
