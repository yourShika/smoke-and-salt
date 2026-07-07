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
 * Linksklick gibt 1 Stueck, Shift-Klick gibt 16. Bei mehr als einer Seite wird
 * unten mit Pfeilen geblaettert.
 */
public final class GiveMenu {

    private static final int PAGE_SIZE = 45;

    private GiveMenu() {
    }

    /** Ein Eintrag im Give-Menue (Custom-Item oder Seed). */
    private record Entry(String id, boolean seed) {
    }

    public static void open(SmokeAndSalt plugin, Player player) {
        open(plugin, player, 0);
    }

    public static void open(SmokeAndSalt plugin, Player player, int page) {
        List<Entry> entries = new ArrayList<>();
        for (ItemDefinition def : plugin.items().all()) entries.add(new Entry(def.id(), false));
        for (SeedDefinition def : plugin.seeds().all()) entries.add(new Entry(def.id(), true));

        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, pages - 1));

        MenuHolder holder = new MenuHolder("give");
        Inventory inv = Bukkit.createInventory(holder, 54,
                Text.line("<gradient:#f2c87a:#c65b3a><bold>Give Items</bold></gradient> <dark_gray>("
                        + (current + 1) + "/" + pages + ")"));
        holder.setInventory(inv);

        for (int i = 45; i < 54; i++) holder.set(i, Icons.accent());

        int start = current * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int idx = start; idx < end; idx++) {
            Entry entry = entries.get(idx);
            ItemStack icon = entry.seed()
                    ? plugin.seeds().create(entry.id(), 1)
                    : plugin.items().create(entry.id(), 1);
            if (icon == null) continue;
            appendLore(icon, entry.id(), entry.seed() ? "Custom seed" : "Custom item");
            String id = entry.id();
            boolean seed = entry.seed();
            holder.set(slot++, icon, (p, e) -> giveItem(plugin, p, id, e.isShiftClick() ? 16 : 1, seed));
        }

        if (entries.isEmpty()) {
            holder.set(22, Icons.of(Material.STRUCTURE_VOID, "<gray>No items registered",
                    "<dark_gray>Define items in config.yml",
                    "<dark_gray>under 'items:' or 'seeds:'."));
        }

        // Blaetter-Navigation.
        if (current > 0) {
            holder.set(45, Icons.of(Material.ARROW, "<yellow>Previous page",
                    "<dark_gray>Page " + current + "/" + pages),
                    (p, e) -> open(plugin, p, current - 1));
        }
        holder.set(49, Icons.of(Material.PAPER, "<white>Page " + (current + 1) + "/" + pages,
                "<dark_gray>" + entries.size() + " entries total"));
        if (current < pages - 1) {
            holder.set(53, Icons.of(Material.ARROW, "<yellow>Next page",
                    "<dark_gray>Page " + (current + 2) + "/" + pages),
                    (p, e) -> open(plugin, p, current + 1));
        }
        holder.set(48, Icons.of(Material.BARRIER, "<red>Close"), (p, e) -> p.closeInventory());

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
        lore.add(Text.line("<yellow>Click: 1 · Shift-click: 16"));
        meta.lore(lore);
        icon.setItemMeta(meta);
    }
}
