package de.yourshika.smokeandsalt.seed;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Merkt sich, welche gepflanzten Crop-Bloecke zu welchem Custom-Seed gehoeren.
 * So kann bei der Ernte das richtige Custom-Ergebnis ausgegeben werden, ohne die
 * Vanilla-Crop-Bloecke selbst markieren zu muessen. Persistiert in
 * {@code crops.yml} im Plugin-Datenordner.
 */
public final class CropStore {

    private final SmokeAndSalt plugin;
    private final File file;
    private final Map<String, String> crops = new HashMap<>();
    private boolean dirty;

    public CropStore(SmokeAndSalt plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crops.yml");
    }

    public void load() {
        crops.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            crops.put(key, yml.getString(key));
        }
    }

    public void save() {
        if (!dirty) return;
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, String> entry : crops.entrySet()) {
            yml.set(entry.getKey(), entry.getValue());
        }
        try {
            yml.save(file);
            dirty = false;
        } catch (Exception ex) {
            plugin.getLogger().warning("crops.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    public void put(Block block, String seedId) {
        crops.put(key(block.getLocation()), seedId);
        dirty = true;
        save();
    }

    public String get(Block block) {
        return crops.get(key(block.getLocation()));
    }

    public String remove(Block block) {
        String removed = crops.remove(key(block.getLocation()));
        if (removed != null) {
            dirty = true;
            save();
        }
        return removed;
    }

    public boolean contains(Block block) {
        return crops.containsKey(key(block.getLocation()));
    }

    public int size() {
        return crops.size();
    }

    private String key(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }
}
