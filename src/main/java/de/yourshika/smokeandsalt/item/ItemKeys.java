package de.yourshika.smokeandsalt.item;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.NamespacedKey;

/**
 * Zentrale Sammlung der {@link NamespacedKey}s, mit denen Smoke &amp; Salt seine
 * Items und Zwischenprodukte im PersistentDataContainer markiert.
 */
public final class ItemKeys {

    /** Stabile ID eines Custom-Items (z.B. Zutat, Werkzeug, Ergebnis). */
    public final NamespacedKey itemId;
    /** ID eines Custom-Seeds (zum Anpflanzen und fuer Drops). */
    public final NamespacedKey seedId;
    /** Aktuelle Stufe innerhalb eines sequentiellen Koch-Ablaufs. */
    public final NamespacedKey stage;
    /** Markiert eine an einer Kette aufgehaengte Item-Entity. */
    public final NamespacedKey chainHung;
    /** Markiert eine im Kessel schwebende, kochende Item-Entity. */
    public final NamespacedKey cookingFloat;
    /** Markiert ein visuelles Crop-Overlay fuer Custom-Pflanzen. */
    public final NamespacedKey cropDisplay;

    public ItemKeys(SmokeAndSalt plugin) {
        this.itemId = new NamespacedKey(plugin, "item_id");
        this.seedId = new NamespacedKey(plugin, "seed_id");
        this.stage = new NamespacedKey(plugin, "stage");
        this.chainHung = new NamespacedKey(plugin, "chain_hung");
        this.cookingFloat = new NamespacedKey(plugin, "cooking_float");
        this.cropDisplay = new NamespacedKey(plugin, "crop_display");
    }
}
