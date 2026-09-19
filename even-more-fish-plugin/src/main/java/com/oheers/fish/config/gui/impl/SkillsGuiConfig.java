package com.oheers.fish.config.gui.impl;

import com.oheers.fish.config.gui.GuiConfig;
import org.jspecify.annotations.NonNull;

public class SkillsGuiConfig extends GuiConfig {

    private static final SkillsGuiConfig INSTANCE = new SkillsGuiConfig();

    private SkillsGuiConfig() {
        super("skills.yml");
    }

    public static @NonNull SkillsGuiConfig getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isPaginated() {
        return false;
    }

}
