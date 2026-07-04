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
import java.util.Locale;

/**
 * GUI fuer {@code /sas recipes}: zeigt alle registrierten Rezepte (Smoker,
 * Lagerfeuer, Kessel, Werkbank) als Ergebnis-Icons mit Zutaten in der Lore.
 * Bei mehr als einer Seite laesst sich blaettern.
 */
public final class RecipesMenu {

    private static final int PAGE_SIZE = 45;

    private RecipesMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        open(plugin, player, 0);
    }

    public static void open(SmokeAndSalt plugin, Player player, int page) {
        List<ItemStack> icons = collect(plugin);
        int pages = Math.max(1, (int) Math.ceil(icons.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, pages - 1));

        MenuHolder holder = new MenuHolder("recipes");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.line("<gradient:#e2a76f:#c65b3a><bold>Rezepte</bold></gradient> <dark_gray>(" + (page + 1) + "/" + pages + ")"));
        holder.setInventory(inv);
        for (int i = 45; i < 54; i++) holder.set(i, Icons.accent());

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < icons.size(); i++) {
            holder.set(i, icons.get(start + i));
        }

        final int current = page;
        if (page > 0) {
            holder.set(45, Icons.of(Material.ARROW, "<yellow>Zurueck"),
                    (p, e) -> open(plugin, p, current - 1));
        }
        holder.set(49, Icons.of(Material.BOOK, "<gold><bold>Rezepte</bold>",
                "<gray>Gesamt: <white>" + icons.size(),
                "<gray>Smoker/Lagerfeuer: <white>" + plugin.cooking().registry().size(),
                "<gray>Kessel: <white>" + plugin.cauldron().size(),
                "<gray>Werkbank: <white>" + plugin.crafting().size()));
        if (page < pages - 1) {
            holder.set(53, Icons.of(Material.ARROW, "<yellow>Weiter"),
                    (p, e) -> open(plugin, p, current + 1));
        }

        player.openInventory(inv);
    }

    private static List<ItemStack> collect(SmokeAndSalt plugin) {
        List<ItemStack> out = new ArrayList<>();
        for (CookingRecipe r : plugin.cooking().registry().all()) {
            if (r.station() == CookingStation.CAULDRON_LAVA) continue;
            ItemStack icon = plugin.cooking().registry().buildResult(r);
            out.add(decorate(icon, r.station().displayName(), List.of(inputName(plugin, r)), resultName(plugin, icon, r.resultAmount())));
        }
        for (CauldronRecipe r : plugin.cauldron().recipes()) {
            ItemStack icon = r.result().build(plugin);
            out.add(decorate(icon, "Wasserkessel", ingredientNames(r.ingredients()),
                    resultName(plugin, icon, r.result().amount())));
        }
        for (CraftingRecipe r : plugin.crafting().recipes()) {
            ItemStack icon = r.result().build(plugin);
            out.add(decorate(icon, "Werkbank", ingredientNames(r.ingredients()),
                    resultName(plugin, icon, r.result().amount())));
        }
        return out;
    }

    private static ItemStack decorate(ItemStack icon, String station, List<String> inputs, String result) {
        if (icon == null) icon = new ItemStack(Material.PAPER);
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;
        List<Component> lore = new ArrayList<>();
        lore.add(Text.line("<dark_gray>" + station));
        lore.add(Text.line(" "));
        lore.add(Text.line("<gray>Zutaten:"));
        for (String in : inputs) lore.add(Text.line("<gray> • <white>" + in));
        lore.add(Text.line("<gray>Ergebnis: <white>" + result));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static List<String> ingredientNames(List<Ingredient> ingredients) {
        List<String> out = new ArrayList<>();
        for (Ingredient ingredient : ingredients) out.add(ingredient.display());
        return out;
    }

    private static String inputName(SmokeAndSalt plugin, CookingRecipe r) {
        if (r.inputIsCustom()) {
            var def = plugin.items().definition(r.inputItemId());
            return def != null ? stripName(def.displayName()) : r.inputItemId();
        }
        return pretty(r.inputMaterial());
    }

    private static String resultName(SmokeAndSalt plugin, ItemStack icon, int amount) {
        String base;
        if (icon != null && icon.hasItemMeta() && icon.getItemMeta().hasDisplayName()) {
            base = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(icon.getItemMeta().displayName());
        } else {
            base = icon != null ? pretty(icon.getType()) : "?";
        }
        return base + (amount > 1 ? " x" + amount : "");
    }

    private static String stripName(String mini) {
        return mini.replaceAll("<[^>]+>", "");
    }

    private static String pretty(Material material) {
        String s = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
