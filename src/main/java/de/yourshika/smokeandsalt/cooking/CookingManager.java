package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.item.ItemKeys;
import de.yourshika.smokeandsalt.util.Effects;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale Steuerung aller laufenden Koch-Vorgaenge. Fuehrt einen Tick-Task, der
 * jeden aktiven {@link ActiveCook} fortschreibt, die passenden Partikel spawnt und
 * bei Fertigstellung das Ergebnis ausgibt.
 *
 * <p>Vorgaenge werden bewusst nur im Speicher gehalten. Bei einem Server-Neustart
 * werden laufende Vorgaenge abgebrochen und die eingelegten Zutaten
 * zurueckgegeben, damit nichts verloren geht.</p>
 */
public final class CookingManager {

    /** Der Tick-Task laeuft alle 2 Server-Ticks und rechnet entsprechend hoch. */
    private static final int TICK_INTERVAL = 2;

    private final SmokeAndSalt plugin;
    private final CookingRegistry registry;
    private final Effects effects;
    private final ItemKeys keys;

    /** Block-basierte Vorgaenge (Smoker, Lagerfeuer, Kessel) - Schluessel: Block-Key. */
    private final Map<String, ActiveCook> active = new LinkedHashMap<>();

    private BukkitTask tickTask;

    public CookingManager(SmokeAndSalt plugin, CookingRegistry registry, Effects effects, ItemKeys keys) {
        this.plugin = plugin;
        this.registry = registry;
        this.effects = effects;
        this.keys = keys;
    }

    /** Ist diese Entity ein im Kessel schwebendes, kochendes Item? */
    public boolean isFloatingCook(Item item) {
        return item.getPersistentDataContainer().has(keys.cookingFloat, PersistentDataType.BYTE);
    }

    public CookingRegistry registry() {
        return registry;
    }

    public int activeCount() {
        return active.size();
    }

    public void start() {
        if (tickTask != null) return;
        tickTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        // Alle laufenden Vorgaenge sauber abbrechen und Zutaten zurueckgeben.
        for (ActiveCook cook : active.values()) {
            refund(cook);
        }
        active.clear();
    }

    /** Laeuft an diesem Block bereits ein Vorgang? */
    public boolean isBusy(Block block) {
        return active.containsKey(key(block));
    }

    /**
     * Startet einen block-basierten Koch-Vorgang (Smoker, Lagerfeuer, Wasserkessel
     * ohne Schweben). Der Aufrufer hat die Zutat bereits aus der Hand entfernt und
     * uebergibt hier eine 1er-Kopie zur moeglichen Rueckerstattung.
     */
    public boolean startBlockCook(Block block, CookingStation station, CookingRecipe recipe, ItemStack input) {
        if (isBusy(block)) return false;
        active.put(key(block), new ActiveCook(block, station, recipe, null, input));
        return true;
    }

    /**
     * Startet einen schwebenden Kessel-Vorgang. Das gedroppte {@link Item} wird an
     * der Wasseroberflaeche gehalten (keine Schwerkraft, kein Aufheben).
     */
    public boolean startFloatingCook(Block cauldron, CookingStation station, CookingRecipe recipe, Item item) {
        if (isBusy(cauldron)) return false;
        freeze(item, cauldron);
        active.put(key(cauldron), new ActiveCook(cauldron, station, recipe, item, item.getItemStack()));
        return true;
    }

    private void tick() {
        if (active.isEmpty()) return;
        Iterator<Map.Entry<String, ActiveCook>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            ActiveCook cook = it.next().getValue();

            if (!isStillValid(cook)) {
                refund(cook);
                it.remove();
                continue;
            }

            spawnParticles(cook);

            if (cook.tick(TICK_INTERVAL)) {
                complete(cook);
                it.remove();
            } else if (cook.floatingItem() != null) {
                // Schwebendes Item weiterhin an der Oberflaeche halten.
                keepFloating(cook.floatingItem(), cook.block());
            }
        }
    }

    private void spawnParticles(ActiveCook cook) {
        Location loc = cook.effectLocation();
        switch (cook.station()) {
            case SMOKER, CAMPFIRE -> effects.smoke(loc, 3);
            case CAULDRON_WATER -> effects.boil(loc, 4);
            case CAULDRON_LAVA -> effects.fry(loc, 4);
            case CUTTING -> {
                // Schneiden laeuft ohne dauerhaften Block-Vorgang.
            }
        }
    }

    private void complete(ActiveCook cook) {
        Location loc = cook.block().getLocation();
        // Schwebendes Item entfernen.
        if (cook.floatingItem() != null && cook.floatingItem().isValid()) {
            cook.floatingItem().remove();
        }
        ItemStack result = registry.buildResult(cook.recipe());
        if (result != null && loc.getWorld() != null) {
            Item drop = loc.getWorld().dropItem(loc.clone().add(0.5, 1.1, 0.5), result);
            drop.setVelocity(new Vector(0, 0.15, 0));
        }
        effects.finish(loc);
    }

    private void refund(ActiveCook cook) {
        Location loc = cook.block().getLocation();
        if (cook.floatingItem() != null && cook.floatingItem().isValid()) {
            // Schwebendes Item wieder normal freigeben.
            unfreeze(cook.floatingItem());
            return;
        }
        if (cook.input() != null && loc.getWorld() != null) {
            loc.getWorld().dropItem(loc.clone().add(0.5, 1.1, 0.5), cook.input().clone());
        }
    }

    /**
     * Prueft, ob der Vorgang noch gueltig ist: Block existiert noch als passende
     * Station und (fuer schwebende Vorgaenge) das Item lebt noch.
     */
    private boolean isStillValid(ActiveCook cook) {
        Block block = cook.block();
        if (cook.floatingItem() != null && (!cook.floatingItem().isValid() || cook.floatingItem().isDead())) {
            return false;
        }
        return switch (cook.station()) {
            case SMOKER -> block.getType() == org.bukkit.Material.SMOKER;
            case CAMPFIRE -> block.getType() == org.bukkit.Material.CAMPFIRE
                    || block.getType() == org.bukkit.Material.SOUL_CAMPFIRE;
            case CAULDRON_WATER -> block.getType() == org.bukkit.Material.WATER_CAULDRON;
            case CAULDRON_LAVA -> block.getType() == org.bukkit.Material.LAVA_CAULDRON;
            case CUTTING -> true;
        };
    }

    private void freeze(Item item, Block cauldron) {
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setUnlimitedLifetime(true);
        item.setCanMobPickup(false);
        item.getPersistentDataContainer().set(keys.cookingFloat, PersistentDataType.BYTE, (byte) 1);
        keepFloating(item, cauldron);
    }

    private void unfreeze(Item item) {
        item.setGravity(true);
        item.setPickupDelay(20);
        item.setUnlimitedLifetime(false);
        item.getPersistentDataContainer().remove(keys.cookingFloat);
    }

    private void keepFloating(Item item, Block cauldron) {
        Location surface = cauldron.getLocation().add(0.5, 0.85, 0.5);
        if (item.getLocation().distanceSquared(surface) > 0.01) {
            item.teleport(surface);
        }
        item.setVelocity(new Vector(0, 0, 0));
    }

    private String key(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
