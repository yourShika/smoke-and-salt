package de.yourshika.smokeandsalt.seed;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plugin-getriebene Custom-Crops: gepflanzt wird auf Ackerland OHNE Weizenblock,
 * die Pflanze ist ein eigenes {@link ItemDisplay} (Custom-Textur pro Pflanze),
 * das das Plugin selbst wachsen laesst. Dadurch gibt es keine doppelte Textur.
 *
 * <p>Der Zustand liegt in {@code crops.yml}; die Displays sind bewusst
 * NICHT persistent und werden vom Tick-Task in geladenen Chunks selbst neu
 * erzeugt bzw. aktualisiert (self-healing ueber Neustart/Chunk-Reload).</p>
 */
public final class CropManager {

    private static final int MAX_STAGE = 7;
    private static final int TICK_INTERVAL = 40; // 2s

    private final SmokeAndSalt plugin;
    private final File file;
    private final Map<String, Crop> crops = new LinkedHashMap<>();
    private final Map<String, ItemDisplay> displays = new HashMap<>();

    private BukkitTask task;
    private boolean dirty;
    private int saveTick;

    public CropManager(SmokeAndSalt plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crops.yml");
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("seeds.custom-crops", false);
    }

    public void start() {
        load();
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (ItemDisplay display : displays.values()) {
            if (display.isValid()) display.remove();
        }
        displays.clear();
        save();
    }

    // --- API ----------------------------------------------------------------

    public boolean contains(Block block) {
        return crops.containsKey(key(block));
    }

    /** Pflanzt einen Custom-Crop in den (Luft-)Block ueber dem Ackerland. */
    public boolean plant(Block block, String seedId) {
        if (contains(block)) return false;
        crops.put(key(block), new Crop(block, seedId, 0, 0));
        dirty = true;
        spawnOrUpdate(block);
        return true;
    }

    /**
     * Interaktion (Rechtsklick) auf einen Custom-Crop. Reif -> Ernte, sonst nichts.
     * Gibt {@code true}, wenn hier ein Crop war (Event soll abgebrochen werden).
     */
    public boolean interact(Player player, Block block) {
        Crop crop = crops.get(key(block));
        if (crop == null) return false;
        if (crop.stage >= MAX_STAGE) {
            harvest(player, block, crop);
        }
        return true;
    }

    /** Bonemeal auf den Crop -> mehrere Stufen weiter. */
    public boolean bonemeal(Block block) {
        Crop crop = crops.get(key(block));
        if (crop == null) return false;
        crop.stage = Math.min(MAX_STAGE, crop.stage + 1 + (int) (Math.random() * 2));
        crop.growth = crop.stage * ticksPerStage();
        dirty = true;
        spawnOrUpdate(block);
        return true;
    }

