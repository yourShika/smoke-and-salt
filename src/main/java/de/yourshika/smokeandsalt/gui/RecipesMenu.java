package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.content.Ingredient;
import de.yourshika.smokeandsalt.cooking.CauldronRecipe;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.crafting.CraftingRecipe;
import de.yourshika.smokeandsalt.seed.SeedDefinition;
import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for {@code /sas recipes}. The first screen shows categories; clicking a
 * category opens only that recipe family.
 */
public final class RecipesMenu {

    private static final int PAGE_SIZE = 45;
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private RecipesMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        List<RecipeView> views = collect(plugin);
        Map<RecipeCategory, Integer> counts = counts(views);

        MenuHolder holder = new MenuHolder("recipes_categories");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.line("<gradient:#e2a76f:#c65b3a><bold>Recipe Categories</bold></gradient>"));
        holder.setInventory(inv);
        for (int i = 45; i < 54; i++) holder.set(i, Icons.accent());

        RecipeCategory[] categories = RecipeCategory.values();
        for (int i = 0; i < categories.length && i < CATEGORY_SLOTS.length; i++) {
            RecipeCategory category = categories[i];
            int count = counts.getOrDefault(category, 0);
            holder.set(CATEGORY_SLOTS[i], Icons.of(category.icon(), "<gold><bold>" + category.displayName() + "</bold>",
                    "<gray>Recipes: <white>" + count,
                    " ",
                    "<yellow>Click to open"), (p, e) -> open(plugin, p, category, 0));
        }

        holder.set(49, Icons.of(Material.BOOK, "<gold><bold>Recipes</bold>",
                "<gray>Total: <white>" + views.size(),
                "<gray>Seeds included in their own category.",
                " ",
                "<dark_gray>Pick a category."));
        player.openInventory(inv);
    }

    public static void open(SmokeAndSalt plugin, Player player, RecipeCategory category, int page) {
        List<RecipeView> views = collect(plugin).stream()
                .filter(view -> view.category() == category)
                .toList();
        int pages = Math.max(1, (int) Math.ceil(views.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, pages - 1));

        MenuHolder holder = new MenuHolder("recipes");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.line("<gradient:#e2a76f:#c65b3a><bold>" + category.displayName()
                        + "</bold></gradient> <dark_gray>(" + (page + 1) + "/" + pages + ")"));
        holder.setInventory(inv);
        for (int i = 45; i < 54; i++) holder.set(i, Icons.accent());

        int start = page * PAGE_SIZE;
        final int current = page;
        for (int i = 0; i < PAGE_SIZE && start + i < views.size(); i++) {
            RecipeView view = views.get(start + i);
            holder.set(i, resultIcon(view), (p, e) -> RecipeDetailMenu.open(plugin, p, view, category, current, 0));
        }

        if (page > 0) {
            holder.set(45, Icons.of(Material.ARROW, "<yellow>Back"),
                    (p, e) -> open(plugin, p, category, current - 1));
        }
        holder.set(49, Icons.of(Material.BOOK, "<gold><bold>" + category.displayName() + "</bold>",
                "<gray>Recipes: <white>" + views.size(),
                " ",
                "<yellow>Click a recipe for details",
                "<gray>Use back to return to categories."), (p, e) -> open(plugin, p));
        if (page < pages - 1) {
            holder.set(53, Icons.of(Material.ARROW, "<yellow>Next"),
                    (p, e) -> open(plugin, p, category, current + 1));
        }

        player.openInventory(inv);
    }

    /** Result icon with a short hint in the lore. */
    private static ItemStack resultIcon(RecipeView view) {
        ItemStack icon = view.result() != null ? view.result().clone() : new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Text.line(" "));
            lore.add(Text.line("<dark_gray>" + view.station()));
            if (view.variants().size() > 1) {
                lore.add(Text.line("<gray>Variants: <white>" + view.variants().size()));
            }
            lore.add(Text.line("<yellow>Click to view the recipe"));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    /** Builds a unified list of recipe views from all sources. */
    static List<RecipeView> collect(SmokeAndSalt plugin) {
        List<RecipeView> out = new ArrayList<>();
        for (CookingRecipe r : plugin.cooking().registry().all()) {
            Material icon = stationIcon(r.station());
            RecipeCategory category = stationCategory(r.station());
            out.add(new RecipeView(r.id(), category, r.station().displayName(), icon,
                    List.of(stationInput(plugin, r)),
                    plugin.cooking().registry().buildResult(r), r.durationTicks()));
        }
        for (CauldronRecipe r : plugin.cauldron().recipes()) {
            String note = null;
            if (r.serveWithBowl()) {
                note = "After cooking, right-click the cauldron with bowls to take portions until the water is empty.";
            } else if (r.waterCost() > 0) {
                note = "Consumes " + r.waterCost() + " water level from the cauldron.";
            }
            out.add(RecipeView.single(r.id(), RecipeCategory.WATER_CAULDRON, "Water Cauldron", Material.CAULDRON,
                    icons(plugin, r.ingredients()), r.result().build(plugin), r.duration(), note));
        }

        List<CraftingRecipe> shashlik = new ArrayList<>();
        for (CraftingRecipe r : plugin.crafting().recipes()) {
            if (r.id().startsWith("schaschlik_")) {
                shashlik.add(r);
                continue;
            }
            out.add(new RecipeView(r.id(), RecipeCategory.CRAFTING, "Crafting Table", Material.CRAFTING_TABLE,
                    icons(plugin, r.ingredients()), r.result().build(plugin), 0));
        }
        if (!shashlik.isEmpty()) {
            List<List<ItemStack>> variants = new ArrayList<>();
            for (CraftingRecipe r : shashlik) variants.add(icons(plugin, r.ingredients()));
            out.add(new RecipeView("schaschlik", RecipeCategory.CRAFTING, "Crafting Table",
                    Material.CRAFTING_TABLE, variants, shashlik.get(0).result().build(plugin), 0,
                    "Use the arrows to switch between carrot and potato variants."));
        }

        for (SeedDefinition seed : plugin.seeds().all()) {
            ItemStack result = plugin.seeds().create(seed.id(), 1);
            String note = "Break seagrass for a chance to get this seed. Plant it on farmland; fully grown crops yield "
                    + seedResultName(seed) + " and seeds back.";
            out.add(RecipeView.single("seed_" + seed.id(), RecipeCategory.SEEDS, "Seed Drop & Farming",
                    Material.WHEAT_SEEDS,
                    List.of(new ItemStack(Material.SEAGRASS), new ItemStack(Material.FARMLAND)),
                    result, 0, note));
        }
        return out;
    }

    private static Map<RecipeCategory, Integer> counts(List<RecipeView> views) {
        Map<RecipeCategory, Integer> out = new EnumMap<>(RecipeCategory.class);
        for (RecipeView view : views) {
            out.merge(view.category(), 1, Integer::sum);
        }
        return out;
    }

    private static RecipeCategory stationCategory(CookingStation station) {
        return switch (station) {
            case SMOKER -> RecipeCategory.SMOKER;
            case CAMPFIRE -> RecipeCategory.CAMPFIRE;
            case CAULDRON_WATER -> RecipeCategory.WATER_CAULDRON;
            case CAULDRON_LAVA -> RecipeCategory.LAVA_CAULDRON;
            case CUTTING -> RecipeCategory.CUTTING;
        };
    }

    private static Material stationIcon(CookingStation station) {
        return switch (station) {
            case SMOKER -> Material.SMOKER;
            case CAMPFIRE -> Material.CAMPFIRE;
            case CAULDRON_WATER -> Material.CAULDRON;
            case CAULDRON_LAVA -> Material.LAVA_BUCKET;
            case CUTTING -> Material.IRON_AXE;
        };
    }

    private static ItemStack stationInput(SmokeAndSalt plugin, CookingRecipe r) {
        if (r.inputIsCustom()) {
            ItemStack item = plugin.items().create(r.inputItemId(), 1);
            return item != null ? item : new ItemStack(Material.PAPER);
        }
        return new ItemStack(r.inputMaterial());
    }

    private static List<ItemStack> icons(SmokeAndSalt plugin, List<Ingredient> ingredients) {
        List<ItemStack> out = new ArrayList<>();
        for (Ingredient ingredient : ingredients) out.add(ingredient.icon(plugin));
        return out;
    }

    private static String seedResultName(SeedDefinition seed) {
        if (seed.resultItemId() != null) return seed.resultItemId();
        if (seed.resultMaterial() != null) return seed.resultMaterial().name().toLowerCase(java.util.Locale.ROOT);
        return seed.id();
    }
}
