package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.module.Module;
import de.yourshika.smokeandsalt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * GUI fuer {@code /sas modules}: zeigt alle externen Module mit Live-Status und
 * erlaubt Admins, jedes Modul per Klick an-/auszuschalten (mit Live-Reload).
 */
public final class ModulesMenu {

    private ModulesMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        MenuHolder holder = new MenuHolder("modules");
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.line("<gradient:#a68bff:#5be8d4><bold>Externe Module</bold></gradient>"));
        holder.setInventory(inv);

        for (int i = 0; i < 27; i++) holder.set(i, Icons.accent());

        holder.set(4, Icons.of(Material.COMPARATOR, "<gold><bold>Externe Hooks</bold>",
                "<gray>Hooks aktivieren sich automatisch,",
                "<gray>sobald das Plugin installiert ist.",
                "<gray>Einzeln per Klick ab-/anschalten."));

        int slot = 10;
        for (Module module : plugin.moduleManager().modules()) {
            if (slot > 16) break;
            holder.set(slot, moduleItem(module), (p, e) -> {
                if (module.required() && module.isActive()) {
                    plugin.messages().send(p, "modules.required");
                    return;
                }
                plugin.setModuleEnabled(module.id(), !module.isEnabledInConfig());
                open(plugin, p); // neu aufbauen
            });
            slot++;
        }

        holder.set(22, Icons.of(Material.BARRIER, "<red>Schliessen"), (p, e) -> p.closeInventory());

        player.openInventory(inv);
    }

    private static ItemStack moduleItem(Module module) {
        Material material;
        if (module.isActive()) {
            material = Material.LIME_DYE;
        } else if (!module.isPluginPresent()) {
            material = Material.RED_DYE;
        } else if (!module.isEnabledInConfig()) {
            material = Material.ORANGE_DYE;
        } else {
            material = Material.GRAY_DYE;
        }

        return Icons.of(material, "<white><bold>" + module.displayName() + "</bold>",
                "<gray>" + module.description(),
                " ",
                "<gray>Benoetigt: <white>" + module.requiredPlugin(),
                "<gray>Installiert: " + yesNo(module.isPluginPresent()),
                "<gray>In Config aktiviert: " + yesNo(module.isEnabledInConfig()),
                " ",
                "<gray>Status: " + (module.isActive() ? "<green><bold>AKTIV" : "<red><bold>INAKTIV"),
                " ",
                "<yellow>Klicken zum Umschalten");
    }

    private static String yesNo(boolean value) {
        return value ? "<green>ja" : "<red>nein";
    }
}
