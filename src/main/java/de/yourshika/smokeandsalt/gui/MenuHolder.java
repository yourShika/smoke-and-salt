package de.yourshika.smokeandsalt.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Generischer {@link InventoryHolder} fuer alle Smoke &amp; Salt GUIs. Haelt pro
 * Slot eine optionale Klick-Aktion, sodass ein einziger Listener alle Menues
 * bedienen kann.
 */
public final class MenuHolder implements InventoryHolder {

    /** Klick-Aktion fuer einen GUI-Slot. */
    @FunctionalInterface
    public interface Click {
        void run(Player player, InventoryClickEvent event);
    }

    private final String type;
    private final Map<Integer, Click> actions = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Setzt ein Item ohne Aktion (reine Deko). */
    public void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** Setzt ein Item mit optionaler Klick-Aktion. */
    public void set(int slot, ItemStack item, Click click) {
        if (inventory != null) inventory.setItem(slot, item);
        if (click != null) actions.put(slot, click);
        else actions.remove(slot);
    }

    public Click actionAt(int slot) {
        return actions.get(slot);
    }
}
