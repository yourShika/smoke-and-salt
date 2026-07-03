package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.chain.ChainManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Ketten-Funktion: Ketten dienen als Aufhaengung fuer Kessel-Behang oder
 * Raeucherware. Rechtsklick auf eine Kette mit einem Item haengt es darunter auf;
 * Rechtsklick mit leerer Hand nimmt es wieder ab.
 */
public final class ChainListener implements Listener {

    private final SmokeAndSalt plugin;
    private final ChainManager chains;

    public ChainListener(SmokeAndSalt plugin, ChainManager chains) {
        this.plugin = plugin;
        this.chains = chains;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.pluginConfig().chainEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || !Tag.CHAINS.isTagged(block.getType())) return;
        if (!event.getPlayer().hasPermission("smokeandsalt.use")) return;
        if (!plugin.pluginConfig().isWorldAllowed(block.getWorld().getName())) return;

        var player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            // Abnehmen.
            if (!chains.isHung(block)) return;
            event.setCancelled(true);
            ItemStack stack = chains.retrieve(block);
            if (stack != null) {
                var leftover = player.getInventory().addItem(stack);
                leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
                plugin.messages().send(player, "chain.removed");
            }
        } else {
            // Aufhaengen.
            if (chains.isHung(block)) {
                plugin.messages().send(player, "chain.occupied");
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            ItemStack toHang = hand.clone();
            toHang.setAmount(1);
            if (chains.hang(block, toHang)) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    hand.setAmount(hand.getAmount() - 1);
                }
                plugin.messages().send(player, "chain.hung");
            }
        }
    }

    /** Aufgehaengte Items nicht von Spielern aufsammeln lassen. */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (chains.isHungEntity(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Aufgehaengte Items nicht von Hoppern etc. aufsaugen lassen. */
    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryPickupItemEvent event) {
        if (chains.isHungEntity(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
