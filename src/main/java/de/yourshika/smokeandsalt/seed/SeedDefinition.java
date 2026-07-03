package de.yourshika.smokeandsalt.seed;

import org.bukkit.Material;

/**
 * Deklarative Beschreibung eines Custom-Seeds. Wird aus der config.yml
 * ({@code seeds:}) geladen und ist standardmaessig leer - konkrete Pflanzen
 * kommen spaeter dazu.
 *
 * @param id             stabile ID des Seeds
 * @param material       Basis-Material des Seed-Items (Vanilla-Optik)
 * @param displayName    MiniMessage-Anzeigename
 * @param providerId     Oraxen-Item-ID fuer Custom-Textur (kann {@code null} sein)
 * @param cropMaterial   Vanilla-Crop-Block, der beim Anpflanzen gesetzt wird (Ageable)
 * @param resultItemId   Custom-Item-ID, die bei voller Reife geerntet wird (oder {@code null})
 * @param resultMaterial Vanilla-Material als Ernte, falls kein Custom-Item (oder {@code null})
 * @param resultAmount   Anzahl der Ernte
 * @param grassChance    Drop-Wahrscheinlichkeit beim Abbauen von Gras (0..1)
 * @param composterChance Drop-Wahrscheinlichkeit an einem vollen Komposter (0..1)
 */
public record SeedDefinition(
        String id,
        Material material,
        String displayName,
        String providerId,
        Material cropMaterial,
        String resultItemId,
        Material resultMaterial,
        int resultAmount,
        double grassChance,
        double composterChance
) {
    public SeedDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("SeedDefinition benoetigt eine id");
        }
        if (material == null) material = Material.WHEAT_SEEDS;
        if (resultAmount < 1) resultAmount = 1;
    }

    public boolean plantable() {
        return cropMaterial != null;
    }
}
