package de.yourshika.smokeandsalt.crafting;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.content.Ingredient;
import de.yourshika.smokeandsalt.content.RecipeMatch;
import de.yourshika.smokeandsalt.item.ItemDefinition;
import de.yourshika.smokeandsalt.seed.SeedDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.Keyed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verwaltet die formfreien Crafting-Rezepte in der Werkbank. Registriert echte
 * Bukkit-{@link ShapelessRecipe}s (damit Werkbank, Rezeptbuch und Verbrauch
 * funktionieren) und validiert die Zutaten in {@link PrepareItemCraftEvent} per
 * PDC. Werkzeuge (Schwert) bleiben erhalten, Eimer werden geleert zurueckgegeben.
 */
public final class CraftingManager implements Listener {

    private static final Set<Material> BUCKETS = EnumSet.of(
            Material.MILK_BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.POWDER_SNOW_BUCKET);

    private final SmokeAndSalt plugin;
    private final Map<String, CraftingRecipe> recipes = new LinkedHashMap<>();
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public CraftingManager(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    public List<CraftingRecipe> recipes() {
        return new ArrayList<>(recipes.values());
    }

    public int size() {
        return recipes.size();
    }

    public boolean contains(String id) {
        if (id == null) return false;
        String key = id.toLowerCase(java.util.Locale.ROOT);
        return recipes.containsKey(key) || recipes.containsKey("sas_craft_" + key);
    }

    /** Registriert ein Rezept (fuegt es der Werkbank hinzu). */
    public void register(CraftingRecipe recipe) {
        ItemStack result = recipe.result().build(plugin);
        if (result == null) {
            plugin.getLogger().warning("Crafting-Rezept '" + recipe.id() + "': Ergebnis nicht verfuegbar.");
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "sas_craft_" + recipe.id());
        ShapelessRecipe shapeless = new ShapelessRecipe(key, result);
        for (Ingredient ingredient : recipe.ingredients()) {
            shapeless.addIngredient(choice(ingredient));
        }
        Bukkit.addRecipe(shapeless);
        recipes.put(key.getKey(), recipe);
        registeredKeys.add(key);
    }

    /** Entfernt alle registrierten Rezepte (fuer /sas reload). */
    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
        recipes.clear();
    }

    /** Entfernt das Vanilla-Rezept fuer Brot, damit Teig/Kaiser Roll die Progression fuehren. */
    public void disableVanillaBreadRecipe() {
        Bukkit.removeRecipe(NamespacedKey.minecraft("bread"));
    }

    private RecipeChoice choice(Ingredient ingredient) {
        if (ingredient instanceof Ingredient.CustomItemIngredient ci) {
            ItemDefinition def = plugin.items().definition(ci.id());
            return new RecipeChoice.MaterialChoice(def != null ? def.material() : Material.PAPER);
        }
        if (ingredient instanceof Ingredient.SeedIngredient si) {
            SeedDefinition def = plugin.seeds().definition(si.id());
            return new RecipeChoice.MaterialChoice(def != null ? def.material() : Material.WHEAT_SEEDS);
        }
        if (ingredient instanceof Ingredient.MaterialIngredient mi) {
            return new RecipeChoice.MaterialChoice(mi.material());
        }
        if (ingredient instanceof Ingredient.TagIngredient ti) {
            return new RecipeChoice.MaterialChoice(ti.tag());
        }
        return new RecipeChoice.MaterialChoice(Material.PAPER);
    }

    // --- Validierung + Ergebnis ---------------------------------------------

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        CraftingRecipe ours = lookup(recipe);
        List<ItemStack> matrix = Arrays.asList(event.getInventory().getMatrix());
        if (ours == null) {
            if (containsSmokeAndSaltItem(matrix)) {
                event.getInventory().setResult(null);
            }
            return;
        }

        if (RecipeMatch.exact(plugin, matrix, ours.ingredients())) {
            event.getInventory().setResult(ours.result().build(plugin));
        } else {
            event.getInventory().setResult(null); // Zutaten passen nicht (z.B. echter Honig statt Kaese)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onCraft(CraftItemEvent event) {
        CraftingRecipe ours = lookup(event.getRecipe());
        if (ours == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack[] matrix = event.getInventory().getMatrix();
        List<ItemStack> special = collectSpecial(matrix);

        if (!special.isEmpty() && event.isShiftClick()) {
            // Bei Werkzeug-/Eimer-Rezepten nur einzeln craften (sichere Rueckgabe).
            event.setCancelled(true);
            plugin.messages().send(player, "crafting.single-only");
            return;
        }

        // Kleiner Fertigstellungs-Effekt.
        plugin.effects().craft(player.getLocation());

        if (special.isEmpty()) return;
        // Werkzeuge/Eimer nach dem Verbrauch zurueckgeben - nur wenn der Craft
        // wirklich durchlief (kein spaeter Abbruch -> sonst Dupe).
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled()) return;
            for (ItemStack ret : special) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(ret);
                leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
            }
            player.updateInventory();
        });
    }

    /** Sammelt zurueckzugebende Werkzeuge (Schwert, leicht abgenutzt) und Eimer. */
    private List<ItemStack> collectSpecial(ItemStack[] matrix) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            if (Tag.ITEMS_SWORDS.isTagged(item.getType())) {
                ItemStack sword = item.clone();
                sword.setAmount(1);
                if (sword.getItemMeta() instanceof Damageable dmg && item.getType().getMaxDurability() > 0) {
                    dmg.setDamage(Math.min(item.getType().getMaxDurability() - 1, dmg.getDamage() + 1));
                    sword.setItemMeta(dmg);
                }
                out.add(sword);
            } else if (BUCKETS.contains(item.getType())) {
                out.add(new ItemStack(Material.BUCKET));
            }
        }
        return out;
    }

    private boolean containsSmokeAndSaltItem(List<ItemStack> matrix) {
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            if (plugin.items().idOf(item) != null || plugin.seeds().idOf(item) != null) {
                return true;
            }
        }
        return false;
    }

    private CraftingRecipe lookup(Recipe recipe) {
        if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals(
                plugin.getName().toLowerCase())) {
            return recipes.get(keyed.getKey().getKey());
        }
        return null;
    }
}
