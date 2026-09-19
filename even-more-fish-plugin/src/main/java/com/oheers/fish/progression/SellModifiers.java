package com.oheers.fish.progression;

import com.oheers.fish.api.economy.selling.SoldFish;
import com.oheers.fish.api.events.EMFFishPreSaleEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Extra money for selling: the special fish bonus and the Master Merchant style skills. The same
 * calculation is used when a sale really happens and when a menu only shows what fish are worth.
 */
public final class SellModifiers implements Listener {

    /** The value of one fish after the player's bonuses. A negative value means it cannot be sold. */
    public static double valueOf(@Nullable Player player, @NonNull SoldFish sold) {
        double value = sold.getValue();
        if (value < 0 || !ProgressionConfig.getInstance().isEnabled()) {
            return value;
        }
        if (SpecialFish.isEnabled() && SpecialFish.isSpecial(sold.getFish())) {
            value *= SpecialFish.multiplierFor(player);
        }
        if (player != null) {
            value *= 1.0D + ProgressionManager.getInstance().getBonus(player, ProgressionConfig.Effect.SELL_BONUS) / 100.0D;
        }
        return value;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreSale(EMFFishPreSaleEvent event) {
        SoldFish sold = event.getSoldFish();
        sold.setValue(valueOf(event.getPlayer(), sold));
    }
}
