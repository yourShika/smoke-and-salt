package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CauldronStation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Kessel-Station (Wasser + Lava). Zutaten koennen auf zwei Wegen eingelegt werden:
 *
 * <ul>
 *   <li><b>Hineinwerfen</b> - ein passendes Item, das auf den Kessel fallengelassen
 *       wird, landet im persistenten Container.</li>
 *   <li><b>Schleichen + Rechtsklick</b> - oeffnet die kleine Kessel-GUI (Schmelzofen-
 *       Prinzip), in der man Items einlegen, den Zustand sehen und Ergebnisse
 *       entnehmen kann.</li>
 * </ul>
 *
 * Der Kessel arbeitet die Zutaten anschliessend Charge fuer Charge ab.
 */
public final class CauldronListener implements Listener {

    private static final long[] LANDING_CHECKS = {2L, 6L, 12L};

    private final SmokeAndSalt plugin;

    public CauldronListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    // --- Hineinwerfen --------------------------------------------------------

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

        CauldronStation station = stationFor(cauldron);
        if (station == null) return;
        ItemStack stack = entity.getItemStack();
        if (!station.acceptsIngredient(stack)) return;

        entity.setFireTicks(0);
        ItemStack leftover = station.deposit(cauldron, stack);
        if (leftover == null || leftover.getType().isAir()) {
            entity.remove();
        } else if (leftover.getAmount() != stack.getAmount()) {
            entity.setItemStack(leftover);
        }
    }

    // --- GUI oeffnen (Schleichen + Rechtsklick) ------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        CauldronStation station = stationFor(block);
        if (station == null) return;
        if (!plugin.pluginConfig().cookingEnabled()) return;
        if (!player.hasPermission("smokeandsalt.use")) return;
        if (!plugin.pluginConfig().isWorldAllowed(block.getWorld().getName())) return;

        event.setCancelled(true);
        station.open(player, block);
    }

    /** Wird der Kessel abgebaut, den Container-Inhalt sicher ausgeben. */
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        CauldronStation station = stationFor(block);
        if (station != null) {
            station.dropAndClear(block);
        }
    }

    // --- Hilfen --------------------------------------------------------------

    /** Aufgehaengte Anzeige-Items nicht von Spielern/Mobs aufsammeln lassen. */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (plugin.cooking().isFloatingCook(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** ... und auch nicht von Hoppern aufsaugen lassen. */
    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryPickupItemEvent event) {
        if (plugin.cooking().isFloatingCook(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Die zustaendige Station fuer diesen Block (oder {@code null}). */
    private CauldronStation stationFor(Block block) {
        Material type = block.getType();
        boolean water = plugin.pluginConfig().cauldronWaterEnabled();
        boolean lava = plugin.pluginConfig().cauldronLavaEnabled();
        if (type == Material.WATER_CAULDRON && water) {
            return plugin.cauldron();
        }
        if (type == Material.LAVA_CAULDRON && lava) {
            return plugin.lavaCauldron();
        }
        if (type == Material.CAULDRON) {
            // Leerer Kessel: dem bestehenden Container folgen, sonst Wasser bevorzugen.
            if (lava && plugin.lavaCauldron().hasStation(block)) return plugin.lavaCauldron();
            if (water) return plugin.cauldron();
            if (lava) return plugin.lavaCauldron();
        }
        return null;
    }

    private Block findCauldron(Item entity) {
        Block at = entity.getLocation().getBlock();
        if (isCauldron(at)) return at;
        Block below = at.getRelative(0, -1, 0);
        if (isCauldron(below)) return below;
        return null;
    }

    private boolean isCauldron(Block block) {
        return block.getType() == Material.WATER_CAULDRON
                || block.getType() == Material.LAVA_CAULDRON
                || block.getType() == Material.CAULDRON;
    }

    private boolean protectLavaIngredient(Item item) {
        if (!plugin.pluginConfig().cookingEnabled() || !plugin.pluginConfig().cauldronLavaEnabled()) return false;
        Block cauldron = findCauldron(item);
        if (cauldron == null || cauldron.getType() != Material.LAVA_CAULDRON) return false;
        if (!plugin.pluginConfig().isWorldAllowed(cauldron.getWorld().getName())) return false;
        return plugin.lavaCauldron().acceptsIngredient(item.getItemStack());
    }

    private boolean isFireDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR;
    }
}
