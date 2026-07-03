package de.yourshika.smokeandsalt.module;

import org.bukkit.inventory.ItemStack;

/**
 * Liefert das visuelle Modell/Texturen fuer Smoke &amp; Salt Items. Standardmaessig
 * kommt die Vanilla-Optik zum Einsatz; ein aktives externes Modul (Oraxen) kann
 * die Optik ueber die {@code provider-id} eines Items ueberlagern.
 */
public interface CustomItemProvider {

    /** Anbieter-Schluessel (z.B. {@code oraxen}). */
    String id();

    /** Uebernimmt Modell-Komponenten des externen Items anhand seiner Provider-ID. */
    void apply(ItemStack item, String providerId);
}
