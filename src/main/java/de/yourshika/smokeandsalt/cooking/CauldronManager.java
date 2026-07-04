package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.content.RecipeMatch;
import de.yourshika.smokeandsalt.util.Heat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verwaltet das Kochen im Wasserkessel. Ein kochender Kessel (Wasser + Waermequelle
 * darunter) sammelt hineingeworfene Zutaten, die an der Oberflaeche schweben. Sobald
 * die Kombination einem {@link CauldronRecipe} entspricht, wird gekocht und das
 * Ergebnis ausgegeben.
 */
public final class CauldronManager {

    private static final int TICK_INTERVAL = 3;

    private final SmokeAndSalt plugin;
    private final List<CauldronRecipe> recipes = new ArrayList<>();
    private final Map<String, Pot> pots = new LinkedHashMap<>();

    private BukkitTask task;

    public CauldronManager(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    public void register(CauldronRecipe recipe) {
        recipes.add(recipe);
    }

    public void clearRecipes() {
        recipes.clear();
    }

    public List<CauldronRecipe> recipes() {
        return recipes;
    }

    public int size() {
        return recipes.size();
    }

    public void start() {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Pot pot : pots.values()) {
            releaseEntities(pot);
        }
        pots.clear();
    }

    /**
     * Versucht, ein hineingeworfenes Item in den Kessel aufzunehmen. Gibt
     * {@code true} zurueck, wenn das Item Teil eines Rezepts ist und aufgenommen
     * wurde (dann schwebt es und wird nicht mehr normal fallengelassen).
     */
    public boolean tryAdd(Block cauldron, Item entity) {
        if (recipes.isEmpty()) return false;
        Pot pot = pots.get(key(cauldron));
        if (pot != null && pot.cooking != null) return false; // kocht gerade

        List<ItemStack> candidate = new ArrayList<>();
        if (pot != null) {
            for (Item existing : pot.entities) candidate.add(existing.getItemStack());
        }
        candidate.add(entity.getItemStack());

        // Nur aufnehmen, wenn die Kombination Teil (oder Ganzes) eines Rezepts ist.
        boolean fits = recipes.stream().anyMatch(r -> RecipeMatch.partial(plugin, candidate, r.ingredients()));
        if (!fits) return false;

        if (pot == null) {
            pot = new Pot(cauldron);
            pots.put(key(cauldron), pot);
        }
        // Nur ein einzelnes Stueck aufnehmen, den Rest zurueckgeben.
        ItemStack stack = entity.getItemStack();
        if (stack.getAmount() > 1 && entity.getWorld() != null) {
            ItemStack extra = stack.clone();
            extra.setAmount(stack.getAmount() - 1);
            ItemStack one = stack.clone();
            one.setAmount(1);
            entity.setItemStack(one);
            Item rem = entity.getWorld().dropItem(entity.getLocation().add(0, 0.25, 0), extra);
            rem.setVelocity(new Vector(0, 0.2, 0));
        }
        freeze(entity);
        pot.entities.add(entity);
        arrange(pot);

        CauldronRecipe complete = recipes.stream()
                .filter(r -> RecipeMatch.exact(plugin, candidate, r.ingredients()))
                .findFirst().orElse(null);
        if (complete != null) {
            pot.cooking = complete;
            pot.elapsed = 0;
            plugin.effects().sizzle(cauldron.getLocation(), false);
        }
        plugin.effects().boil(cauldron.getLocation(), 8);
        return true;
    }

    private void tick() {
        if (pots.isEmpty()) return;
        Iterator<Map.Entry<String, Pot>> it = pots.entrySet().iterator();
        while (it.hasNext()) {
            Pot pot = it.next().getValue();
            Block block = pot.block;

            if (block.getType() != Material.WATER_CAULDRON || !Heat.hasHeatSourceBelow(block)) {
                releaseEntities(pot); // nicht mehr kochend -> Zutaten freigeben
                it.remove();
                continue;
            }
            // Tote Entities entfernen.
            pot.entities.removeIf(e -> !e.isValid() || e.isDead());
            if (pot.entities.isEmpty() && pot.cooking == null) {
                it.remove();
                continue;
            }

            plugin.effects().boil(block.getLocation(), 3);
            arrange(pot);

            if (pot.cooking != null) {
                pot.elapsed += TICK_INTERVAL;
                if (pot.elapsed >= pot.cooking.duration()) {
                    complete(pot);
                    it.remove();
                }
            }
        }
    }

    private void complete(Pot pot) {
        Block block = pot.block;
        Location out = block.getLocation().add(0.5, 1.05, 0.5);
        // Zutaten entfernen, Behaelter-Reste (Eimer) zurueckgeben.
        for (Item entity : pot.entities) {
            if (entity.isValid()) {
                ItemStack remainder = remainderOf(entity.getItemStack());
                entity.remove();
                if (remainder != null && out.getWorld() != null) {
                    out.getWorld().dropItem(out, remainder);
                }
            }
        }
        pot.entities.clear();

        ItemStack result = pot.cooking.result().build(plugin);
        if (result != null && out.getWorld() != null) {
            Item drop = out.getWorld().dropItem(out, result);
            drop.setVelocity(new Vector(0, 0.15, 0));
        }
        plugin.effects().finish(block.getLocation());
    }

    private void releaseEntities(Pot pot) {
        for (Item entity : pot.entities) {
            if (entity.isValid()) unfreeze(entity);
        }
        pot.entities.clear();
    }

    private void freeze(Item entity) {
        entity.setGravity(false);
        entity.setVelocity(new Vector(0, 0, 0));
        entity.setPickupDelay(Integer.MAX_VALUE);
        entity.setUnlimitedLifetime(true);
        entity.setCanMobPickup(false);
        entity.getPersistentDataContainer().set(plugin.keys().cookingFloat, PersistentDataType.BYTE, (byte) 1);
    }

    private void unfreeze(Item entity) {
        entity.setGravity(true);
        entity.setPickupDelay(20);
        entity.setUnlimitedLifetime(false);
        entity.getPersistentDataContainer().remove(plugin.keys().cookingFloat);
    }

    /** Ordnet die schwebenden Zutaten kreisfoermig an der Wasseroberflaeche an. */
    private void arrange(Pot pot) {
        int count = pot.entities.size();
        Location center = pot.block.getLocation().add(0.5, 0.85, 0.5);
        if (count == 1) {
            teleport(pot.entities.get(0), center);
            return;
        }
        double radius = 0.22;
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            Location loc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            teleport(pot.entities.get(i), loc);
        }
    }

    private void teleport(Item entity, Location loc) {
        if (entity.getLocation().distanceSquared(loc) > 0.01) {
            entity.teleport(loc);
        }
        entity.setVelocity(new Vector(0, 0, 0));
    }

    /** Behaelter-Rest einer Zutat (Milch-/Wassereimer -> leerer Eimer). */
    private ItemStack remainderOf(ItemStack stack) {
        return switch (stack.getType()) {
            case MILK_BUCKET, WATER_BUCKET, LAVA_BUCKET, POWDER_SNOW_BUCKET -> new ItemStack(Material.BUCKET);
            default -> null;
        };
    }

    private String key(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private static final class Pot {
        private final Block block;
        private final List<Item> entities = new ArrayList<>();
        private CauldronRecipe cooking;
        private int elapsed;

        private Pot(Block block) {
            this.block = block;
        }
    }
}
