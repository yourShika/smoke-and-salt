package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.content.Ingredient;
import de.yourshika.smokeandsalt.cooking.CauldronRecipe;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.crafting.CraftingRecipe;
import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for {@code /sas recipes}: a grid of result icons. Clicking an icon opens a
 * detail view ({@link RecipeDetailMenu}) that lays out the whole process nicely.
 */
public final class RecipesMenu {

    private static final int PAGE_SIZE = 45;

    private RecipesMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        open(plugin, player, 0);
    }

    public static void open(SmokeAndSalt plugin, Player player, int page) {
        List<RecipeView> views = collect(plugin);
        int pages = Math.max(1, (int) Math.ceil(views.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, pages - 1));

        MenuHolder holder = new MenuHolder("recipes");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.line("<gradient:#e2a76f:#c65b3a><bold>Recipes</bold></gradient> <dark_gray>(" + (page + 1) + "/" + pages + ")"));
        holder.setInventory(inv);
        for (int i = 45; i < 54; i++) holder.set(i, Icons.accent());

        int start = page * PAGE_SIZE;
        final int current = page;
        for (int i = 0; i < PAGE_SIZE && start + i < views.size(); i++) {
            RecipeView view = views.get(start + i);
            holder.set(i, resultIcon(view), (p, e) -> RecipeDetailMenu.open(plugin, p, view, current));
        }

        if (page > 0) {
            holder.set(45, Icons.of(Material.ARROW, "<yellow>Back"),
                    (p, e) -> open(plugin, p, current - 1));
        }
        holder.set(49, Icons.of(Material.BOOK, "<gold><bold>Recipes</bold>",
                "<gray>Total: <white>" + views.size(),
                "<gray>Smoker/Campfire: <white>" + plugin.cooking().registry().size(),
                "<gray>Cauldron: <white>" + plugin.cauldron().size(),
                "<gray>Crafting: <white>" + plugin.crafting().size(),
                " ",
                "<dark_gray>Click a recipe for details."));
        if (page < pages - 1) {
            holder.set(53, Icons.of(Material.ARROW, "<yellow>Next"),
                    (p, e) -> open(plugin, p, current + 1));
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
            Material icon = switch (r.station()) {
                case SMOKER -> Material.SMOKER;
                case CAMPFIRE -> Material.CAMPFIRE;
                case CAULDRON_WATER, CAULDRON_LAVA -> Material.CAULDRON;
                case CUTTING -> Material.IRON_AXE;
            };
            if (r.station() == CookingStation.CAULDRON_LAVA) continue;
            out.add(new RecipeView(r.station().displayName(), icon,
                    List.of(stationInput(plugin, r)),
                    plugin.cooking().registry().buildResult(r), r.durationTicks()));
        }
        for (CauldronRecipe r : plugin.cauldron().recipes()) {
            out.add(new RecipeView("Water Cauldron", Material.CAULDRON,
                    icons(plugin, r.ingredients()), r.result().build(plugin), r.duration()));
        }
        for (CraftingRecipe r : plugin.crafting().recipes()) {
            out.add(new RecipeView("Crafting Table", Material.CRAFTING_TABLE,
                    icons(plugin, r.ingredients()), r.result().build(plugin), 0));
        }
        return out;
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
}
