package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.item.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Haelt alle {@link CookingRecipe}s. Standardmaessig leer - Rezepte werden spaeter
 * ueber die config.yml ({@code recipes:}) oder {@link #register(CookingRecipe)}
 * ergaenzt. Die Koch-Funktionen (Partikel, Timing, Schweben, Schneiden) sind
 * bereits vollstaendig vorhanden und greifen, sobald ein passendes Rezept
 * existiert.
 */
public final class CookingRegistry {

    private final SmokeAndSalt plugin;
    private final ItemRegistry items;
    private final Map<String, CookingRecipe> byId = new LinkedHashMap<>();
    private final Map<CookingStation, List<CookingRecipe>> byStation = new EnumMap<>(CookingStation.class);

    public CookingRegistry(SmokeAndSalt plugin, ItemRegistry items) {
        this.plugin = plugin;
        this.items = items;
        for (CookingStation station : CookingStation.values()) {
            byStation.put(station, new ArrayList<>());
        }
    }

    /** Liest die Rezepte aus der config.yml neu ein. */
    public void loadFromConfig() {
        byId.clear();
        byStation.values().forEach(List::clear);
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("recipes");
        int loaded = loadFromSection(root, CookingStation.SMOKER, false, "config.yml");
        if (loaded > 0) {
            plugin.getLogger().info("Koch-Rezepte geladen: " + byId.size());
        }
    }

    /** Liest einfache Ein-Zutaten-Rezepte aus einer beliebigen YAML-Section. */
    public int loadFromSection(ConfigurationSection root, CookingStation defaultStation,
                               boolean skipExisting, String source) {
        if (root == null) return 0;
        int loaded = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            if (skipExisting && contains(id)) continue;
            try {
                register(parse(id.toLowerCase(Locale.ROOT), sec, defaultStation));
                loaded++;
            } catch (Exception ex) {
                plugin.getLogger().warning(source + " Rezept '" + id
                        + "' konnte nicht geladen werden: " + ex.getMessage());
            }
        }
        return loaded;
    }

    private CookingRecipe parse(String id, ConfigurationSection sec, CookingStation defaultStation) {
        CookingStation station = CookingStation.valueOf(
                sec.getString("station", defaultStation.name()).toUpperCase(Locale.ROOT));
        CookingRecipe.Builder b = CookingRecipe.builder(id, station)
                .resultAmount(sec.getInt("result-amount", 1))
                .duration(sec.getInt("duration-ticks", 100));

        String inputItem = sec.getString("input-item", null);
        if (inputItem != null) {
            b.inputItem(inputItem.toLowerCase(Locale.ROOT));
        } else {
            b.inputMaterial(Material.matchMaterial(
                    sec.getString("input-material", "AIR").toUpperCase(Locale.ROOT)));
        }

        String resultItem = sec.getString("result-item", null);
        if (resultItem != null) {
            b.resultItem(resultItem.toLowerCase(Locale.ROOT));
        } else {
            b.resultMaterial(Material.matchMaterial(
                    sec.getString("result-material", "AIR").toUpperCase(Locale.ROOT)));
        }
        return b.build();
    }

    public void register(CookingRecipe recipe) {
        byId.put(recipe.id(), recipe);
        byStation.get(recipe.station()).add(recipe);
    }

    /** Findet ein passendes Rezept fuer eine Zutat an einer Station. */
    public Optional<CookingRecipe> find(CookingStation station, ItemStack input) {
        if (input == null) return Optional.empty();
        String customId = items.idOf(input);
        for (CookingRecipe recipe : byStation.get(station)) {
            if (recipe.inputIsCustom()) {
                if (customId != null && customId.equalsIgnoreCase(recipe.inputItemId())) {
                    return Optional.of(recipe);
                }
            } else if (customId == null && input.getType() == recipe.inputMaterial()) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    /** Baut den Ergebnis-ItemStack eines Rezepts. */
    public ItemStack buildResult(CookingRecipe recipe) {
        if (recipe.resultIsCustom()) {
            ItemStack custom = items.create(recipe.resultItemId(), recipe.resultAmount());
            if (custom != null) return custom;
            plugin.getLogger().warning("Rezept '" + recipe.id() + "': unbekanntes Ergebnis-Item '"
                    + recipe.resultItemId() + "'.");
            return null;
        }
        return new ItemStack(recipe.resultMaterial(), recipe.resultAmount());
    }

    public Collection<CookingRecipe> all() {
        return byId.values();
    }

    public List<CookingRecipe> forStation(CookingStation station) {
        return byStation.get(station);
    }

    public int size() {
        return byId.size();
    }

    public boolean contains(String id) {
        return id != null && byId.containsKey(id.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }
}
