package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.content.Ingredient;
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

        if (stack.getAmount() > take) {
            // Ueberschuss als Warteschlange puffern (wird nach dem Kochen nachverarbeitet).
            ItemStack extra = stack.clone();
            extra.setAmount(stack.getAmount() - take);
            addToBuffer(pot, extra);
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
        // Auch die gepufferte Warteschlange zurueckgeben.
        for (ItemStack stack : pot.buffer) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        }
        pot.buffer.clear();
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

            // Entladene Chunks ueberspringen (kein Force-Load, kein Item-Loss).
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                continue;
            }

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
                    boolean continues = complete(pot);
                    if (!continues) it.remove();
                }
            }
        }
    }

    /** Schliesst eine Charge ab. Gibt {@code true}, wenn eine weitere Charge aus der
     *  Warteschlange gestartet wurde (der Kessel also weiterlaeuft). */
    private boolean complete(Pot pot) {
        Block block = pot.block;
        Location out = block.getLocation().add(0.5, 1.05, 0.5);
        CauldronRecipe recipe = pot.cooking;
        // Zutaten entfernen, Behaelter-Reste (Eimer) zurueckgeben.
        for (Item entity : pot.entities) {
            if (entity.isValid()) {
                ItemStack remainder = remainderOf(entity.getItemStack());
                entity.remove();
                if (remainder != null) {
                    dropSafe(out, remainder);
                }
            }
        }
        pot.entities.clear();
        pot.cooking = null;
        pot.elapsed = 0;

        if (recipe.serveWithBowl()) {
            servings.put(key(block), new ServingPot(block, recipe.result()));
            dropBuffer(pot);
            plugin.effects().finish(block.getLocation());
            return false;
        }

        ItemStack result = recipe.result().build(plugin);
        if (result != null) {
            dropSafe(out, result);
        }
        reduceWater(block, recipe.waterCost());
        plugin.effects().finish(block.getLocation());

        // Warteschlange: naechste Charge aus dem Puffer starten, wenn noch Wasser kocht.
        if (block.getType() == Material.WATER_CAULDRON && Heat.hasHeatSourceBelow(block)) {
            return refillFromBuffer(pot);
        }
        dropBuffer(pot);
        return false;
    }

    /** Startet die naechste Charge aus dem Puffer. Gibt {@code true} bei Erfolg. */
    private boolean refillFromBuffer(Pot pot) {
        if (pot.buffer.isEmpty()) return false;
        List<ItemStack> units = expandBuffer(pot.buffer);
        for (CauldronRecipe recipe : recipes) {
            List<ItemStack> chosen = selectForRecipe(units, recipe);
            if (chosen == null) continue;
            for (ItemStack unit : chosen) spawnFrozen(pot, unit);
            consumeFromBuffer(pot, chosen);
            pot.cooking = recipe;
            pot.elapsed = 0;
            arrange(pot);
            plugin.effects().boil(pot.block.getLocation(), 6);
            return true;
        }
        dropBuffer(pot);
        return false;
    }

    /** Waehlt aus den Einheiten genau die aus, die ein Rezept exakt erfuellen (oder {@code null}). */
    private List<ItemStack> selectForRecipe(List<ItemStack> units, CauldronRecipe recipe) {
        List<Ingredient> ingredients = recipe.ingredients();
        boolean[] used = new boolean[units.size()];
        List<ItemStack> chosen = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            int found = -1;
            for (int i = 0; i < units.size(); i++) {
                if (!used[i] && ingredient.matches(plugin, units.get(i))) {
                    found = i;
                    break;
                }
            }
            if (found < 0) return null;
            used[found] = true;
            chosen.add(units.get(found));
        }
        return chosen;
    }

    private void addToBuffer(Pot pot, ItemStack stack) {
        for (ItemStack existing : pot.buffer) {
            if (existing.isSimilar(stack)) {
                existing.setAmount(existing.getAmount() + stack.getAmount());
                return;
            }
        }
        pot.buffer.add(stack.clone());
    }

    private List<ItemStack> expandBuffer(List<ItemStack> buffer) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : buffer) {
            for (int i = 0; i < stack.getAmount(); i++) {
                ItemStack one = stack.clone();
                one.setAmount(1);
                out.add(one);
            }
        }
        return out;
    }

    private void consumeFromBuffer(Pot pot, List<ItemStack> chosen) {
        for (ItemStack unit : chosen) {
            for (Iterator<ItemStack> it = pot.buffer.iterator(); it.hasNext(); ) {
                ItemStack stack = it.next();
                if (stack.isSimilar(unit)) {
                    stack.setAmount(stack.getAmount() - 1);
                    if (stack.getAmount() <= 0) it.remove();
                    break;
                }
            }
        }
    }

    private void spawnFrozen(Pot pot, ItemStack unit) {
        Location loc = pot.block.getLocation().add(0.5, 0.55, 0.5);
        if (loc.getWorld() == null) return;
        Item item = loc.getWorld().dropItem(loc, unit.clone());
        freeze(item);
        pot.entities.add(item);
    }

    private void dropBuffer(Pot pot) {
        if (pot.buffer.isEmpty()) return;
        Location out = pot.block.getLocation().add(0.5, 1.05, 0.5);
        for (ItemStack stack : pot.buffer) dropSafe(out, stack);
        pot.buffer.clear();
    }

    /**
     * Gibt ein Ergebnis sicher ueber dem Kessel aus. Der Kessel steht oft ueber
     * einer Waermequelle (Lava/Feuer); damit das Ergebnis nicht verbrennt, wird
     * die Item-Entity unverwundbar gemacht und nur minimal nach oben gestossen,
     * sodass sie auf dem Kessel liegen bleibt.
     */
    private void dropSafe(Location loc, ItemStack stack) {
        if (loc.getWorld() == null) return;
        Item drop = loc.getWorld().dropItem(loc, stack);
        drop.setVelocity(new Vector(0, 0.08, 0));
        drop.setInvulnerable(true);
    }

    private void releaseEntities(Pot pot) {
        for (Item entity : pot.entities) {
            if (entity.isValid()) unfreeze(entity);
        }
        pot.entities.clear();
        dropBuffer(pot);
    }

    private void freeze(Item entity) {
        entity.setGravity(false);
        entity.setVelocity(new Vector(0, 0, 0));
        entity.setPickupDelay(Integer.MAX_VALUE);
        entity.setUnlimitedLifetime(true);
        entity.setCanMobPickup(false);
        entity.setPersistent(false); // kein Crash-Orphan: verschwindet bei Unload/Neustart
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
        /** Ueberschuss-Zutaten, die als Warteschlange nacheinander verarbeitet werden. */
        private final List<ItemStack> buffer = new ArrayList<>();
        private CauldronRecipe cooking;
        private int elapsed;

        private Pot(Block block) {
            this.block = block;
        }
    }

    private record ServingPot(Block block, de.yourshika.smokeandsalt.content.ResultSpec result) {
    }
}
