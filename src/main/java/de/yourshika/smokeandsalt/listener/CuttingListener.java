package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schneide-Station: Axt in einer Hand + Zutat in der anderen. Man muss
 * <strong>durchgehend zuschlagen</strong> (Linksklick/Arm-Schwung); hoert man auf,
 * bricht der Vorgang ab. Die Gesamtdauer entspricht der Rezeptdauer.
 */
public final class CuttingListener implements Listener {

    /** Ohne einen Schwung innerhalb dieser Zeit bricht das Schneiden ab. */
    private static final long SWING_GRACE_MS = 500L;
    private static final int STEP = 2; // Ticks pro Fortschritt

    private final SmokeAndSalt plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public CuttingListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        Player player = event.getPlayer();
        var config = plugin.pluginConfig();
        if (!config.cookingEnabled() || !config.cuttingEnabled()) return;
        if (!player.hasPermission("smokeandsalt.use")) return;
        if (!config.isWorldAllowed(player.getWorld().getName())) return;

        UUID id = player.getUniqueId();
        Session session = sessions.get(id);
        if (session != null) {
            session.lastSwingMs = System.currentTimeMillis(); // weiter zuschlagen
            return;
        }

        CutSetup setup = findSetup(player);
        if (setup == null) return;
        Optional<CookingRecipe> recipe = plugin.cooking().registry().find(CookingStation.CUTTING, setup.ingredient());
        if (recipe.isEmpty()) return;

        Session started = new Session(recipe.get().id(), setup.axeHand(), setup.ingredientHand());
        started.lastSwingMs = System.currentTimeMillis();
        started.task = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> tick(id), STEP, STEP);
        sessions.put(id, started);
        plugin.effects().cut(cutLocation(player));
    }

    private void tick(UUID id) {
        Session session = sessions.get(id);
        if (session == null) return;
        Player player = plugin.getServer().getPlayer(id);
        if (player == null || !player.isOnline()) {
            end(id);
            return;
        }
        // Aufgehoert zuzuschlagen -> Abbruch.
        if (System.currentTimeMillis() - session.lastSwingMs > SWING_GRACE_MS) {
            end(id);
            return;
        }
        // Setup muss weiterhin passen (Anti-Dupe).
        CookingRecipe recipe = currentRecipe(player, session);
        if (recipe == null) {
            end(id);
            return;
        }
        session.progress += STEP;
        if (session.progress % 10 < STEP) {
            plugin.effects().cut(cutLocation(player));
        }
        int total = Math.max(20, recipe.durationTicks());
        int pct = Math.min(100, (int) (100.0 * session.progress / total));
        int bars = pct / 10;
        player.sendActionBar(plugin.messages().mini().deserialize(
                "<gray>Cutting <green>" + "|".repeat(bars) + "<dark_gray>" + "|".repeat(10 - bars)
                        + " <white>" + pct + "%"));
        if (session.progress >= total) {
            finish(player, session, recipe);
            end(id);
        }
    }

    private void finish(Player player, Session session, CookingRecipe recipe) {
        ItemStack axe = itemIn(player, session.axeHand);
        ItemStack ingredient = itemIn(player, session.ingredientHand);
        if (!isAxe(axe.getType()) || ingredient.getType().isAir()) return;

        // Zutat verbrauchen: bei Menge 1 den Slot leeren (nicht auf Menge 0 lassen).
        if (ingredient.getAmount() <= 1) {
            player.getInventory().setItem(session.ingredientHand, null);
        } else {
            ingredient.setAmount(ingredient.getAmount() - 1);
        }
        ItemStack result = plugin.cooking().registry().buildResult(recipe);
        if (result != null) {
            result.setAmount(recipe.rollResultAmount());
            var leftover = player.getInventory().addItem(result);
            leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        }
        damageAxe(player, axe);
        player.updateInventory();
        plugin.effects().cut(cutLocation(player));
    }

    /** Prueft, dass Axt + passende Zutat noch in den erwarteten Haenden liegen. */
    private CookingRecipe currentRecipe(Player player, Session session) {
        ItemStack axe = itemIn(player, session.axeHand);
        ItemStack ingredient = itemIn(player, session.ingredientHand);
        if (!isAxe(axe.getType()) || ingredient.getType().isAir()) return null;
        return plugin.cooking().registry().find(CookingStation.CUTTING, ingredient)
                .filter(r -> r.id().equals(session.recipeId))
                .orElse(null);
    }

    private void end(UUID id) {
        Session session = sessions.remove(id);
        if (session != null && session.task != null) session.task.cancel();
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (sessions.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        CutSetup setup = findSetup(player);
        if (setup == null) return;
        if (plugin.cooking().registry().find(CookingStation.CUTTING, event.getItem()).isPresent()) {
            event.setCancelled(true);
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

    private org.bukkit.Location cutLocation(Player player) {
        return player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.6));
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

    private static final class Session {
        private final String recipeId;
        private final EquipmentSlot axeHand;
        private final EquipmentSlot ingredientHand;
        private int progress;
        private long lastSwingMs;
        private BukkitTask task;

        private Session(String recipeId, EquipmentSlot axeHand, EquipmentSlot ingredientHand) {
            this.recipeId = recipeId;
            this.axeHand = axeHand;
            this.ingredientHand = ingredientHand;
        }
    }
}
