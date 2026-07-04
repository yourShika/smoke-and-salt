package de.yourshika.smokeandsalt.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Vereinheitlichte Ansicht eines Rezepts fuer die GUI - unabhaengig davon, ob es
 * aus dem Smoker, Lagerfeuer, Kessel oder der Werkbank stammt.
 *
 * @param station    Anzeigename der Station
 * @param stationIcon Icon-Material der Station
 * @param inputs     Zutaten-Icons
 * @param result     Ergebnis-Icon
 * @param cookTicks  Kochzeit in Ticks (0 = ohne Zeitangabe, z.B. Werkbank)
 */
public record RecipeView(String station, Material stationIcon, List<ItemStack> inputs,
                         ItemStack result, int cookTicks) {

    public RecipeView {
        inputs = List.copyOf(inputs);
    }
}
