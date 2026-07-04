package de.yourshika.smokeandsalt.crafting;

import de.yourshika.smokeandsalt.content.Ingredient;
import de.yourshika.smokeandsalt.content.ResultSpec;

import java.util.List;

/**
 * Ein formfreies (shapeless) Crafting-Rezept fuer die Werkbank. Die eigentliche
 * Zutaten-Pruefung laeuft ueber {@link de.yourshika.smokeandsalt.content.RecipeMatch}
 * (inklusive Custom-Item-Erkennung per PDC), damit z.B. echter Honig nicht als
 * Kaese durchgeht.
 *
 * @param id          stabile Rezept-ID (auch NamespacedKey-Suffix)
 * @param ingredients benoetigte Zutaten (ungeordnet)
 * @param result      das Ergebnis
 */
public record CraftingRecipe(String id, List<Ingredient> ingredients, ResultSpec result) {

    public CraftingRecipe {
        ingredients = List.copyOf(ingredients);
        if (ingredients.isEmpty() || ingredients.size() > 9) {
            throw new IllegalArgumentException("Crafting-Rezept '" + id + "' braucht 1..9 Zutaten.");
        }
    }
}
