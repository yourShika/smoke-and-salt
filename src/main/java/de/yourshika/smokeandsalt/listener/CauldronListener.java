package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.util.Heat;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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

    private static final long[] LANDING_CHECKS = {2L, 6L, 12L};

    private final SmokeAndSalt plugin;

    public CauldronListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.pluginConfig().cookingEnabled()) return;
        if (!event.getPlayer().hasPermission("smokeandsalt.use")) return;
        Item entity = event.getItemDrop();
        // Mehrere kurze Checks: Lava kann Items sehr schnell verbrennen.
        for (long delay : LANDING_CHECKS) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> handleLanded(entity), delay);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        if (!isFireDamage(event.getCause())) return;
        if (!protectLavaIngredient(item)) return;
        event.setCancelled(true);
        item.setFireTicks(0);
        handleLanded(item);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        if (!protectLavaIngredient(item)) return;
        event.setCancelled(true);
        item.setFireTicks(0);
    }

    private void handleLanded(Item entity) {
        if (entity == null || !entity.isValid() || entity.isDead()) return;
        Block cauldron = findCauldron(entity);
        if (cauldron == null) return;
        if (!plugin.pluginConfig().isWorldAllowed(cauldron.getWorld().getName())) return;

        Material type = cauldron.getType();

        if (type == Material.WATER_CAULDRON) {
            if (!plugin.pluginConfig().cauldronWaterEnabled()) return;
            if (!Heat.hasHeatSourceBelow(cauldron)) return; // kein kochendes Wasser
            // Zutat in den kochenden Kessel aufnehmen (Sammel-/Kombinations-Logik).
            plugin.cauldron().tryAdd(cauldron, entity);
        } else if (type == Material.LAVA_CAULDRON) {
            if (!plugin.pluginConfig().cauldronLavaEnabled()) return;
            if (plugin.cooking().isBusy(cauldron)) return;
            ItemStack stack = entity.getItemStack();
            Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.CAULDRON_LAVA, stack);
            if (recipe.isEmpty()) return;
            entity.setFireTicks(0);
            ItemStack input = stack.clone();
            input.setAmount(1);
            if (plugin.cooking().startBlockCook(cauldron, CookingStation.CAULDRON_LAVA, recipe.get(), input)) {
                dropExtra(entity);
                entity.remove();
                plugin.effects().sizzle(cauldron.getLocation(), true);
                plugin.effects().fry(cauldron.getLocation(), 8);
            }
        }
    }

    /** Rechtsklick auf einen Wasserkessel bricht einen laufenden Vorgang ab. */
    @EventHandler
    public void onCancel(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.cauldron().tryServe(block, event.getPlayer())) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "cauldron.served");
            return;
        }
        if (plugin.cauldron().isServing(block) && isCauldronLiquidTool(hand.getType())) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "cauldron.serving");
            return;
        }
        if (plugin.cauldron().cancel(block, event.getPlayer())) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "cauldron.cancelled");
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

    private boolean isCauldronLiquidTool(Material material) {
        return material == Material.BUCKET
                || material == Material.WATER_BUCKET
                || material == Material.GLASS_BOTTLE;
    }

    private boolean protectLavaIngredient(Item item) {
        if (!plugin.pluginConfig().cookingEnabled() || !plugin.pluginConfig().cauldronLavaEnabled()) return false;
        Block cauldron = findCauldron(item);
        if (cauldron == null || cauldron.getType() != Material.LAVA_CAULDRON) return false;
        if (!plugin.pluginConfig().isWorldAllowed(cauldron.getWorld().getName())) return false;
        return plugin.cooking().registry().find(CookingStation.CAULDRON_LAVA, item.getItemStack()).isPresent();
    }

    private boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR;
    }

    private void dropExtra(Item entity) {
        ItemStack stack = entity.getItemStack();
        if (stack.getAmount() <= 1 || entity.getWorld() == null) return;
        ItemStack extra = stack.clone();
        extra.setAmount(stack.getAmount() - 1);
        Item rem = entity.getWorld().dropItem(entity.getLocation().add(0.35, 0.35, 0), extra);
        rem.setVelocity(new org.bukkit.util.Vector(0.28, 0.22, 0));
    }
}
