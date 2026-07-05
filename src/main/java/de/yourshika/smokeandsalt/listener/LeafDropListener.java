package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Optionale Funktion: Drops beim Abbauen von Blaettern. Konfiguriert ueber die
 * config.yml unter {@code leaf-drops.drops}. Standardmaessig leer - konkrete
 * Drops (z.B. Custom-Seeds oder Fruechte) werden spaeter ergaenzt.
 *
 * <p>Schema pro Eintrag:</p>
 * <pre>
 * leaf-drops:
 *   enabled: true
 *   drops:
 *     apple_from_oak:
 *       item: &lt;item_id&gt;        # Custom-Item ODER ...
 *       material: APPLE          # ... Vanilla-Material
 *       chance: 0.05             # 0..1
 *       min: 1
 *       max: 1
 *       leaves: [OAK_LEAVES]     # optionaler Filter; leer = alle Blaetter
 * </pre>
 */
public final class LeafDropListener implements Listener {

    private final SmokeAndSalt plugin;

    public LeafDropListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("leaf-drops.enabled", true)) return;
        Block block = event.getBlock();
        if (!Tag.LEAVES.isTagged(block.getType())) return;
        // Nicht im Kreativ-Modus und nicht mit Silk Touch.
        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (event.getPlayer().getInventory().getItemInMainHand()
                .containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH)) return;

        ConfigurationSection drops = plugin.getConfig().getConfigurationSection("leaf-drops.drops");
        if (drops == null) return;
        if (!plugin.pluginConfig().isWorldAllowed(block.getWorld().getName())) return;

        for (String key : drops.getKeys(false)) {
            ConfigurationSection sec = drops.getConfigurationSection(key);
            if (sec == null) continue;
            if (!matchesLeaf(sec, block.getType())) continue;

            double chance = sec.getDouble("chance", 0.0);
            if (chance <= 0 || Math.random() >= chance) continue;

            int min = Math.max(1, sec.getInt("min", 1));
            int max = Math.max(min, sec.getInt("max", min));
            int amount = min + (int) (Math.random() * (max - min + 1));

            ItemStack drop = build(sec, amount);
            if (drop != null) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), drop);
            }
        }
    }

    private boolean matchesLeaf(ConfigurationSection sec, Material leaf) {
        var filter = sec.getStringList("leaves");
        if (filter.isEmpty()) return true;
        for (String name : filter) {
            if (name.equalsIgnoreCase(leaf.name())) return true;
        }
        return false;
    }

    private ItemStack build(ConfigurationSection sec, int amount) {
        String itemId = sec.getString("item", null);
        if (itemId != null) {
            ItemStack custom = plugin.items().create(itemId.toLowerCase(Locale.ROOT), amount);
            if (custom != null) return custom;
            ItemStack seed = plugin.seeds().create(itemId.toLowerCase(Locale.ROOT), amount);
            if (seed != null) return seed;
        }
        String mat = sec.getString("material", null);
        if (mat != null) {
            Material material = Material.matchMaterial(mat.toUpperCase(Locale.ROOT));
            if (material != null && material.isItem()) return new ItemStack(material, amount);
        }
        return null;
    }
}
