package com.oheers.fish.gui.guis;

import com.oheers.fish.config.gui.SlotLayout;
import com.oheers.fish.config.gui.impl.SkillsGuiConfig;
import com.oheers.fish.gui.ConfigGui;
import com.oheers.fish.gui.InfoItems;
import com.oheers.fish.progression.LevelCurve;
import com.oheers.fish.progression.ProgressionConfig;
import com.oheers.fish.progression.ProgressionManager;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.StaticGuiElement;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The skill tree: spend the skill points earned from fishing levels on permanent bonuses.
 */
public class SkillTreeGui extends ConfigGui {

    public SkillTreeGui(@NonNull Player player) {
        super(SkillsGuiConfig.getInstance(), player);
        createGui();

        Section config = getGuiConfig();
        addSummary(config.getSection("summary"));
        addSkills(config.getSection("skill"));
    }

    private void addSummary(Section section) {
        if (section == null) {
            return;
        }
        ProgressionManager progression = ProgressionManager.getInstance();
        LevelCurve curve = ProgressionConfig.getInstance().curve();
        long xp = progression.getXp(player);

        Map<String, String> values = new LinkedHashMap<>();
        values.put("{level}", Integer.toString(curve.levelFor(xp)));
        values.put("{skill-points}", Integer.toString(progression.getAvailablePoints(player)));
        values.put("{xp-into-level}", Long.toString(curve.xpIntoLevel(xp)));
        values.put("{xp-required}", Long.toString(curve.xpForNextLevel(xp)));

        List<String> lore = section.getStringList("lines").stream().map(line -> InfoItems.fill(line, values)).toList();
        getGui().addElement(new StaticGuiElement(
            character(section, 'i'),
            InfoItems.build(InfoItems.material(section, "experience_bottle"), section.getString("title", ""), lore)
        ));
    }

    private void addSkills(Section section) {
        if (section == null) {
            return;
        }
        GuiElementGroup group = new GuiElementGroup(character(section, 'k'));
        ProgressionManager progression = ProgressionManager.getInstance();

        for (ProgressionConfig.Skill skill : ProgressionConfig.getInstance().skills()) {
            int level = progression.getSkillLevel(player, skill.id());
            boolean maxed = level >= skill.maxLevel();

            Map<String, String> values = new LinkedHashMap<>();
            values.put("{name}", skill.name());
            values.put("{description}", skill.description());
            values.put("{level}", Integer.toString(level));
            values.put("{max-level}", Integer.toString(skill.maxLevel()));
            values.put("{cost}", Integer.toString(skill.cost()));
            String template = section.getString("effects." + skill.effect().name().toLowerCase(Locale.ROOT).replace('_', '-'), "{bonus}");
            values.put("{current-bonus}", format(skill.perLevel() * level));
            values.put("{per-level}", format(skill.perLevel()));
            values.put("{current-effect}", template.replace("{bonus}", format(skill.perLevel() * level)));
            values.put("{per-level-effect}", template.replace("{bonus}", format(skill.perLevel())));
            values.put("{effect}", template.replace("{bonus}", format(skill.perLevel())));

            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lines")) {
                lore.add(InfoItems.fill(line, values));
            }
            lore.add(InfoItems.fill(section.getString(maxed ? "maxed-line" : "buy-line", ""), values));

            var item = InfoItems.build(skill.icon(), InfoItems.fill(section.getString("title", "{name}"), values), lore);
            if (level > 0) {
                item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
            }

            group.addElement(new StaticGuiElement(character(section, 'k'), item, click -> {
                handleBuy(skill);
                return true;
            }));
        }
        getGui().addElement(group);
    }

    private void handleBuy(ProgressionConfig.Skill skill) {
        ProgressionManager.BuyResult result = ProgressionManager.getInstance().buy(player, skill.id());
        String key = switch (result) {
            case BOUGHT -> "messages.bought";
            case MAXED -> "messages.maxed";
            case NOT_ENOUGH_POINTS -> "messages.not-enough-points";
            case UNKNOWN_SKILL -> "messages.unknown";
        };
        String text = getGuiConfig().getString(key, "");
        if (!text.isEmpty()) {
            player.sendMessage(InfoItems.line(InfoItems.fill(text, Map.of("{name}", skill.name()))));
        }
        if (result == ProgressionManager.BuyResult.BOUGHT) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.3f);
            // Redraw with the new numbers.
            new SkillTreeGui(player).open();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static char character(Section section, char fallback) {
        return SlotLayout.itemCharacter(section, fallback);
    }

    @Override
    public void doRescue() { /* Nothing to give back, view only. */ }

}
