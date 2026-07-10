package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.item.ItemDefinition;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Schaltet Vanilla-Nebenfunktionen von Smoke-&-Salt-Custom-Items ab. Die Items
 * duerfen weiterhin gegessen und in Smoke-&-Salt-Rezepten verwendet werden, aber
 * ihr Basis-Material soll keine fremden Aktionen ausloesen.
 */
public final class CustomItemSafetyListener implements Listener {

    private static final Set<Material> THROWABLES = EnumSet.of(
            Material.EGG,
            Material.SNOWBALL,
            Material.ENDER_PEARL,
            Material.EXPERIENCE_BOTTLE,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.FIREWORK_ROCKET
    );

    /** Bloecke, die das Basis-Material eines Essens sonst VERBRAUCHEN wuerden (statt
     *  es essen zu lassen) - dort wird die Nutzung geblockt. Pflanzen auf Ackerland
     *  faengt separat {@link #onBlockPlace} ab. */
    private static final Set<Material> ITEM_CONSUMING_BLOCKS = EnumSet.of(
            Material.COMPOSTER
    );

    private final SmokeAndSalt plugin;

    public CustomItemSafetyListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        String id = plugin.items().idOf(item);
        if (id == null) return;
        ItemDefinition def = plugin.items().definition(id);

        if (isAllowedCookingUse(event, item)) return;

        // Nicht-Essen (reine Zutaten) und Wurf-Items: jede Nutzung unterbinden.
        if (def == null || def.food() == null || THROWABLES.contains(item.getType())) {
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        // Essbare Custom-Items sollen sich immer normal essen lassen - auch wenn man
        // dabei auf einen Block zeigt oder etwas in der Offhand haelt. Nur bei Bloecken,
        // die das Basis-Material sonst verbrauchen wuerden (z.B. Komposter), wird die
        // Nutzung geblockt.
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && ITEM_CONSUMING_BLOCKS.contains(block.getType())) {
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    /**
     * Custom-Items ohne eigene Nahrungswerte duerfen nicht gegessen/getrunken
     * werden, auch wenn ihr Basis-Material (z.B. HONEY_BOTTLE) essbar waere.
     * So bleibt z.B. Oel eine reine Zutat.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onConsume(PlayerItemConsumeEvent event) {
        String id = plugin.items().idOf(event.getItem());
        if (id == null) return;
        ItemDefinition def = plugin.items().definition(id);
        if (def == null || def.food() == null) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        if (isThrowableCustom(player.getInventory().getItemInMainHand())
                || isThrowableCustom(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.items().idOf(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    private boolean isThrowableCustom(ItemStack item) {
        return item != null
                && THROWABLES.contains(item.getType())
                && plugin.items().idOf(item) != null;
    }

    private boolean isAllowedCookingUse(PlayerInteractEvent event, ItemStack item) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        Block block = event.getClickedBlock();
        if (block == null) return false;
        if (block.getType() != Material.CAMPFIRE && block.getType() != Material.SOUL_CAMPFIRE) return false;
        return plugin.cooking().registry().find(CookingStation.CAMPFIRE, item).isPresent();
    }
}
