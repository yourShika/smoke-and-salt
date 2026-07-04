package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Schneide-Station: Axt in einer Hand + Zutat in der anderen, dann Rechtsklick.
 * Loest einen kurzen Schnitt-Vorgang aus und verwandelt eine Zutat gemaess
 * Rezept in das Ergebnis. Ohne passendes Rezept passiert nichts Besonderes.
 */
public final class CuttingListener implements Listener {

    private final SmokeAndSalt plugin;
    /** Kurze Abkling-Zeit pro Spieler, um Doppel-Ausloesen zu vermeiden. */
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> active = new ConcurrentHashMap<>();

    public CuttingListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        var config = plugin.pluginConfig();
        if (!config.cookingEnabled() || !config.cuttingEnabled()) return;
        if (!player.hasPermission("smokeandsalt.use")) return;
        if (!config.isWorldAllowed(player.getWorld().getName())) return;

        CutSetup setup = findSetup(player);
        if (setup == null) return;

        Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.CUTTING, setup.ingredient());
        if (recipe.isEmpty()) return;

        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        UUID id = player.getUniqueId();
        if (active.containsKey(id)) return;

        long now = System.currentTimeMillis();
        Long last = cooldown.get(id);
        if (last != null && now - last < 400) {
            return;
        }
        cooldown.put(id, now);

        // Schnitt-Effekt vor dem Spieler; Abschluss folgt nach der Rezeptdauer.
        plugin.effects().cut(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.6)));
        int duration = Math.max(20, recipe.get().durationTicks());
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> finishCut(id, recipe.get(), setup), duration);
        active.put(id, task);
        scheduleCutEffects(id, player, duration);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (active.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        CutSetup setup = findSetup(player);
        if (setup == null) return;
        if (plugin.cooking().registry().find(CookingStation.CUTTING, event.getItem()).isPresent()) {
            event.setCancelled(true);
        }
    }

    private void finishCut(UUID id, CookingRecipe recipe, CutSetup setup) {
        active.remove(id);
        Player player = plugin.getServer().getPlayer(id);
        if (player == null || !player.isOnline()) return;

        ItemStack axe = itemIn(player, setup.axeHand());
        ItemStack ingredient = itemIn(player, setup.ingredientHand());
        if (!isAxe(axe.getType())) return;
        Optional<CookingRecipe> current = plugin.cooking().registry().find(CookingStation.CUTTING, ingredient);
        if (current.isEmpty() || !current.get().id().equals(recipe.id())) return;

        // Eine Zutat verbrauchen und Ergebnis in zufaelliger Menge ausgeben.
        ingredient.setAmount(ingredient.getAmount() - 1);
        ItemStack result = plugin.cooking().registry().buildResult(recipe);
        if (result != null) {
            result.setAmount(recipe.rollResultAmount());
            var leftover = player.getInventory().addItem(result);
            leftover.values().forEach(stack ->
                    player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
        // Zufaellige Werkzeug-Abnutzung (Standard 1-5, konfigurierbar).
        damageAxe(player, axe);
        plugin.effects().cut(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.6)));
    }

    private void scheduleCutEffects(UUID id, Player player, int duration) {
        for (int delay = 10; delay < duration; delay += 10) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!active.containsKey(id) || !player.isOnline()) return;
                plugin.effects().cut(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.6)));
            }, delay);
        }
    }

    private CutSetup findSetup(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean mainAxe = isAxe(main.getType());
        boolean offAxe = isAxe(off.getType());
        if (mainAxe == offAxe) return null;
        EquipmentSlot axeHand = mainAxe ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        EquipmentSlot ingredientHand = mainAxe ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ItemStack ingredient = ingredientHand == EquipmentSlot.HAND ? main : off;
        if (ingredient == null || ingredient.getType().isAir()) return null;
        return new CutSetup(axeHand, ingredientHand, ingredient);
    }

    private boolean isAxe(Material material) {
        return Tag.ITEMS_AXES.isTagged(material);
    }

    private void damageAxe(Player player, ItemStack axe) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (!(axe.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg)) return;
        int max = axe.getType().getMaxDurability();
        if (max <= 0) return;
        int lo = Math.max(1, plugin.getConfig().getInt("cooking.cutting.durability-min", 1));
        int hi = Math.max(lo, plugin.getConfig().getInt("cooking.cutting.durability-max", 5));
        int amount = lo + (int) (Math.random() * (hi - lo + 1));
        dmg.setDamage(Math.min(max - 1, dmg.getDamage() + amount));
        axe.setItemMeta(dmg);
    }

    private ItemStack itemIn(Player player, EquipmentSlot slot) {
        return slot == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
    }

    private record CutSetup(EquipmentSlot axeHand, EquipmentSlot ingredientHand, ItemStack ingredient) {
    }
}
