package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.content.RecipeMatch;
import de.yourshika.smokeandsalt.util.Heat;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
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
    private final Map<String, ServingPot> servings = new LinkedHashMap<>();

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

    public boolean contains(String id) {
        if (id == null) return false;
        return recipes.stream().anyMatch(recipe -> recipe.id().equalsIgnoreCase(id));
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
        servings.clear();
    }

    /**
     * Versucht, ein hineingeworfenes Item in den Kessel aufzunehmen. Gibt
     * {@code true} zurueck, wenn das Item Teil eines Rezepts ist und aufgenommen
     * wurde (dann schwebt es und wird nicht mehr normal fallengelassen).
     */
    public boolean tryAdd(Block cauldron, Item entity) {
        if (recipes.isEmpty()) return false;
        if (serving(cauldron) != null) return false;
        Pot pot = pots.get(key(cauldron));
        if (pot != null && pot.cooking != null) return false; // kocht gerade

        ItemStack stack = entity.getItemStack();
        int take = countToTake(pot, stack);
        if (take <= 0) return false;

        if (pot == null) {
            pot = new Pot(cauldron);
            pots.put(key(cauldron), pot);
        }

        if (stack.getAmount() > take && entity.getWorld() != null) {
            ItemStack extra = stack.clone();
            extra.setAmount(stack.getAmount() - take);
            Item rem = entity.getWorld().dropItem(entity.getLocation().add(0, 0.25, 0), extra);
            rem.setVelocity(new Vector(0, 0.2, 0));
        }

        ItemStack one = stack.clone();
        one.setAmount(1);
        entity.setItemStack(one);
        freeze(entity);
        pot.entities.add(entity);

        for (int i = 1; i < take && entity.getWorld() != null; i++) {
            Item copy = entity.getWorld().dropItem(entity.getLocation(), one.clone());
            freeze(copy);
            pot.entities.add(copy);
        }
        arrange(pot);

        List<ItemStack> candidate = stacks(pot);
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

    private int countToTake(Pot pot, ItemStack stack) {
        int maxRecipeSize = recipes.stream().mapToInt(r -> r.ingredients().size()).max().orElse(1);
        int existing = pot == null ? 0 : pot.entities.size();
        int limit = Math.min(stack.getAmount(), Math.max(1, maxRecipeSize - existing));
        int best = 0;
        for (int take = 1; take <= limit; take++) {
            List<ItemStack> candidate = candidateStacks(pot, stack, take);
            boolean fits = recipes.stream().anyMatch(r -> RecipeMatch.partial(plugin, candidate, r.ingredients()));
            if (fits) best = take;
        }
        return best;
    }

    private List<ItemStack> candidateStacks(Pot pot, ItemStack stack, int take) {
        List<ItemStack> candidate = new ArrayList<>();
        if (pot != null) {
            for (Item existing : pot.entities) candidate.add(existing.getItemStack());
        }
        for (int i = 0; i < take; i++) {
            ItemStack one = stack.clone();
            one.setAmount(1);
            candidate.add(one);
        }
        return candidate;
    }

    private List<ItemStack> stacks(Pot pot) {
        List<ItemStack> out = new ArrayList<>();
        for (Item entity : pot.entities) out.add(entity.getItemStack());
        return out;
    }

    /**
     * Bricht einen laufenden Sammel-/Koch-Vorgang an diesem Kessel ab und gibt
     * die enthaltenen Zutaten an den Spieler zurueck. Gibt {@code true}, wenn es
     * etwas abzubrechen gab.
     */
    public boolean cancel(Block cauldron, org.bukkit.entity.Player player) {
        if (serving(cauldron) != null) return false;
        Pot pot = pots.remove(key(cauldron));
        if (pot == null) return false;
        for (Item entity : pot.entities) {
            if (!entity.isValid()) continue;
            ItemStack stack = entity.getItemStack();
            entity.remove();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        }
        pot.entities.clear();
        plugin.effects().sizzle(cauldron.getLocation(), false);
        return true;
    }

    /** Gibt eine fertige Kessel-Suppe mit einer Bowl aus und senkt den Wasserstand. */
    public boolean tryServe(Block cauldron, Player player) {
        ServingPot serving = serving(cauldron);
        if (serving == null) return false;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.BOWL) return false;

        if (player.getGameMode() != GameMode.CREATIVE) {
            hand.setAmount(hand.getAmount() - 1);
        }
        ItemStack result = serving.result.build(plugin);
        if (result != null) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
            leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        }
        plugin.effects().sizzle(cauldron.getLocation(), false);
        if (reduceWater(cauldron, 1) <= 0) {
            servings.remove(key(cauldron));
        }
        return true;
    }

    /** Laeuft in diesem Kessel gerade ein fertiges, bowl-portionierbares Ergebnis? */
    public boolean isServing(Block cauldron) {
        return serving(cauldron) != null;
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

        if (pot.cooking.serveWithBowl()) {
            servings.put(key(block), new ServingPot(block, pot.cooking.result()));
            plugin.effects().finish(block.getLocation());
            return;
        }

        ItemStack result = pot.cooking.result().build(plugin);
        if (result != null && out.getWorld() != null) {
            Item drop = out.getWorld().dropItem(out, result);
            drop.setVelocity(new Vector(0, 0.15, 0));
        }
        reduceWater(block, pot.cooking.waterCost());
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

    /** Ordnet die schwebenden Zutaten kreisfoermig knapp unter der Wasseroberflaeche an. */
    private void arrange(Pot pot) {
        int count = pot.entities.size();
        // Etwas tiefer, damit die Zutaten sichtbar IM Wasser liegen.
        Location center = pot.block.getLocation().add(0.5, 0.55, 0.5);
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

    private ServingPot serving(Block block) {
        String key = key(block);
        ServingPot serving = servings.get(key);
        if (serving != null && block.getType() != Material.WATER_CAULDRON) {
            servings.remove(key);
            return null;
        }
        return serving;
    }

    private int reduceWater(Block block, int levels) {
        if (levels <= 0 || block.getType() != Material.WATER_CAULDRON) {
            return currentWaterLevel(block);
        }
        int current = currentWaterLevel(block);
        int next = Math.max(0, current - levels);
        if (next <= 0) {
            block.setType(Material.CAULDRON, false);
            return 0;
        }
        if (block.getBlockData() instanceof Levelled levelled) {
            levelled.setLevel(next);
            block.setBlockData(levelled, false);
        }
        return next;
    }

    private int currentWaterLevel(Block block) {
        if (block.getType() == Material.WATER_CAULDRON && block.getBlockData() instanceof Levelled levelled) {
            return levelled.getLevel();
        }
        return 0;
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

    private record ServingPot(Block block, de.yourshika.smokeandsalt.content.ResultSpec result) {
    }
}
