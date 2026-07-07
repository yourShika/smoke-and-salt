package de.yourshika.smokeandsalt.gui;

import org.bukkit.Material;

/** Kategorien fuer den Rezeptbrowser. */
public enum RecipeCategory {
    // Uebergreifende Kategorien: fertige Gerichte (essbar) vs. Zwischenprodukte.
    DISHES("Finished Dishes", Material.COOKED_BEEF),
    INGREDIENTS("Ingredients", Material.WHEAT),
    // Rezepte, die ein Vanilla-Item ausgeben (z.B. Teig -> Brot, Cotton -> Wolle/Faden).
    VANILLA("Vanilla Outputs", Material.STRING),
    // Nach Station.
    SMOKER("Smoker", Material.SMOKER),
    CAMPFIRE("Campfire", Material.CAMPFIRE),
    WATER_CAULDRON("Water Cauldron", Material.CAULDRON),
    LAVA_CAULDRON("Lava Cauldron", Material.LAVA_BUCKET),
    CUTTING("Cutting", Material.IRON_AXE),
    CRAFTING("Crafting", Material.CRAFTING_TABLE),
    SEEDS("Seeds", Material.WHEAT_SEEDS);

    /** Ist dies eine uebergreifende (nicht stationsgebundene) Kategorie? */
    public boolean isCrossCutting() {
        return this == DISHES || this == INGREDIENTS || this == VANILLA;
    }

    private final String displayName;
    private final Material icon;

    RecipeCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }
}
