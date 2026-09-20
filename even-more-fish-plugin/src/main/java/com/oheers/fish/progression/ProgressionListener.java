package com.oheers.fish.progression;

import com.oheers.fish.api.events.EMFFishCaughtEvent;
import com.oheers.fish.api.fishing.items.IFish;
import com.oheers.fish.fishing.items.Fish;
import com.oheers.fish.api.events.EMFFishHuntEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gives fishing XP for every fish a player catches or hunts.
 */
public final class ProgressionListener implements Listener {

    /** Bites never get more than this much faster, so fishing does not turn into a machine gun. */
    private static final double MAX_BITE_REDUCTION = 80.0D;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCatch(EMFFishCaughtEvent event) {
        Player player = event.getPlayer();
        award(player, event.getFish());
        rollDoubleCatch(player, event);
    }

    /** Fish bite sooner for players with the fast bite skill. */
    @EventHandler(ignoreCancelled = true)
    public void onCast(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING || !ProgressionConfig.getInstance().isEnabled()) {
            return;
        }
        double percent = Math.min(MAX_BITE_REDUCTION, ProgressionManager.getInstance().getBonus(event.getPlayer(), ProgressionConfig.Effect.FAST_BITE));
        if (percent <= 0) {
            return;
        }
        FishHook hook = event.getHook();
        double keep = 1.0D - percent / 100.0D;
        int min = Math.max(1, (int) Math.round(hook.getMinWaitTime() * keep));
        int max = Math.max(min, (int) Math.round(hook.getMaxWaitTime() * keep));
        hook.setMinWaitTime(min);
        hook.setMaxWaitTime(max);
    }

    private void rollDoubleCatch(Player player, EMFFishCaughtEvent event) {
        if (!ProgressionConfig.getInstance().isEnabled()) {
            return;
        }
        double chance = ProgressionManager.getInstance().getBonus(player, ProgressionConfig.Effect.DOUBLE_CATCH);
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() * 100.0D >= chance) {
            return;
        }
        ItemStack extra = event.getFish().createCopy().give();
        player.getInventory().addItem(extra).values()
            .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        String message = ProgressionConfig.getInstance().doubleCatchMessage();
        if (!message.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHunt(EMFFishHuntEvent event) {
        award(event.getPlayer(), event.getFish());
    }

    private void award(Player player, IFish fish) {
        long override = fish instanceof Fish configured ? configured.getXpOverride() : -1L;
        long xp = override >= 0 ? override : ProgressionConfig.getInstance().xpFor(fish.getRarity().getId());
        ProgressionManager.getInstance().addXp(player, xp);
    }
}
