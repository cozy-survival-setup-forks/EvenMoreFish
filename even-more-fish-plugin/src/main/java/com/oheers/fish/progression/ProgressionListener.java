package com.oheers.fish.progression;

import com.oheers.fish.api.events.EMFFishCaughtEvent;
import com.oheers.fish.api.events.EMFFishHuntEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Gives fishing XP for every fish a player catches or hunts.
 */
public final class ProgressionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCatch(EMFFishCaughtEvent event) {
        award(event.getPlayer(), event.getFish().getRarity().getId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHunt(EMFFishHuntEvent event) {
        award(event.getPlayer(), event.getFish().getRarity().getId());
    }

    private void award(Player player, String rarityId) {
        ProgressionManager.getInstance().addXp(player, ProgressionConfig.getInstance().xpFor(rarityId));
    }
}
