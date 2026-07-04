package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Detail view of a recipe: station, ingredients, an arrow and the result.
 */
public final class RecipeDetailMenu {

    private static final int[] INPUT_SLOTS = {9, 10, 11, 12, 13};

    private RecipeDetailMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player, RecipeView view,
                            RecipeCategory backCategory, int backPage, int variant) {
        variant = Math.max(0, Math.min(variant, Math.max(0, view.variants().size() - 1)));
        MenuHolder holder = new MenuHolder("recipe_detail");
        String resultName = plainName(view.result());
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.line("<gradient:#e2a76f:#c65b3a><bold>" + resultName + "</bold></gradient>"));
        holder.setInventory(inv);
        for (int i = 0; i < 27; i++) holder.set(i, Icons.accent());

        String time = view.cookTicks() > 0
                ? "<gray>Time: <white>" + String.format(java.util.Locale.ROOT, "%.1f", view.cookTicks() / 20.0) + "s"
                : "<gray>No cooking time";
        holder.set(4, Icons.of(view.stationIcon(), "<gold><bold>" + view.station() + "</bold>",
                "<gray>Station", " ", time));

        List<ItemStack> inputs = view.inputs(variant);
        for (int i = 0; i < inputs.size() && i < INPUT_SLOTS.length; i++) {
            holder.set(INPUT_SLOTS[i], labelIcon(inputs.get(i)));
        }

        holder.set(15, Icons.of(Material.SPECTRAL_ARROW, "<yellow><bold>-></bold>",
                "<gray>" + view.station()));
        if (view.result() != null) {
            holder.set(16, labelIcon(view.result()));
        }

        if (view.note() != null && !view.note().isBlank()) {
            holder.set(21, Icons.of(Material.PAPER, "<yellow>Note", "<gray>" + view.note()));
        }

        if (view.variants().size() > 1) {
            final int currentVariant = variant;
            holder.set(18, Icons.of(Material.ARROW, "<yellow>Previous Variant",
                    "<gray>" + (variant + 1) + "/" + view.variants().size()),
                    (p, e) -> open(plugin, p, view, backCategory, backPage,
                            (currentVariant - 1 + view.variants().size()) % view.variants().size()));
            holder.set(26, Icons.of(Material.ARROW, "<yellow>Next Variant",
                    "<gray>" + (variant + 1) + "/" + view.variants().size()),
                    (p, e) -> open(plugin, p, view, backCategory, backPage,
                            (currentVariant + 1) % view.variants().size()));
        }

        holder.set(22, Icons.of(Material.ARROW, "<yellow>Back to " + backCategory.displayName()),
                (p, e) -> RecipesMenu.open(plugin, p, backCategory, backPage));

        player.openInventory(inv);
    }

    /** Ensures the icon carries a visible name (falls back to a prettified type). */
    private static ItemStack labelIcon(ItemStack source) {
        ItemStack icon = source.clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null && !meta.hasDisplayName()) {
            meta.displayName(Text.line("<white>" + pretty(icon.getType())));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static String plainName(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName());
        }
        return item != null ? pretty(item.getType()) : "Recipe";
    }

    private static String pretty(Material material) {
        String s = material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
