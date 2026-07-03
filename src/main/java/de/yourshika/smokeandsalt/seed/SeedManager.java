package de.yourshika.smokeandsalt.seed;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.item.ItemKeys;
import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
 * Registry und Fabrik fuer Custom-Seeds. Standardmaessig leer - Pflanzen werden
 * spaeter ueber die config.yml ({@code seeds:}) ergaenzt. Verwaltet zusaetzlich den
 * {@link CropStore} fuer die Ernte-Zuordnung.
 */
public final class SeedManager {

    private final SmokeAndSalt plugin;
    private final ItemKeys keys;
    private final CropStore cropStore;
    private final Map<String, SeedDefinition> definitions = new LinkedHashMap<>();

    public SeedManager(SmokeAndSalt plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        this.cropStore = new CropStore(plugin);
    }

    public CropStore cropStore() {
        return cropStore;
    }

    public void loadFromConfig() {
        definitions.clear();
        cropStore.load();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("seeds.definitions");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) continue;
                try {
                    register(parse(id.toLowerCase(Locale.ROOT), sec));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Seed '" + id + "' konnte nicht geladen werden: " + ex.getMessage());
                }
            }
        }
        if (!definitions.isEmpty()) {
            plugin.getLogger().info("Custom-Seeds geladen: " + definitions.size());
        }
    }

    private SeedDefinition parse(String id, ConfigurationSection sec) {
        Material material = Material.matchMaterial(
                sec.getString("material", "WHEAT_SEEDS").toUpperCase(Locale.ROOT));
        Material crop = sec.contains("crop-material")
                ? Material.matchMaterial(sec.getString("crop-material").toUpperCase(Locale.ROOT)) : null;
        Material resultMat = sec.contains("result-material")
                ? Material.matchMaterial(sec.getString("result-material").toUpperCase(Locale.ROOT)) : null;
        return new SeedDefinition(
                id,
                material,
                sec.getString("display-name", id),
                sec.getString("provider-id", null),
                crop,
                sec.getString("result-item", null),
                resultMat,
                sec.getInt("result-amount", 1),
                sec.getDouble("grass-chance", 0.0),
                sec.getDouble("composter-chance", 0.0));
    }

    public void register(SeedDefinition def) {
        definitions.put(def.id().toLowerCase(Locale.ROOT), def);
    }

    public SeedDefinition definition(String id) {
        return id == null ? null : definitions.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<SeedDefinition> all() {
        return definitions.values();
    }

    public List<String> ids() {
        return new ArrayList<>(definitions.keySet());
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    /** Baut das Seed-Item fuer die gegebene ID (oder {@code null}). */
    public ItemStack create(String id, int amount) {
        SeedDefinition def = definition(id);
        if (def == null) return null;
        ItemStack item = new ItemStack(def.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.line(def.displayName()));
            meta.lore(List.of(Text.line("<dark_gray>Smoke & Salt Saatgut")));
            meta.getPersistentDataContainer().set(keys.seedId, PersistentDataType.STRING, def.id());
            item.setItemMeta(meta);
        }
        plugin.moduleManager().applyExternalModel(item, def.providerId());
        return item;
    }

    /** Liest die Seed-ID aus einem Item (oder {@code null}). */
    public String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(keys.seedId, PersistentDataType.STRING);
    }
}
