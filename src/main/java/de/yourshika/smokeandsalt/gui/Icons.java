package de.yourshika.smokeandsalt.gui;

import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Helfer zum Bauen von GUI-Icons mit MiniMessage-Name und -Lore.
 */
public final class Icons {

    private Icons() {
    }

    public static ItemStack of(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.line(name));
            if (loreLines.length > 0) {
                List<Component> lore = new ArrayList<>();
                for (String s : loreLines) lore.add(Text.line(s));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filler() {
        return of(Material.GRAY_STAINED_GLASS_PANE, "<gray>");
    }

    public static ItemStack accent() {
        return of(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>");
    }
}
