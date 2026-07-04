package de.yourshika.smokeandsalt.seed;

import org.bukkit.Material;

/**
 * Deklarative Beschreibung eines Custom-Seeds. Wird aus der config.yml
 * ({@code seeds:}) geladen oder ueber die API registriert.
 *
 * @param id              stabile ID des Seeds
 * @param material        Basis-Material des Seed-Items (Vanilla-Optik)
 * @param displayName     MiniMessage-Anzeigename
 * @param providerId      Oraxen-Item-ID fuer Custom-Textur (kann {@code null} sein)
 * @param cropMaterial    Vanilla-Crop-Block, der beim Anpflanzen gesetzt wird (Ageable)
 * @param resultItemId    Custom-Item-ID, die bei voller Reife geerntet wird (oder {@code null})
 * @param resultMaterial  Vanilla-Material als Ernte, falls kein Custom-Item (oder {@code null})
 * @param resultAmount    Anzahl der Ernte
 * @param seedReturnMin   min. Anzahl Seeds, die bei der Ernte zurueckkommen
 * @param seedReturnMax   max. Anzahl Seeds, die bei der Ernte zurueckkommen
 * @param grassChance     Drop-Chance beim Abbauen von Land-Gras (0..1)
 * @param seagrassChance  Drop-Chance beim Abbauen von Seegras (0..1)
 * @param composterChance Drop-Chance an einem vollen Komposter (0..1)
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
        int seedReturnMin,
        int seedReturnMax,
        double grassChance,
        double seagrassChance,
        double composterChance
) {
    public SeedDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("SeedDefinition benoetigt eine id");
        }
        if (material == null) material = Material.WHEAT_SEEDS;
        if (resultAmount < 1) resultAmount = 1;
        if (seedReturnMin < 0) seedReturnMin = 0;
        if (seedReturnMax < seedReturnMin) seedReturnMax = seedReturnMin;
    }

    public boolean plantable() {
        return cropMaterial != null;
    }
}
