package de.yourshika.smokeandsalt.cooking;

/**
 * Die Koch-Stationen von Smoke &amp; Salt. Jede Station hat ein eigenes
 * Interaktions-Muster und eigene Partikel-/Sound-Funktionen; die konkreten
 * Rezepte pro Station kommen spaeter dazu.
 */
public enum CookingStation {

    /** Smoking in the smoker block (custom items, smoke particles). */
    SMOKER("Smoker", "Smoking in the smoker"),

    /** Cooking on the campfire. */
    CAMPFIRE("Campfire", "Cooking on the campfire"),

    /** Cooking/washing/brewing/soup in boiling water (cauldron over heat). */
    CAULDRON_WATER("Water Cauldron", "Cooking in boiling water"),

    /** Frying/roasting in a lava cauldron (only certain items). */
    CAULDRON_LAVA("Lava Cauldron", "Frying/roasting in lava"),

    /** Cutting with an axe in one hand and an ingredient in the other. */
    CUTTING("Cutting Board", "Cutting with the axe");

    private final String displayName;
    private final String description;

    CookingStation(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
