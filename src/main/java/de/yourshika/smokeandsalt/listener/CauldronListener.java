package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.util.Heat;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Kessel-Station. Ein Item wird in einen Kessel geworfen (fallengelassen):
 *
 * <ul>
 *   <li><b>Wasserkessel</b> mit Waermequelle darunter (kochendes Wasser): das Item
 *       schwebt an der Oberflaeche und wird mit Blasen-Partikeln gekocht - fuer
 *       Kochen, Waschen, Bruehen und Suppe.</li>
 *   <li><b>Lavakessel</b>: dient als vorsichtige Frittier-/Bratstation. Nur Items
 *       mit passendem Rezept werden verarbeitet.</li>
 * </ul>
 *
 * Der Vorgang startet erst, wenn ein passendes Rezept existiert.
 */
public final class CauldronListener implements Listener {

    private static final long LANDING_DELAY = 12L;

    private final SmokeAndSalt plugin;

    public CauldronListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.pluginConfig().cookingEnabled()) return;
        if (!event.getPlayer().hasPermission("smokeandsalt.use")) return;
        Item entity = event.getItemDrop();
        // Kurz warten, bis das Item im Kessel zur Ruhe gekommen ist.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> handleLanded(entity), LANDING_DELAY);
    }

    private void handleLanded(Item entity) {
        if (entity == null || !entity.isValid() || entity.isDead()) return;
        Block cauldron = findCauldron(entity);
        if (cauldron == null) return;
        if (!plugin.pluginConfig().isWorldAllowed(cauldron.getWorld().getName())) return;
        if (plugin.cooking().isBusy(cauldron)) return;

        ItemStack stack = entity.getItemStack();
        Material type = cauldron.getType();

        if (type == Material.WATER_CAULDRON) {
            if (!plugin.pluginConfig().cauldronWaterEnabled()) return;
            if (!Heat.hasHeatSourceBelow(cauldron)) return; // kein kochendes Wasser
            Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.CAULDRON_WATER, stack);
            if (recipe.isEmpty()) return;
            if (plugin.cooking().startFloatingCook(cauldron, CookingStation.CAULDRON_WATER, recipe.get(), entity)) {
                plugin.effects().sizzle(cauldron.getLocation(), false);
                plugin.effects().boil(cauldron.getLocation(), 8);
            }
        } else if (type == Material.LAVA_CAULDRON) {
            if (!plugin.pluginConfig().cauldronLavaEnabled()) return;
            Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.CAULDRON_LAVA, stack);
            if (recipe.isEmpty()) return;
            ItemStack input = stack.clone();
            input.setAmount(1);
            if (plugin.cooking().startBlockCook(cauldron, CookingStation.CAULDRON_LAVA, recipe.get(), input)) {
                entity.remove();
                plugin.effects().sizzle(cauldron.getLocation(), true);
                plugin.effects().fry(cauldron.getLocation(), 8);
            }
        }
    }

    /** Schwebende, kochende Items nicht von Spielern aufsammeln lassen. */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (plugin.cooking().isFloatingCook(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Schwebende, kochende Items nicht von Hoppern aufsaugen lassen. */
    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryPickupItemEvent event) {
        if (plugin.cooking().isFloatingCook(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Findet den Kessel, in bzw. auf dem das Item liegt (oder {@code null}). */
    private Block findCauldron(Item entity) {
        Block at = entity.getLocation().getBlock();
        if (isCauldron(at)) return at;
        Block below = at.getRelative(0, -1, 0);
        if (isCauldron(below)) return below;
        return null;
    }

    private boolean isCauldron(Block block) {
        return block.getType() == Material.WATER_CAULDRON || block.getType() == Material.LAVA_CAULDRON;
    }
}
