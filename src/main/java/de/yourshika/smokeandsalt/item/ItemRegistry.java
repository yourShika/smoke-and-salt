package de.yourshika.smokeandsalt.item;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry aller Custom-Items. Standardmaessig leer - Zutaten und Gerichte werden
 * spaeter ueber die config.yml ({@code items:}) oder {@link #register(ItemDefinition)}
 * hinzugefuegt. Die Registry baut fertige {@link ItemStack}s inklusive
 * PDC-Markierung und (falls das Oraxen-Modul aktiv ist) Custom-Textur.
 */
public final class ItemRegistry {

    private final SmokeAndSalt plugin;
    private final ItemKeys keys;
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();

    public ItemRegistry(SmokeAndSalt plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    /** Liest die Item-Definitionen aus der config.yml neu ein. */
    public void loadFromConfig() {
        definitions.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("items");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            try {
                Material material = Material.matchMaterial(
                        sec.getString("material", "PAPER").toUpperCase(Locale.ROOT));
                if (material == null) {
                    plugin.getLogger().warning("Item '" + id + "': unbekanntes Material - uebersprungen.");
                    continue;
                }
                ItemDefinition def = new ItemDefinition(
                        id.toLowerCase(Locale.ROOT),
                        material,
                        sec.getString("display-name", id),
                        sec.getStringList("lore"),
                        sec.getString("provider-id", null),
                        sec.getBoolean("glow", false));
                register(def);
            } catch (Exception ex) {
                plugin.getLogger().warning("Item '" + id + "' konnte nicht geladen werden: " + ex.getMessage());
            }
        }
        if (!definitions.isEmpty()) {
            plugin.getLogger().info("Custom-Items geladen: " + definitions.size());
        }
    }

    /** Registriert oder ueberschreibt eine Item-Definition. */
    public void register(ItemDefinition def) {
        definitions.put(def.id().toLowerCase(Locale.ROOT), def);
    }

    public ItemDefinition definition(String id) {
        return id == null ? null : definitions.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String id) {
        return id != null && definitions.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<ItemDefinition> all() {
        return definitions.values();
    }

    public List<String> ids() {
        return new ArrayList<>(definitions.keySet());
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    /** Baut einen fertigen ItemStack fuer die gegebene ID (oder {@code null}). */
    public ItemStack create(String id, int amount) {
        ItemDefinition def = definition(id);
        if (def == null) return null;
        ItemStack item = new ItemStack(def.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.line(def.displayName()));
            if (!def.lore().isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String s : def.lore()) lore.add(Text.line(s));
                meta.lore(lore);
            }
            meta.getPersistentDataContainer().set(keys.itemId, PersistentDataType.STRING, def.id());
            if (def.glow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        // Optionale Custom-Textur via aktivem Item-Modul (Oraxen).
        plugin.moduleManager().applyExternalModel(item, def.providerId());
        return item;
    }

    /** Liest die Custom-Item-ID aus einem Stack (oder {@code null}). */
    public String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(keys.itemId, PersistentDataType.STRING);
    }
}
