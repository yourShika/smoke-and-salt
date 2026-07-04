package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Bildet die Smoker- und Lagerfeuer-Rezepte aus der {@link CookingRegistry} auf
 * <strong>echte Vanilla-Rezepte</strong> ab (SmokingRecipe / CampfireRecipe).
 * Dadurch verhalten sich beide Stationen exakt wie im Vanilla-Spiel: Zutat in den
 * Smoker legen (mit Brennstoff) bzw. auf ein brennendes Lagerfeuer legen, kurz
 * warten, Ergebnis einsammeln.
 *
 * <p>Als Zutat wird {@link RecipeChoice.ExactChoice} genutzt, damit die
 * Custom-Ergebnisse (die dasselbe Basis-Material tragen) nicht erneut
 * verarbeitet werden - nur die echte, unveraenderte Zutat passt.</p>
 */
public final class StationRecipeManager {

    private final SmokeAndSalt plugin;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public StationRecipeManager(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    /** Registriert alle Smoker-/Lagerfeuer-Rezepte als Vanilla-Rezepte. */
    public void registerAll() {
        for (CookingRecipe recipe : plugin.cooking().registry().all()) {
            switch (recipe.station()) {
                case SMOKER -> addSmoking(recipe);
                case CAMPFIRE -> addCampfire(recipe);
                default -> {
                    // Kessel/Schneiden laufen ueber eigene Systeme.
                }
            }
        }
    }

    public void unregisterAll() {
        for (NamespacedKey key : keys) {
            Bukkit.removeRecipe(key);
        }
        keys.clear();
    }

    private void addSmoking(CookingRecipe recipe) {
        ItemStack result = plugin.cooking().registry().buildResult(recipe);
        RecipeChoice input = inputChoice(recipe);
        if (result == null || input == null) return;
        NamespacedKey key = new NamespacedKey(plugin, "sas_smoke_" + recipe.id());
        Bukkit.addRecipe(new SmokingRecipe(key, result, input, 0.2f, recipe.durationTicks()));
        keys.add(key);
    }

    private void addCampfire(CookingRecipe recipe) {
        ItemStack result = plugin.cooking().registry().buildResult(recipe);
        RecipeChoice input = inputChoice(recipe);
        if (result == null || input == null) return;
        NamespacedKey key = new NamespacedKey(plugin, "sas_camp_" + recipe.id());
        Bukkit.addRecipe(new CampfireRecipe(key, result, input, 0.2f, recipe.durationTicks()));
        keys.add(key);
    }

    private RecipeChoice inputChoice(CookingRecipe recipe) {
        ItemStack proto;
        if (recipe.inputIsCustom()) {
            proto = plugin.items().create(recipe.inputItemId(), 1);
        } else {
            proto = new ItemStack(recipe.inputMaterial());
        }
        return proto == null ? null : new RecipeChoice.ExactChoice(proto);
    }
}
