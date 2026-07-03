package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Zentrale Uebersichts-GUI ({@code /sas} bzw. {@code /sas help}). Zeigt alle
 * Koch-Stationen und - je nach Berechtigung - schnelle Buttons zu Rezepten,
 * Modulen, Give, Assets, Version, Update und Reload.
 */
public final class HelpMenu {

    private HelpMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        MenuHolder holder = new MenuHolder("help");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.line("<gradient:#e2a76f:#c65b3a><bold>Smoke & Salt</bold></gradient>"));
        holder.setInventory(inv);

        for (int i = 0; i < 54; i++) holder.set(i, Icons.accent());

        holder.set(4, Icons.of(Material.COOKED_BEEF,
                "<gradient:#e2a76f:#c65b3a><bold>Smoke & Salt</bold></gradient>",
                "<gray>Ein vanilla-treues Koch-System.",
                "<gray>Version <white>" + plugin.getPluginMeta().getVersion(),
                " ",
                "<dark_gray>Klicke auf eine Station fuer Infos."));

        // --- Stationen ---
        holder.set(19, station(Material.SMOKER, "Smoker",
                "Rechtsklick auf einen Smoker mit einer",
                "passenden Zutat startet das Raeuchern."));
        holder.set(20, station(Material.CAMPFIRE, "Lagerfeuer",
                "Rechtsklick auf ein brennendes Lagerfeuer",
                "gart die Zutat mit Rauch-Partikeln."));
        holder.set(21, station(Material.WATER_CAULDRON, "Wasserkessel",
                "Kessel ueber einer Waermequelle kocht.",
                "Item hineinwerfen - es schwebt und kocht.",
                "Fuer Kochen, Waschen, Bruehen, Suppe."));
        holder.set(22, station(Material.LAVA_CAULDRON, "Lavakessel",
                "Vorsichtige Frittier-/Bratstation.",
                "Nur bestimmte Items funktionieren."));
        holder.set(23, station(Material.IRON_AXE, "Schneiden",
                "Axt in die Haupthand, Zutat in die",
                "Zweithand - Rechtsklick schneidet."));
        holder.set(24, station(Material.WHEAT_SEEDS, "Custom Seeds",
                "Anpflanzbar auf Ackerland.",
                "Drops von Gras und Komposter."));
        holder.set(25, station(Material.IRON_CHAIN, "Ketten",
                "Aufhaengung fuer Kessel-Behang",
                "oder Raeucherware."));

        // --- Befehls-Buttons ---
        holder.set(37, Icons.of(Material.BOOK, "<yellow><bold>Rezepte</bold>",
                "<gray>Uebersicht der Koch-Rezepte.", " ", "<dark_gray>» /sas recipes"),
                (p, e) -> p.performCommand("sas recipes"));

        holder.set(38, Icons.of(Material.NAME_TAG, "<aqua><bold>Version</bold>",
                "<gray>Plugin-Version und Info.", " ", "<dark_gray>» /sas version"),
                (p, e) -> {
                    p.closeInventory();
                    p.performCommand("sas version");
                });

        if (player.hasPermission("smokeandsalt.admin.modules")) {
            holder.set(39, Icons.of(Material.COMPARATOR, "<light_purple><bold>Module</bold>",
                    "<gray>Externe Hooks (Oraxen, PlaceholderAPI)", "<gray>live ab-/anschalten.", " ",
                    "<dark_gray>» /sas modules"),
                    (p, e) -> ModulesMenu.open(plugin, p));
        }
        if (player.hasPermission("smokeandsalt.admin.give")) {
            holder.set(40, Icons.of(Material.CHEST, "<gold><bold>Give</bold>",
                    "<gray>Custom-Items ausgeben.", " ", "<dark_gray>» /sas give"),
                    (p, e) -> GiveMenu.open(plugin, p));
        }
        if (player.hasPermission("smokeandsalt.admin.assets")) {
            holder.set(41, Icons.of(Material.ITEM_FRAME, "<green><bold>Assets</bold>",
                    "<gray>Oraxen-Assets: Status & Redeploy.", " ", "<dark_gray>» /sas assets"),
                    (p, e) -> AssetsMenu.open(plugin, p));
        }
        if (player.hasPermission("smokeandsalt.admin.update")) {
            holder.set(42, Icons.of(Material.LIME_DYE, "<green><bold>Update</bold>",
                    "<gray>Neueste Release von GitHub laden.", " ", "<dark_gray>» /sas update"),
                    (p, e) -> {
                        p.closeInventory();
                        p.performCommand("sas update");
                    });
        }
        if (player.hasPermission("smokeandsalt.admin.reload")) {
            holder.set(43, Icons.of(Material.BLAZE_POWDER, "<red><bold>Reload</bold>",
                    "<gray>Config, Nachrichten, Items,", "<gray>Rezepte und Module neu laden.", " ",
                    "<dark_gray>» /sas reload"),
                    (p, e) -> {
                        p.closeInventory();
                        p.performCommand("sas reload");
                    });
        }

        player.openInventory(inv);
    }

    private static org.bukkit.inventory.ItemStack station(Material material, String name, String... desc) {
        String[] lore = new String[desc.length + 2];
        lore[0] = "<dark_gray>Koch-Station";
        lore[1] = " ";
        for (int i = 0; i < desc.length; i++) lore[i + 2] = "<gray>" + desc[i];
        return Icons.of(material, "<white><bold>" + name + "</bold>", lore);
    }
}
