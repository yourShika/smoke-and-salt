package de.yourshika.smokeandsalt.item;

import java.util.List;

/**
 * Deklarative Beschreibung eines Custom-Items. Wird aus der config.yml (Abschnitt
 * {@code items:}) oder ueber die API in die {@link ItemRegistry} geladen. Enthaelt
 * bewusst keinerlei fest verdrahtete Inhalte - konkrete Zutaten und Gerichte
 * werden spaeter hier ergaenzt.
 *
 * @param id          stabile, kleingeschriebene ID (z.B. {@code chopped_carrot})
 * @param material    Basis-Material des Items (Vanilla-Optik ohne Oraxen)
 * @param displayName MiniMessage-Anzeigename
 * @param lore        MiniMessage-Zeilen fuer die Lore (kann leer sein)
 * @param providerId  Oraxen-Item-ID fuer Custom-Textur (kann {@code null} sein)
 * @param glow        soll das Item verzaubert schimmern?
 */
public record ItemDefinition(
        String id,
        org.bukkit.Material material,
        String displayName,
        List<String> lore,
        String providerId,
        boolean glow
) {
    public ItemDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ItemDefinition benoetigt eine id");
        }
        if (material == null) {
            throw new IllegalArgumentException("ItemDefinition '" + id + "' benoetigt ein Material");
        }
        lore = lore == null ? List.of() : List.copyOf(lore);
    }
}
