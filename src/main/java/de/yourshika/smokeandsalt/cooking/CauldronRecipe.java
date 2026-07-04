package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.content.Ingredient;
import de.yourshika.smokeandsalt.content.ResultSpec;

import java.util.List;

/**
 * Ein Rezept fuer den Wasserkessel. Anders als die einfachen Stationen kann ein
 * Kessel-Rezept mehrere Zutaten verlangen (das Wasser selbst ist implizit). Die
 * Zutaten werden nacheinander in den kochenden Kessel geworfen und dort
 * gesammelt, bis die Kombination passt.
 *
 * @param id          stabile Rezept-ID
 * @param ingredients benoetigte Zutaten (ungeordnet, 1..n)
 * @param result      das Ergebnis
 * @param duration    Kochzeit in Ticks, sobald die Kombination vollstaendig ist
 * @param waterCost   wie viele Wasserlevel beim Abschluss verbraucht werden
 * @param serveWithBowl bleibt das Ergebnis im Kessel und wird mit Bowls portioniert?
 */
public record CauldronRecipe(String id, List<Ingredient> ingredients, ResultSpec result,
                             int duration, int waterCost, boolean serveWithBowl) {

    public CauldronRecipe(String id, List<Ingredient> ingredients, ResultSpec result, int duration) {
        this(id, ingredients, result, duration, 0, false);
    }

    public CauldronRecipe(String id, List<Ingredient> ingredients, ResultSpec result,
                          int duration, int waterCost) {
        this(id, ingredients, result, duration, waterCost, false);
    }

    public CauldronRecipe {
        ingredients = List.copyOf(ingredients);
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Kessel-Rezept '" + id + "' benoetigt Zutaten.");
        }
        if (duration < 1) duration = 1;
        if (waterCost < 0) waterCost = 0;
    }
}
