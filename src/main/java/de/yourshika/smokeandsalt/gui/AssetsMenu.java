package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.module.OraxenAssetDeployer;
import de.yourshika.smokeandsalt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * GUI fuer {@code /sas assets}: zeigt den Status der mitgelieferten Oraxen-Assets
 * und erlaubt einen erneuten Redeploy.
 */
public final class AssetsMenu {

    private AssetsMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        MenuHolder holder = new MenuHolder("assets");
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.line("<gradient:#5be88a:#5be8d4><bold>Oraxen-Assets</bold></gradient>"));
        holder.setInventory(inv);

        for (int i = 0; i < 27; i++) holder.set(i, Icons.accent());

        OraxenAssetDeployer.AssetStatus status = new OraxenAssetDeployer(plugin).status();

        holder.set(11, Icons.of(status.oraxenPresent() ? Material.LIME_DYE : Material.RED_DYE,
                "<white><bold>Oraxen</bold>",
                "<gray>Installiert: " + (status.oraxenPresent() ? "<green>ja" : "<red>nein"),
                "<gray>Modul aktiv: " + (plugin.moduleManager().isActive("oraxen") ? "<green>ja" : "<red>nein")));

        holder.set(13, Icons.of(Material.ITEM_FRAME, "<gold><bold>Asset-Status</bold>",
                "<gray>Gebuendelte Version: <white>" + status.bundledVersion(),
                "<gray>Ausgerollte Version: <white>" + status.deployedVersion(),
                "<gray>Verwaltete Dateien: <white>" + status.managed() + "/" + status.total(),
                "<gray>Fehlend in Oraxen: <white>" + status.missing()));

        holder.set(15, Icons.of(Material.ANVIL, "<yellow><bold>Redeploy</bold>",
                "<gray>Stellt die mitgelieferten Assets erneut",
                "<gray>in Oraxen bereit (mit Backup).", " ",
                "<yellow>Klicken zum Ausfuehren",
                "<dark_gray>Danach '/oraxen reload' ausfuehren."),
                (p, e) -> {
                    p.closeInventory();
                    p.performCommand("sas assets redeploy");
                });

        player.openInventory(inv);
    }
}
