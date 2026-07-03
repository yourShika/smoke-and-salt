package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Schneide-Station: Axt in der Haupthand + Zutat in der Zweithand, dann
 * Rechtsklick. Loest einen Schnitt-Effekt (Partikel + Sound) aus und verwandelt
 * eine Zutat gemaess Rezept in das Ergebnis. Ohne passendes Rezept passiert
 * nichts Besonderes.
 */
public final class CuttingListener implements Listener {

    private final SmokeAndSalt plugin;
    /** Kurze Abkling-Zeit pro Spieler, um Doppel-Ausloesen zu vermeiden. */
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    public CuttingListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        var config = plugin.pluginConfig();
        if (!config.cookingEnabled() || !config.cuttingEnabled()) return;
        if (!player.hasPermission("smokeandsalt.use")) return;
        if (!config.isWorldAllowed(player.getWorld().getName())) return;

        ItemStack main = player.getInventory().getItemInMainHand();
        if (!isAxe(main.getType())) return;
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType().isAir()) return;

        Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.CUTTING, off);
        if (recipe.isEmpty()) return;

        long now = System.currentTimeMillis();
        Long last = cooldown.get(player.getUniqueId());
        if (last != null && now - last < 400) {
            event.setCancelled(true);
            return;
        }
        cooldown.put(player.getUniqueId(), now);
        event.setCancelled(true);

        // Schnitt-Effekt an der Zweithand-Position.
        plugin.effects().cut(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.6)));

        // Eine Zutat verbrauchen und Ergebnis ausgeben.
        off.setAmount(off.getAmount() - 1);
        ItemStack result = plugin.cooking().registry().buildResult(recipe.get());
        if (result != null) {
            var leftover = player.getInventory().addItem(result);
            leftover.values().forEach(stack ->
                    player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
        // Leichte Werkzeug-Abnutzung fuer die Axt.
        damageAxe(player, main);
    }

    private boolean isAxe(Material material) {
        return Tag.ITEMS_AXES.isTagged(material);
    }

    private void damageAxe(Player player, ItemStack axe) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (!(axe.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg)) return;
        int max = axe.getType().getMaxDurability();
        if (max <= 0) return;
        dmg.setDamage(Math.min(max - 1, dmg.getDamage() + 1));
        axe.setItemMeta(dmg);
    }
}
