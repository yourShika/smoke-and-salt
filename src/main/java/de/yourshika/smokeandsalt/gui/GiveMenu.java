package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.item.ItemDefinition;
import de.yourshika.smokeandsalt.seed.SeedDefinition;
import de.yourshika.smokeandsalt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI fuer {@code /sas give}: listet alle registrierten Custom-Items und Seeds.
 * Linksklick gibt 1 Stueck, Shift-Klick gibt 16. Standardmaessig leer, bis Items
 * ueber die config.yml definiert werden.
 */
public final class GiveMenu {

    private GiveMenu() {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        MenuHolder holder = new MenuHolder("give");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.line("<gradient:#f2c87a:#c65b3a><bold>Items ausgeben</bold></gradient>"));
        holder.setInventory(inv);

        for (int i = 45; i < 54; i++) holder.set(i, Icons.accent());

        int slot = 0;
        for (ItemDefinition def : plugin.items().all()) {
            if (slot >= 45) break;
            ItemStack icon = plugin.items().create(def.id(), 1);
            if (icon == null) continue;
            appendLore(icon, def.id(), "Custom-Item");
            String id = def.id();
            holder.set(slot++, icon, (p, e) -> giveItem(plugin, p, id, e.isShiftClick() ? 16 : 1, false));
        }
        for (SeedDefinition def : plugin.seeds().all()) {
            if (slot >= 45) break;
            ItemStack icon = plugin.seeds().create(def.id(), 1);
            if (icon == null) continue;
            appendLore(icon, def.id(), "Custom-Seed");
            String id = def.id();
            holder.set(slot++, icon, (p, e) -> giveItem(plugin, p, id, e.isShiftClick() ? 16 : 1, true));
        }

        if (slot == 0) {
            holder.set(22, Icons.of(Material.STRUCTURE_VOID, "<gray>Keine Items registriert",
                    "<dark_gray>Definiere Items in der config.yml",
                    "<dark_gray>unter 'items:' bzw. 'seeds:'."));
        }

        holder.set(49, Icons.of(Material.BARRIER, "<red>Schliessen"), (p, e) -> p.closeInventory());
        player.openInventory(inv);
    }

    private static void giveItem(SmokeAndSalt plugin, Player player, String id, int amount, boolean seed) {
        ItemStack stack = seed ? plugin.seeds().create(id, amount) : plugin.items().create(id, amount);
        if (stack == null) return;
        var leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(s -> player.getWorld().dropItemNaturally(player.getLocation(), s));
        plugin.messages().send(player, "give.self",
                de.yourshika.smokeandsalt.config.MessageManager.ph("amount", String.valueOf(amount)),
                de.yourshika.smokeandsalt.config.MessageManager.ph("item", id));
    }

    private static void appendLore(ItemStack icon, String id, String type) {
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return;
        List<net.kyori.adventure.text.Component> lore = meta.hasLore()
                ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Text.line(" "));
        lore.add(Text.line("<dark_gray>" + type + " · " + id));
        lore.add(Text.line("<yellow>Klick: 1 · Shift-Klick: 16"));
        meta.lore(lore);
        icon.setItemMeta(meta);
    }
}
