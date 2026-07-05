package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CauldronStation;
import de.yourshika.smokeandsalt.gui.CauldronMenuHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Klick-/Drag-Steuerung fuer die funktionale Kessel-GUI. Eingabe-Slots erlauben
 * freies Ein-/Auslegen, Ausgabe-Slots nur das Entnehmen; Deko-Slots sind gesperrt.
 * Nach jeder erlaubten Aenderung wird der Container aus der GUI nachgezogen.
 */
public final class CauldronMenuListener implements Listener {

    private final SmokeAndSalt plugin;

    public CauldronMenuListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CauldronMenuHolder holder)) return;
        CauldronStation station = holder.station();
        Inventory top = event.getView().getTopInventory();
        int raw = event.getRawSlot();
        boolean inTop = raw < top.getSize();
        InventoryAction action = event.getAction();

        // Doppelklick-Sammeln koennte aus Ausgabe-/Deko-Slots ziehen -> sperren.
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        if (inTop) {
            if (station.isInputSlot(raw)) {
                scheduleSync(station, holder.stationKey(), top);
            } else if (station.isOutputSlot(raw)) {
                switch (action) {
                    case PICKUP_ALL, PICKUP_HALF, PICKUP_SOME, PICKUP_ONE,
                         DROP_ALL_SLOT, DROP_ONE_SLOT, MOVE_TO_OTHER_INVENTORY ->
                            scheduleSync(station, holder.stationKey(), top);
                    default -> event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // Klick im Spieler-Inventar: Shift-Klick gezielt in die Eingabe leiten.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            ItemStack moved = event.getCurrentItem();
            if (moved == null || moved.getType().isAir()) return;
            ItemStack leftover = station.shiftIntoInput(top, moved.clone());
            event.setCurrentItem(leftover);
            scheduleSync(station, holder.stationKey(), top);
        }
        // Sonstige Interaktionen im eigenen Inventar sind erlaubt.
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CauldronMenuHolder holder)) return;
        CauldronStation station = holder.station();
        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && !station.isInputSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        scheduleSync(station, holder.stationKey(), event.getView().getTopInventory());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CauldronMenuHolder holder) {
            holder.station().handleClose(holder.stationKey(), event.getView().getTopInventory());
        }
    }

    /** Zieht den Container nach dem naechsten Tick aus der (dann aktualisierten) GUI nach. */
    private void scheduleSync(CauldronStation station, String key, Inventory top) {
        plugin.getServer().getScheduler().runTask(plugin, () -> station.handleChange(key, top));
    }
}
