package com.oheers.fish.config.gui.impl;

import com.oheers.fish.config.gui.GuiConfig;
import org.jspecify.annotations.NonNull;

public class StatsGuiConfig extends GuiConfig {

    private static final StatsGuiConfig INSTANCE = new StatsGuiConfig();

    private StatsGuiConfig() {
        super("stats.yml");
    }

    public static @NonNull StatsGuiConfig getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

}
