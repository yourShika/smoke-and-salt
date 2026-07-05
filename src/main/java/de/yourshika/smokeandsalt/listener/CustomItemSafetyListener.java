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

        if (def == null
                || def.food() == null
                || event.getAction() == Action.RIGHT_CLICK_BLOCK
                || THROWABLES.contains(item.getType())) {
            event.setUseItemInHand(Event.Result.DENY);
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
