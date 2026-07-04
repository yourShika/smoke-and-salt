package de.yourshika.smokeandsalt.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Vereinheitlichte Ansicht eines Rezepts fuer die GUI - unabhaengig davon, ob es
 * aus dem Smoker, Lagerfeuer, Kessel oder der Werkbank stammt.
 *
 * @param id         stabile View-ID
 * @param category   Kategorie im Rezeptbrowser
 * @param station    Anzeigename der Station
 * @param stationIcon Icon-Material der Station
 * @param variants   alternative Zutaten-Sets
 * @param result     Ergebnis-Icon
 * @param cookTicks  Kochzeit in Ticks (0 = ohne Zeitangabe, z.B. Werkbank)
 * @param note       optionale Detailnotiz
 */
public record RecipeView(String id, RecipeCategory category, String station, Material stationIcon,
                         List<List<ItemStack>> variants, ItemStack result, int cookTicks, String note) {

    public RecipeView(String id, RecipeCategory category, String station, Material stationIcon,
                      List<ItemStack> inputs, ItemStack result, int cookTicks) {
        this(id, category, station, stationIcon, List.of(inputs), result, cookTicks, null);
    }

    public RecipeView {
        variants = variants.stream().map(List::copyOf).toList();
    }

    public static RecipeView single(String id, RecipeCategory category, String station, Material stationIcon,
                                    List<ItemStack> inputs, ItemStack result, int cookTicks, String note) {
        return new RecipeView(id, category, station, stationIcon, List.of(inputs), result, cookTicks, note);
    }

    public List<ItemStack> inputs(int variant) {
        if (variants.isEmpty()) return List.of();
        return variants.get(Math.max(0, Math.min(variant, variants.size() - 1)));
    }
}
