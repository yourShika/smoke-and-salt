package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Central overview GUI ({@code /sas} / {@code /sas help}). Shows all cooking
 * stations and - depending on permission - quick buttons for recipes, modules,
 * give, assets, version, update and reload.
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
                "<gray>A vanilla-faithful cooking system.",
                "<gray>Version <white>" + plugin.getPluginMeta().getVersion(),
                " ",
                "<dark_gray>Click a station for info."));

        // --- Stations ---
        holder.set(19, station(Material.SMOKER, "Smoker",
                "Put an ingredient into a fuelled smoker",
                "to smoke it - just like vanilla."));
        holder.set(20, station(Material.CAMPFIRE, "Campfire",
                "Place an ingredient on a lit campfire",
                "and wait until it is done."));
        holder.set(21, station(Material.CAULDRON, "Water Cauldron",
                "A cauldron over a heat source boils.",
                "Throw items in - they float and cook.",
                "For cooking, washing, brewing, soup.",
                "Right-click to cancel the process."));
        holder.set(22, station(Material.LAVA_BUCKET, "Lava Cauldron",
                "A careful frying/roasting station.",
                "Only certain items work."));
        holder.set(23, station(Material.IRON_AXE, "Cutting",
                "Axe in one hand, ingredient in the",
                "other hand - right-click to cut."));
        holder.set(24, station(Material.WHEAT_SEEDS, "Custom Seeds",
                "Plantable on farmland.",
                "Drops from grass, seagrass, composter."));

        // --- Command buttons ---
        holder.set(37, Icons.of(Material.BOOK, "<yellow><bold>Recipes</bold>",
                "<gray>Browse all cooking recipes.", " ", "<dark_gray>-> /sas recipes"),
                (p, e) -> RecipesMenu.open(plugin, p));

        holder.set(38, Icons.of(Material.NAME_TAG, "<aqua><bold>Version</bold>",
                "<gray>Plugin version and info.", " ", "<dark_gray>-> /sas version"),
                (p, e) -> {
                    p.closeInventory();
                    p.performCommand("sas version");
                });

        if (player.hasPermission("smokeandsalt.admin.modules")) {
            holder.set(39, Icons.of(Material.COMPARATOR, "<light_purple><bold>Modules</bold>",
                    "<gray>Toggle external hooks (Oraxen,", "<gray>PlaceholderAPI) live.", " ",
                    "<dark_gray>-> /sas modules"),
                    (p, e) -> ModulesMenu.open(plugin, p));
        }
        if (player.hasPermission("smokeandsalt.admin.give")) {
            holder.set(40, Icons.of(Material.CHEST, "<gold><bold>Give</bold>",
                    "<gray>Give out custom items.", " ", "<dark_gray>-> /sas give"),
                    (p, e) -> GiveMenu.open(plugin, p));
        }
        if (player.hasPermission("smokeandsalt.admin.assets")) {
            holder.set(41, Icons.of(Material.ITEM_FRAME, "<green><bold>Assets</bold>",
                    "<gray>Oraxen assets: status & redeploy.", " ", "<dark_gray>-> /sas assets"),
                    (p, e) -> AssetsMenu.open(plugin, p));
        }
        if (player.hasPermission("smokeandsalt.admin.update")) {
            holder.set(42, Icons.of(Material.LIME_DYE, "<green><bold>Update</bold>",
                    "<gray>Download the latest GitHub release.", " ", "<dark_gray>-> /sas update"),
                    (p, e) -> {
                        p.closeInventory();
                        p.performCommand("sas update");
                    });
        }
        if (player.hasPermission("smokeandsalt.admin.reload")) {
            holder.set(43, Icons.of(Material.BLAZE_POWDER, "<red><bold>Reload</bold>",
                    "<gray>Reload config, messages, items,", "<gray>recipes and modules.", " ",
                    "<dark_gray>-> /sas reload"),
                    (p, e) -> {
                        p.closeInventory();
                        p.performCommand("sas reload");
                    });
        }

        player.openInventory(inv);
    }

    private static org.bukkit.inventory.ItemStack station(Material material, String name, String... desc) {
        String[] lore = new String[desc.length + 2];
        lore[0] = "<dark_gray>Cooking station";
        lore[1] = " ";
        for (int i = 0; i < desc.length; i++) lore[i + 2] = "<gray>" + desc[i];
        return Icons.of(material, "<white><bold>" + name + "</bold>", lore);
    }
}
