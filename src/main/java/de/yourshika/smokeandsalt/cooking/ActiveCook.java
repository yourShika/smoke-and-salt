package de.yourshika.smokeandsalt.cooking;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

/**
 * Ein laufender Koch-Vorgang. Wird vom {@link CookingManager} pro Tick
 * fortgeschrieben. Fuer block-basierte Stationen (Smoker, Lagerfeuer, Kessel)
 * ist {@link #block} der Anker; fuer schwebende Kessel-Vorgaenge zusaetzlich das
 * {@link #floatingItem}, das im Wasser gehalten wird.
 */
public final class ActiveCook {

    private final Block block;
    private final CookingStation station;
    private final CookingRecipe recipe;
    private final Item floatingItem;
    private final ItemStack input;
    private final int duration;
    private int elapsed;

    public ActiveCook(Block block, CookingStation station, CookingRecipe recipe,
                      Item floatingItem, ItemStack input) {
        this.block = block;
        this.station = station;
        this.recipe = recipe;
        this.floatingItem = floatingItem;
        this.input = input;
        this.duration = recipe.durationTicks();
    }

    public Block block() { return block; }
    public CookingStation station() { return station; }
    public CookingRecipe recipe() { return recipe; }
    public Item floatingItem() { return floatingItem; }
    /** Die verbrauchte Zutat (1 Stueck) - fuer Rueckerstattung bei Abbruch. */
    public ItemStack input() { return input; }
    public int duration() { return duration; }
    public int elapsed() { return elapsed; }

    /** Zaehlt den Fortschritt hoch und meldet, ob der Vorgang fertig ist. */
    public boolean tick(int amount) {
        elapsed += amount;
        return elapsed >= duration;
    }

    public double progress() {
        return Math.min(1.0, (double) elapsed / duration);
    }

    public Location effectLocation() {
        return block.getLocation();
    }
}
