package de.yourshika.smokeandsalt.cooking;

/**
 * Die Koch-Stationen von Smoke &amp; Salt. Jede Station hat ein eigenes
 * Interaktions-Muster und eigene Partikel-/Sound-Funktionen; die konkreten
 * Rezepte pro Station kommen spaeter dazu.
 */
public enum CookingStation {

    /** Raeuchern im Smoker-Block (Custom-Items, Rauch-Partikel). */
    SMOKER("Smoker", "Raeuchern im Smoker"),

    /** Garen ueber dem Lagerfeuer. */
    CAMPFIRE("Lagerfeuer", "Garen ueber dem Lagerfeuer"),

    /** Kochen/Waschen/Bruehen/Suppe in kochendem Wasser (Kessel ueber Waermequelle). */
    CAULDRON_WATER("Wasserkessel", "Kochen in kochendem Wasser"),

    /** Braten/Frittieren in einem Lavakessel (nur bestimmte Items). */
    CAULDRON_LAVA("Lavakessel", "Braten/Frittieren in Lava"),

    /** Schneiden mit Axt (Haupthand) und Zutat (Zweithand). */
    CUTTING("Schneidebrett", "Schneiden mit der Axt");

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
