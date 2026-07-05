package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.cooking.CauldronStation;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * {@link InventoryHolder} fuer die funktionale Kessel-GUI. Anders als der reine
 * Anzeige-{@link MenuHolder} erlaubt diese GUI echtes Ein- und Auslegen von Items
 * in bestimmten Slots; die Logik dazu liegt in der zugehoerigen Station. Der
 * Holder dient nur zur Identifikation des Kessel-Blocks.
 */
public final class CauldronMenuHolder implements InventoryHolder {

    private final CauldronStation station;
    private final String stationKey;

    public CauldronMenuHolder(CauldronStation station, String stationKey) {
        this.station = station;
        this.stationKey = stationKey;
    }

    public CauldronStation station() {
        return station;
    }

    public String stationKey() {
        return stationKey;
    }

    /** Nicht genutzt - die GUI wird ueber die offene View gefunden, nicht ueber den Holder. */
    @Override
    public Inventory getInventory() {
        return null;
    }
}
