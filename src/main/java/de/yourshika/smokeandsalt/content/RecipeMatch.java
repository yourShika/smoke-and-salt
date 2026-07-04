package de.yourshika.smokeandsalt.content;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Ungeordneter (shapeless) Abgleich zwischen einer Menge an Items und einer
 * Liste von {@link Ingredient}s. Da Rezepte hoechstens eine Handvoll Zutaten
 * haben, genuegt einfaches Backtracking.
 */
public final class RecipeMatch {

    private RecipeMatch() {
    }

    /** Exakter Abgleich: jede Zutat wird durch genau ein Item erfuellt (Bijektion). */
    public static boolean exact(SmokeAndSalt plugin, List<ItemStack> items, List<Ingredient> ingredients) {
        List<ItemStack> filtered = nonEmpty(items);
        return filtered.size() == ingredients.size() && match(plugin, filtered, ingredients, true);
    }

    /**
     * Teil-Abgleich: alle Items lassen sich injektiv auf verschiedene Zutaten
     * abbilden (Items &sube; Zutaten). Fuer die Kessel-Akkumulation.
     */
    public static boolean partial(SmokeAndSalt plugin, List<ItemStack> items, List<Ingredient> ingredients) {
        List<ItemStack> filtered = nonEmpty(items);
        return filtered.size() <= ingredients.size() && match(plugin, filtered, ingredients, false);
    }

    private static boolean match(SmokeAndSalt plugin, List<ItemStack> items,
                                 List<Ingredient> ingredients, boolean exact) {
        boolean[] used = new boolean[ingredients.size()];
        return backtrack(plugin, items, 0, ingredients, used);
    }

    private static boolean backtrack(SmokeAndSalt plugin, List<ItemStack> items, int index,
                                     List<Ingredient> ingredients, boolean[] used) {
        if (index >= items.size()) return true;
        ItemStack item = items.get(index);
        for (int i = 0; i < ingredients.size(); i++) {
            if (used[i]) continue;
            if (ingredients.get(i).matches(plugin, item)) {
                used[i] = true;
                if (backtrack(plugin, items, index + 1, ingredients, used)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    private static List<ItemStack> nonEmpty(List<ItemStack> items) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) out.add(item);
        }
        return out;
    }
}
