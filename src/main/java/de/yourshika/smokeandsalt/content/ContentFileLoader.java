package de.yourshika.smokeandsalt.content;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CauldronRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.crafting.CraftingRecipe;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Laedt optionale, nach Kochfunktion sortierte Content-YAMLs aus
 * {@code plugins/SmokeAndSalt/content/}. Die eingebauten Defaults bleiben als
 * Fallback im Code, aber Server koennen neue Items und Rezepte dort sauber
 * getrennt ergaenzen.
 */
public final class ContentFileLoader {

    private static final Map<String, CookingStation> SIMPLE_RECIPE_FILES = Map.of(
            "smoker.yml", CookingStation.SMOKER,
            "campfire.yml", CookingStation.CAMPFIRE,
            "cutting.yml", CookingStation.CUTTING
    );

    private static final List<String> ALL_FILES = List.of(
            "items.yml",
            "seeds.yml",
            "smoker.yml",
            "campfire.yml",
            "water_cauldron.yml",
            "lava_cauldron.yml",
            "cutting.yml",
            "crafting.yml"
    );

    private ContentFileLoader() {
    }

    /** Bei Bump wird der content-Ordner gesichert und aus dem JAR neu erzeugt,
     *  damit Fixes/neue Inhalte auf bestehenden Servern automatisch ankommen. */
    private static final int CONTENT_VERSION = 5;

    public static void saveDefaults(SmokeAndSalt plugin) {
        File dir = new File(plugin.getDataFolder(), "content");
        File marker = new File(dir, ".content-version");
        int current = readContentVersion(marker);

        if (current < CONTENT_VERSION && dir.exists() && current > 0) {
            // Bestehende Dateien sichern und mit den neuen Defaults ersetzen.
            String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
            File backup = new File(plugin.getDataFolder(), "content-backup-" + stamp);
            try {
                for (String file : ALL_FILES) {
                    File src = new File(dir, file);
                    if (src.exists()) {
                        backup.mkdirs();
                        java.nio.file.Files.copy(src.toPath(), new File(backup, file).toPath());
                        src.delete();
                    }
                }
                plugin.getLogger().warning("content/ war veraltet (v" + current + ") - aktualisiert auf v"
                        + CONTENT_VERSION + ". Alte Dateien gesichert unter '" + backup.getName() + "'.");
            } catch (Exception ex) {
                plugin.getLogger().warning("content/ konnte nicht gesichert werden: " + ex.getMessage());
            }
        }

        for (String file : ALL_FILES) {
            saveResourceIfMissing(plugin, "content/" + file);
        }
        writeContentVersion(marker);
    }

    private static int readContentVersion(File marker) {
        if (!marker.exists()) return 1; // vorhandener Ordner ohne Marker = v1
        try {
            return Integer.parseInt(java.nio.file.Files.readString(marker.toPath()).trim());
        } catch (Exception ex) {
            return 1;
        }
    }

    private static void writeContentVersion(File marker) {
        try {
            marker.getParentFile().mkdirs();
            java.nio.file.Files.writeString(marker.toPath(), String.valueOf(CONTENT_VERSION));
        } catch (Exception ignored) {
        }
    }

    public static void load(SmokeAndSalt plugin) {
        File dir = new File(plugin.getDataFolder(), "content");
        for (String file : ALL_FILES) {
            File target = new File(dir, file);
            if (!target.exists()) continue;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(target);
            String source = "content/" + file;

            loadItems(plugin, yaml, source);
            loadSeeds(plugin, yaml, source);

            ConfigurationSection simple = yaml.getConfigurationSection("recipes");
            CookingStation defaultStation = SIMPLE_RECIPE_FILES.get(file);
            if (simple != null && defaultStation != null) {
                plugin.cooking().registry().loadFromSection(simple, defaultStation, true, source);
            }

            de.yourshika.smokeandsalt.cooking.CauldronStation cauldronTarget =
                    file.equals("lava_cauldron.yml") ? plugin.lavaCauldron() : plugin.cauldron();
            loadCauldron(plugin, yaml.getConfigurationSection("cauldron-recipes"), source, cauldronTarget);
            loadCrafting(plugin, yaml.getConfigurationSection("crafting-recipes"), source);
        }
    }