    /** Entfernt einen Crop (z.B. beim Abbauen des Ackerlands) und gibt den Samen zurueck. */
    public void removeAndDropSeed(Block block) {
        Crop crop = crops.remove(key(block));
        if (crop == null) return;
        dirty = true;
        despawn(block);
        ItemStack seed = plugin.seeds().create(crop.seedId, 1);
        if (seed != null) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), seed);
        }
    }

    private void harvest(Player player, Block block, Crop crop) {
        SeedDefinition def = plugin.seeds().definition(crop.seedId);
        if (def == null) return;
        Location loc = block.getLocation().add(0.5, 0.3, 0.5);
        ItemStack produce = harvestResult(def);
        if (produce != null) block.getWorld().dropItemNaturally(loc, produce);
        int seedBack = seedReturn(def);
        if (seedBack > 0) {
            ItemStack seed = plugin.seeds().create(def.id(), seedBack);
            if (seed != null) block.getWorld().dropItemNaturally(loc, seed);
        }
        // Nach der Ernte auf Stufe 0 zuruecksetzen (nachwachsend).
        crop.stage = 0;
        crop.growth = 0;
        dirty = true;
        spawnOrUpdate(block);
        plugin.effects().finish(block.getLocation());
    }

    // --- Tick ---------------------------------------------------------------

    private void tick() {
        // Custom-Crop-Display aus -> keine Displays verwalten (Vanilla-Weizen wird genutzt).
        if (!enabled()) {
            if (!displays.isEmpty()) {
                for (ItemDisplay display : displays.values()) {
                    if (display.isValid()) display.remove();
                }
                displays.clear();
            }
            return;
        }
        if (crops.isEmpty()) return;
        int perStage = ticksPerStage();
        Iterator<Map.Entry<String, Crop>> it = crops.entrySet().iterator();
        while (it.hasNext()) {
            Crop crop = it.next().getValue();
            Block block = crop.block(plugin);
            if (block == null) continue; // Chunk nicht geladen -> ueberspringen
            // Muss auf Ackerland stehen und der Platz frei/eigener Crop sein.
            Block below = block.getRelative(0, -1, 0);
            if (below.getType() != Material.FARMLAND || !isFreeForCrop(block)) {
                it.remove();
                dirty = true;
                despawn(block);
                continue;
            }
            // Wachsen (nur bei genug Licht).
            if (crop.stage < MAX_STAGE && block.getLightLevel() >= 9) {
                crop.growth++;
                int newStage = Math.min(MAX_STAGE, crop.growth / perStage);
                if (newStage != crop.stage) {
                    crop.stage = newStage;
                    dirty = true;
                    spawnOrUpdate(block);
                    continue;
                }
            }
            // Display sicherstellen (Chunk-Reload/Neustart -> neu erzeugen).
            ItemDisplay display = displays.get(key(block));
            if (display == null || !display.isValid()) {
                spawnOrUpdate(block);
            }
        }
        // Speichern gedrosselt (nicht bei jedem Tick), um Disk-I/O zu sparen.
        if (dirty && (++saveTick % 15 == 0)) save();
    }

    private boolean isFreeForCrop(Block block) {
        Material type = block.getType();
        return type.isAir();
    }

    // --- Display ------------------------------------------------------------

    private void spawnOrUpdate(Block block) {
        Crop crop = crops.get(key(block));
        if (crop == null) return;
        despawn(block);
        SeedDefinition def = plugin.seeds().definition(crop.seedId);
        if (def == null) return;

        ItemStack model = cropModel(def);
        double yOffset = plugin.getConfig().getDouble("seeds.crop-y-offset", 0.15);
        Location loc = block.getLocation().add(0.5, yOffset, 0.5);
        ItemDisplay display = block.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(model);
            d.setBillboard(Display.Billboard.VERTICAL);
            d.setPersistent(false);
            d.setShadowRadius(0.0f);
            d.setShadowStrength(0.0f);
            d.setViewRange(1.2f);
            d.getPersistentDataContainer().set(plugin.keys().cropDisplay,
                    org.bukkit.persistence.PersistentDataType.STRING, key(block));
            applyTransform(d, crop.stage);
        });
        displays.put(key(block), display);
    }

    private void applyTransform(ItemDisplay display, int stage) {
        double base = plugin.getConfig().getDouble("seeds.crop-scale", 0.9);
        float s = (float) (base * (0.4 + 0.6 * (stage / (double) MAX_STAGE)));
        Transformation t = new Transformation(
                new Vector3f(0f, s * 0.5f, 0f),
                new AxisAngle4f(),
                new Vector3f(s, s, s),
                new AxisAngle4f());
        display.setTransformation(t);
    }

    private void despawn(Block block) {
        ItemDisplay display = displays.remove(key(block));
        if (display != null && display.isValid()) display.remove();
    }

    private ItemStack cropModel(SeedDefinition def) {
        ItemStack item = new ItemStack(Material.PAPER);
        String produce = def.resultItemId() != null ? def.resultItemId() : def.id();
        plugin.moduleManager().applyExternalModel(item, "sas_" + produce + "_crop");
        return item;
    }

    // --- Ernte-Helfer -------------------------------------------------------

    private ItemStack harvestResult(SeedDefinition def) {
        if (def.resultItemId() != null) {
            ItemStack item = plugin.items().create(def.resultItemId(), def.resultAmount());
            if (item != null) return item;
        }
        if (def.resultMaterial() != null) {
            return new ItemStack(def.resultMaterial(), def.resultAmount());
        }
        return plugin.seeds().create(def.id(), def.resultAmount());
    }

    private int seedReturn(SeedDefinition def) {
        if (def.seedReturnMax() <= 0) return 0;
        int min = def.seedReturnMin();
        int max = def.seedReturnMax();
        return min + (int) Math.floor(Math.random() * (max - min + 1));
    }

    private int ticksPerStage() {
        return Math.max(1, plugin.getConfig().getInt("seeds.growth-ticks-per-stage", 6));
    }

    // --- Persistenz ---------------------------------------------------------

    private void load() {
        crops.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String k : yml.getKeys(false)) {
            String value = yml.getString(k);
            if (value == null) continue;
            String[] parts = value.split(":");
            if (parts.length < 3) continue;
            try {
                crops.put(k, new Crop(k, parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, Crop> entry : crops.entrySet()) {
            Crop c = entry.getValue();
            yml.set(entry.getKey(), c.seedId + ":" + c.stage + ":" + c.growth);
        }
        try {
            yml.save(file);
            dirty = false;
        } catch (Exception ex) {
            plugin.getLogger().warning("crops.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private String key(Block block) {
        return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    private static final class Crop {
        private final String worldKey;
        private final String seedId;
        private int stage;
        private int growth;

        private Crop(Block block, String seedId, int stage, int growth) {
            this.worldKey = block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
            this.seedId = seedId;
            this.stage = stage;
            this.growth = growth;
        }

        private Crop(String key, String seedId, int stage, int growth) {
            this.worldKey = key;
            this.seedId = seedId;
            this.stage = stage;
            this.growth = growth;
        }

        /** Liefert den Block, falls dessen Chunk geladen ist, sonst {@code null}. */
        private Block block(SmokeAndSalt plugin) {
            String[] p = worldKey.split(";");
            if (p.length < 4) return null;
            org.bukkit.World world = plugin.getServer().getWorld(p[0]);
            if (world == null) return null;
            int x = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            int z = Integer.parseInt(p[3]);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
            return world.getBlockAt(x, y, z);
        }
    }
}
