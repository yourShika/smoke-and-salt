package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Smoker-Station: Rechtsklick auf einen Smoker-Block mit einer passenden Zutat in
 * der Hand legt sie in einen zeitlichen Raeucher-Vorgang (Rauch-Partikel). Nur
 * wenn ein passendes Rezept existiert, wird der vanilla-Smoker unterdrueckt.
 */
public final class SmokerListener implements Listener {

    private final SmokeAndSalt plugin;

    public SmokerListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SMOKER) return;

        var config = plugin.pluginConfig();
        if (!config.cookingEnabled() || !config.smokerEnabled()) return;
        if (!event.getPlayer().hasPermission("smokeandsalt.use")) return;
        if (!config.isWorldAllowed(block.getWorld().getName())) return;

        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (hand.getType().isAir()) return;

        Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.SMOKER, hand);
        if (recipe.isEmpty()) return; // kein Rezept -> vanilla-Smoker bleibt nutzbar

        event.setCancelled(true);
        if (plugin.cooking().isBusy(block)) {
            plugin.messages().send(event.getPlayer(), "cooking.busy");
            return;
        }

        ItemStack input = hand.clone();
        input.setAmount(1);
        boolean started = plugin.cooking().startBlockCook(block, CookingStation.SMOKER, recipe.get(), input);
        if (started) {
            hand.setAmount(hand.getAmount() - 1);
            plugin.effects().smoke(block.getLocation(), 6);
            plugin.messages().send(event.getPlayer(), "cooking.started",
                    de.yourshika.smokeandsalt.config.MessageManager.ph("station", CookingStation.SMOKER.displayName()));
        }
    }
}
