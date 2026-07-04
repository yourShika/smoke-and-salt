package de.yourshika.smokeandsalt.content;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Das Ergebnis eines Rezepts - entweder ein Custom-Item (ueber die {@code itemId})
 * oder ein Vanilla-{@link Material}.
 */
public record ResultSpec(String itemId, Material material, int amount) {

    public static ResultSpec item(String id, int amount) {
        return new ResultSpec(id, null, amount);
    }

    public static ResultSpec material(Material material, int amount) {
        return new ResultSpec(null, material, amount);
    }

    /** Baut den Ergebnis-Stack (oder {@code null}, falls ein Custom-Item fehlt). */
    public ItemStack build(SmokeAndSalt plugin) {
        if (itemId != null) {
            return plugin.items().create(itemId, amount);
        }
        return new ItemStack(material, amount);
    }

    public String display(SmokeAndSalt plugin) {
        if (itemId != null) {
            var def = plugin.items().definition(itemId);
            return def != null ? def.displayName() : itemId;
        }
        return material.name();
    }
}
