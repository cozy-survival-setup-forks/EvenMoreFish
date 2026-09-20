package com.oheers.fish.gui.guis;

import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.api.Logging;
import com.oheers.fish.api.fishing.items.IFish;
import com.oheers.fish.api.fishing.items.IRarity;
import com.oheers.fish.api.utils.Scheduling;
import com.oheers.fish.config.gui.impl.StatsGuiConfig;
import com.oheers.fish.database.DatabaseUtil;
import com.oheers.fish.database.data.UserFishRarityKey;
import com.oheers.fish.database.model.user.UserFishStats;
import com.oheers.fish.database.model.user.UserReport;
import com.oheers.fish.fishing.items.FishManager;
import com.oheers.fish.gui.ConfigGui;
import com.oheers.fish.gui.InfoItems;
import com.oheers.fish.messages.EMFSingleMessage;
import com.oheers.fish.progression.LevelCurve;
import com.oheers.fish.progression.ProgressionConfig;
import com.oheers.fish.progression.ProgressionManager;
import de.themoep.inventorygui.StaticGuiElement;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The player's own fishing numbers: how many fish of each rarity they caught, what they earned,
 * and where they stand in the skill tree.
 */
public class StatsGui extends ConfigGui {

    private final int userId;

    /** Loads the player's statistics off the server thread first, then opens the menu. */
    public static void openAsync(@NonNull Player player) {
        if (!DatabaseUtil.isDatabaseOnline()) {
            // Levels and skills live on the player, so those still work.
            new StatsGui(player, -1).open();
            return;
        }
        EvenMoreFish plugin = EvenMoreFish.getInstance();
        plugin.getPluginDataManager().preloadUserDataAsync(player.getUniqueId()).whenComplete((userId, throwable) -> {
            if (throwable != null) {
                Logging.warn("Could not prepare the stats menu for " + player.getName() + ".", throwable);
                return;
            }
            Scheduling.getInstance().runTask(player, () -> {
                if (player.isOnline()) {
                    new StatsGui(player, userId).open();
                }
            });
        });
    }

    private StatsGui(@NonNull Player player, int userId) {
        super(StatsGuiConfig.getInstance(), player);
        this.userId = userId;
        createGui();

        getGui().setTitle(EMFSingleMessage.fromString(
            InfoItems.fill(StatsGuiConfig.getInstance().getConfig().getString("title", "Fishing | Stats {player}"),
                Map.of("{player}", player.getName()))
        ).getLegacyMessage(player));

        Section config = getGuiConfig();
        addSkillTree(config.getSection("skill-tree"));
        addGeneralStats(config.getSection("general-stats"));
    }

    private void addSkillTree(Section section) {
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

        getGui().addElement(new StaticGuiElement(
            firstCharacter(section, 's'),
            build(section, values),
            click -> {
                new SkillTreeGui(player).open();
                return true;
            }
        ));
    }

    private void addGeneralStats(Section section) {
        if (section == null) {
            return;
        }
        var dataManager = EvenMoreFish.getInstance().getPluginDataManager();
        boolean hasData = userId >= 0;
        UserReport report = hasData ? dataManager.getUserReportDataManager().peek(player.getUniqueId().toString()) : null;

        List<String> rarityLines = new ArrayList<>();
        long total = 0;
        String rarityTemplate = section.getString("rarity-line", "{rarity}: {count}");
        for (IRarity rarity : FishManager.getInstance().getRarityMap().values()) {
            if (rarity.isDisabled() || !rarity.getShowInJournal()) {
                continue;
            }
            long caught = 0;
            for (IFish fish : hasData ? rarity.getFishList() : List.<IFish>of()) {
                UserFishStats stats = dataManager.getUserFishStatsDataManager().peek(UserFishRarityKey.of(userId, fish).toString());
                if (stats != null) {
                    caught += stats.getQuantity();
                }
            }
            total += caught;
            rarityLines.add(InfoItems.fill(rarityTemplate, Map.of(
                "{rarity}", legacyName(rarity),
                "{count}", Long.toString(caught)
            )));
        }

        Map<String, String> values = new LinkedHashMap<>();
        values.put("{total-caught}", Long.toString(report == null ? total : Math.max(total, report.getNumFishCaught())));
        values.put("{fish-sold}", report == null ? "0" : Integer.toString(report.getFishSold()));
        values.put("{money-made}", report == null ? "0" : String.format(Locale.ROOT, "%,.2f", report.getMoneyEarned()));
        values.put("{longest-fish}", report == null ? "0" : String.format(Locale.ROOT, "%.1f", report.getLargestLength()));
        values.put("{competitions-won}", report == null ? "0" : Integer.toString(report.getCompetitionsWon()));
        values.put("{competitions-joined}", report == null ? "0" : Integer.toString(report.getCompetitionsJoined()));

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lines")) {
            if (line.contains("{rarity-lines}")) {
                lore.addAll(rarityLines);
            } else {
                lore.add(InfoItems.fill(line, values));
            }
        }

        getGui().addElement(new StaticGuiElement(
            firstCharacter(section, 'g'),
            InfoItems.build(InfoItems.material(section, "book"), section.getString("title", "General Stats"), lore)
        ));
    }

    private org.bukkit.inventory.ItemStack build(Section section, Map<String, String> values) {
        List<String> lore = section.getStringList("lines").stream().map(line -> InfoItems.fill(line, values)).toList();
        return InfoItems.build(InfoItems.material(section, "paper"), section.getString("title", ""), lore);
    }

    private static char firstCharacter(Section section, char fallback) {
        String value = section.getString("character", String.valueOf(fallback));
        return value.isEmpty() ? fallback : value.charAt(0);
    }

    private static String legacyName(IRarity rarity) {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(rarity.getDisplayName());
    }

    @Override
    public void doRescue() { /* Nothing to give back, view only. */ }

}
