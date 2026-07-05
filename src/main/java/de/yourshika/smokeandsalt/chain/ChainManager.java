package de.yourshika.smokeandsalt.chain;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.item.ItemKeys;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verwaltet an Ketten aufgehaengte Items (Kessel-Behang, Raeucherware).
 *
 * <p>Der Inhalt wird in {@code chains.yml} persistiert; die schwebenden
 * {@link Item}-Entities sind bewusst NICHT persistent und werden vom Tick-Task
 * pro geladenem Chunk selbst erzeugt/aktualisiert. Dadurch ueberstehen Behaenge
 * Neustart und Chunk-Reload und es entstehen keine Karteileichen.</p>
 */
public final class ChainManager {

    private static final int TICK_INTERVAL = 20;

    private final SmokeAndSalt plugin;
    private final ItemKeys keys;
    private final File file;
    private final Map<String, ItemStack> hung = new LinkedHashMap<>();
    private final Map<String, Item> entities = new HashMap<>();

    private BukkitTask task;

    public ChainManager(SmokeAndSalt plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
        this.file = new File(plugin.getDataFolder(), "chains.yml");
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
        for (Item entity : entities.values()) {
            if (entity.isValid()) entity.remove();
        }
        entities.clear();
        save();
    }

    public boolean isHung(Block chain) {
        return hung.containsKey(key(chain));
    }

    /** Haengt einen Stack unter die Kette (persistiert). Gibt {@code true} bei Erfolg. */
    public boolean hang(Block chain, ItemStack stack) {
        if (isHung(chain)) return false;
        hung.put(key(chain), stack.clone());
        save();
        spawn(chain);
        return true;
    }

    /** Nimmt das aufgehaengte Item wieder ab und liefert dessen Stack (oder {@code null}). */
    public ItemStack retrieve(Block chain) {
        ItemStack stack = hung.remove(key(chain));
        if (stack == null) return null;
        save();
        despawn(key(chain));
        return stack;
    }

    /** Ist diese Entity ein aufgehaengtes Item? */
    public boolean isHungEntity(Item item) {
        return item.getPersistentDataContainer().has(keys.chainHung, PersistentDataType.BYTE);
    }

    private void tick() {
        if (hung.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ItemStack> entry : hung.entrySet()) {
            Block chain = block(entry.getKey());
            if (chain == null) continue; // Chunk nicht geladen
            Item entity = entities.get(entry.getKey());
            if (entity == null || !entity.isValid() || entity.isDead()) {
                spawn(chain);
            } else {
                // Am Platz halten.
                Location at = chain.getLocation().add(0.5, -0.35, 0.5);
                if (entity.getLocation().distanceSquared(at) > 0.01) entity.teleport(at);
                entity.setVelocity(new Vector(0, 0, 0));
            }
        }
        // Verwaiste Entity-Referenzen aufraeumen.
        Iterator<Map.Entry<String, Item>> it = entities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Item> e = it.next();
            if (!hung.containsKey(e.getKey())) {
                if (e.getValue().isValid()) e.getValue().remove();
                it.remove();
            }
        }
    }

    private void spawn(Block chain) {
        String key = key(chain);
        ItemStack stack = hung.get(key);
        if (stack == null) return;
        despawn(key);
        Location at = chain.getLocation().add(0.5, -0.35, 0.5);
        if (at.getWorld() == null) return;
        Item item = at.getWorld().dropItem(at, stack.clone());
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setUnlimitedLifetime(true);
        item.setCanMobPickup(false);
        item.setPersistent(false);
        item.getPersistentDataContainer().set(keys.chainHung, PersistentDataType.BYTE, (byte) 1);
        entities.put(key, item);
    }

    private void despawn(String key) {
        Item entity = entities.remove(key);
        if (entity != null && entity.isValid()) entity.remove();
    }

    private Block block(String key) {
        String[] p = key.split(":");
        if (p.length < 4) return null;
        org.bukkit.World world = plugin.getServer().getWorld(p[0]);
        if (world == null) return null;
        int x = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);
        int z = Integer.parseInt(p[3]);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        return world.getBlockAt(x, y, z);
    }

    // --- Persistenz ---------------------------------------------------------

    private void load() {
        hung.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            ItemStack stack = yml.getItemStack(key);
            if (stack != null) hung.put(key, stack);
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, ItemStack> entry : hung.entrySet()) {
            yml.set(entry.getKey(), entry.getValue());
        }
        try {
            yml.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("chains.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private String key(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