    private static void loadItems(SmokeAndSalt plugin, YamlConfiguration yaml, String source) {
        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items != null) {
            plugin.items().loadFromSection(items, true, source);
        }
    }

    private static void loadSeeds(SmokeAndSalt plugin, YamlConfiguration yaml, String source) {
        ConfigurationSection seeds = yaml.getConfigurationSection("seeds");
        if (seeds == null) return;
        ConfigurationSection definitions = seeds.getConfigurationSection("definitions");
        plugin.seeds().loadFromSection(definitions != null ? definitions : seeds, true, source);
    }

    private static void loadCauldron(SmokeAndSalt plugin, ConfigurationSection root, String source,
                                     de.yourshika.smokeandsalt.cooking.CauldronStation target) {
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            if (target.contains(id)) continue;
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            try {
                List<Ingredient> ingredients = parseIngredients(plugin, sec, source, id);
                ResultSpec result = parseResult(sec);
                target.register(new CauldronRecipe(
                        id.toLowerCase(Locale.ROOT),
                        ingredients,
                        result,
                        sec.getInt("duration-ticks", 100),
                        sec.getInt("water-cost", 0),
                        sec.getBoolean("serve-with-bowl", false)));
            } catch (Exception ex) {
                plugin.getLogger().warning(source + " Kessel-Rezept '" + id
                        + "' konnte nicht geladen werden: " + ex.getMessage());
            }
        }
    }

    private static void loadCrafting(SmokeAndSalt plugin, ConfigurationSection root, String source) {
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            if (plugin.crafting().contains(id)) continue;
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            try {
                plugin.crafting().register(new CraftingRecipe(
                        id.toLowerCase(Locale.ROOT),
                        parseIngredients(plugin, sec, source, id),
                        parseResult(sec)));
            } catch (Exception ex) {
                plugin.getLogger().warning(source + " Crafting-Rezept '" + id
                        + "' konnte nicht geladen werden: " + ex.getMessage());
            }
        }
    }

    private static List<Ingredient> parseIngredients(SmokeAndSalt plugin, ConfigurationSection sec,
                                                     String source, String id) {
        List<Ingredient> out = new ArrayList<>();
        for (Map<?, ?> raw : sec.getMapList("ingredients")) {
            Ingredient ingredient = parseIngredient(raw);
            if (ingredient != null) out.add(ingredient);
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException("ingredients fehlt oder ist leer");
        }
        return out;
    }

    private static Ingredient parseIngredient(Map<?, ?> raw) {
        String display = string(raw, "display", null);
        String item = string(raw, "item", null);
        if (item != null) return Ingredient.item(item.toLowerCase(Locale.ROOT), fallback(display, item));

        String seed = string(raw, "seed", null);
        if (seed != null) return Ingredient.seed(seed.toLowerCase(Locale.ROOT), fallback(display, seed));

        String materialName = string(raw, "material", null);
        if (materialName != null) {
            Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (material == null) {
                throw new IllegalArgumentException("unbekanntes Material: " + materialName);
            }
            return Ingredient.material(material, fallback(display, materialName));
        }
        throw new IllegalArgumentException("Zutat braucht item, seed oder material");
    }

    private static ResultSpec parseResult(ConfigurationSection sec) {
        ConfigurationSection result = sec.getConfigurationSection("result");
        if (result != null) {
            return parseResult(result, result.getInt("amount", 1));
        }
        return parseResult(sec, sec.getInt("result-amount", 1));
    }

    private static ResultSpec parseResult(ConfigurationSection sec, int amount) {
        String item = sec.getString("item", sec.getString("result-item", null));
        if (item != null) return ResultSpec.item(item.toLowerCase(Locale.ROOT), amount);
        String materialName = sec.getString("material", sec.getString("result-material", null));
        if (materialName != null) {
            Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (material == null) {
                throw new IllegalArgumentException("unbekanntes Ergebnis-Material: " + materialName);
            }
            return ResultSpec.material(material, amount);
        }
        throw new IllegalArgumentException("Ergebnis braucht result.item/result-item oder result.material/result-material");
    }

    private static void saveResourceIfMissing(SmokeAndSalt plugin, String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (!target.exists() && plugin.getResource(path) != null) {
            plugin.saveResource(path, false);
        }
    }

    private static String string(Map<?, ?> raw, String key, String fallback) {
        Object value = raw.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
