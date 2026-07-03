package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI fuer {@code /sas recipes}: zeigt pro Station die Anzahl registrierter
 * Rezepte. Standardmaessig sind noch keine Rezepte hinterlegt - sie kommen ueber
 * die config.yml hinzu.
 */
public final class RecipesMenu {

    private RecipesMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        MenuHolder holder = new MenuHolder("recipes");
        Inventory inv = Bukkit.createInventory(holder, 27,
                Text.line("<gradient:#e2a76f:#c65b3a><bold>Koch-Rezepte</bold></gradient>"));
        holder.setInventory(inv);

        for (int i = 0; i < 27; i++) holder.set(i, Icons.accent());

        int total = plugin.cooking().registry().size();
        holder.set(4, Icons.of(Material.BOOK, "<gold><bold>Rezepte</bold>",
                "<gray>Registrierte Rezepte gesamt: <white>" + total,
                " ",
                total == 0 ? "<dark_gray>Noch keine Rezepte hinterlegt." : "<dark_gray>Nach Station aufgeteilt:",
                "<dark_gray>Rezepte werden in der config.yml definiert."));

        int[] slots = {10, 11, 12, 13, 14};
        CookingStation[] stations = CookingStation.values();
        Material[] icons = {
                Material.SMOKER, Material.CAMPFIRE, Material.WATER_CAULDRON,
                Material.LAVA_CAULDRON, Material.IRON_AXE
        };
        for (int i = 0; i < stations.length; i++) {
            CookingStation station = stations[i];
            List<CookingRecipe> recipes = plugin.cooking().registry().forStation(station);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + station.description());
            lore.add(" ");
            lore.add("<gray>Rezepte: <white>" + recipes.size());
            int shown = 0;
            for (CookingRecipe r : recipes) {
                if (shown++ >= 6) {
                    lore.add("<dark_gray>... und weitere");
                    break;
                }
                lore.add("<dark_gray>- " + r.id());
            }
            holder.set(slots[i], Icons.of(icons[i], "<white><bold>" + station.displayName() + "</bold>",
                    lore.toArray(new String[0])));
        }

        holder.set(22, Icons.of(Material.BARRIER, "<red>Schliessen"), (p, e) -> p.closeInventory());
        player.openInventory(inv);
    }
}
