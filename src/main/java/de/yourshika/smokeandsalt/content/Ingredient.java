package de.yourshika.smokeandsalt.content;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

/**
 * Eine Zutat eines Koch- oder Crafting-Rezepts. Kann ein Custom-Item, ein
 * Custom-Seed, ein Vanilla-Material oder eine Material-Gruppe (Tag, z.B. alle
 * Schwerter) sein. Wird von {@link RecipeMatch} zum Abgleich genutzt.
 */
public interface Ingredient {

    /** Passt der gegebene Stack auf diese Zutat? */
    boolean matches(SmokeAndSalt plugin, ItemStack stack);

    /** Lesbarer Name fuer die Rezept-Anzeige. */
    String display();

    /** Ein repraesentatives Icon fuer die GUI. */
    ItemStack icon(SmokeAndSalt plugin);

    // --- Fabrik-Methoden ----------------------------------------------------

    static Ingredient item(String id, String display) {
        return new CustomItemIngredient(id, display);
    }

    static Ingredient seed(String id, String display) {
        return new SeedIngredient(id, display);
    }

    static Ingredient material(Material material, String display) {
        return new MaterialIngredient(material, display);
    }

    static Ingredient tag(Tag<Material> tag, Material icon, String display) {
        return new TagIngredient(tag, icon, display);
    }

    // --- Implementierungen --------------------------------------------------

    record CustomItemIngredient(String id, String display) implements Ingredient {
        @Override
        public boolean matches(SmokeAndSalt plugin, ItemStack stack) {
            return id.equalsIgnoreCase(plugin.items().idOf(stack));
        }

        @Override
        public ItemStack icon(SmokeAndSalt plugin) {
            ItemStack item = plugin.items().create(id, 1);
            return item != null ? item : new ItemStack(Material.PAPER);
        }
    }

    record SeedIngredient(String id, String display) implements Ingredient {
        @Override
        public boolean matches(SmokeAndSalt plugin, ItemStack stack) {
            return id.equalsIgnoreCase(plugin.seeds().idOf(stack));
        }

        @Override
        public ItemStack icon(SmokeAndSalt plugin) {
            ItemStack item = plugin.seeds().create(id, 1);
            return item != null ? item : new ItemStack(Material.WHEAT_SEEDS);
        }
    }

    record MaterialIngredient(Material material, String display) implements Ingredient {
        @Override
        public boolean matches(SmokeAndSalt plugin, ItemStack stack) {
            // Custom-Items duerfen NICHT ueber ihr Basis-Material matchen.
            return stack != null && stack.getType() == material
                    && plugin.items().idOf(stack) == null && plugin.seeds().idOf(stack) == null;
        }

        @Override
        public ItemStack icon(SmokeAndSalt plugin) {
            return new ItemStack(material);
        }
    }

    record TagIngredient(Tag<Material> tag, Material icon, String display) implements Ingredient {
        @Override
        public boolean matches(SmokeAndSalt plugin, ItemStack stack) {
            return stack != null && tag.isTagged(stack.getType())
                    && plugin.items().idOf(stack) == null && plugin.seeds().idOf(stack) == null;
        }

        @Override
        public ItemStack icon(SmokeAndSalt plugin) {
            return new ItemStack(icon);
        }
    }
}
