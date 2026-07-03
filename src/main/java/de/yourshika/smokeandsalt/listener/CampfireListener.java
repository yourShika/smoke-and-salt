package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.config.MessageManager;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Lagerfeuer-Station: Rechtsklick auf ein brennendes Lagerfeuer mit passender
 * Zutat startet einen Gar-Vorgang mit Rauch-Partikeln. Nur wenn ein passendes
 * Rezept existiert, wird die vanilla-Lagerfeuer-Interaktion unterdrueckt.
 */
public final class CampfireListener implements Listener {

    private final SmokeAndSalt plugin;

    public CampfireListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.CAMPFIRE && block.getType() != Material.SOUL_CAMPFIRE) return;
        if (block.getBlockData() instanceof Lightable light && !light.isLit()) return;

        var config = plugin.pluginConfig();
        if (!config.cookingEnabled() || !config.campfireEnabled()) return;
        if (!event.getPlayer().hasPermission("smokeandsalt.use")) return;
        if (!config.isWorldAllowed(block.getWorld().getName())) return;

        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (hand.getType().isAir()) return;

        Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.CAMPFIRE, hand);
        if (recipe.isEmpty()) return;

        event.setCancelled(true);
        if (plugin.cooking().isBusy(block)) {
            plugin.messages().send(event.getPlayer(), "cooking.busy");
            return;
        }

        ItemStack input = hand.clone();
        input.setAmount(1);
        boolean started = plugin.cooking().startBlockCook(block, CookingStation.CAMPFIRE, recipe.get(), input);
        if (started) {
            hand.setAmount(hand.getAmount() - 1);
            plugin.effects().smoke(block.getLocation(), 6);
            plugin.messages().send(event.getPlayer(), "cooking.started",
                    MessageManager.ph("station", CookingStation.CAMPFIRE.displayName()));
        }
    }
}
