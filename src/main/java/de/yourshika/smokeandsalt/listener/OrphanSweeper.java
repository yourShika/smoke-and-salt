package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Raeumt verwaiste, eingefrorene Item-Entities auf (Kessel-Float sowie Behaenge
 * der entfernten Ketten-Funktion), die durch einen fruehen Server-Crash (vor der
 * Nicht-Persistenz-Umstellung) im Boden liegen geblieben sind.
 *
 * <p>Neu erzeugte Float-Items sind nicht persistent und werden von ihrem Manager
 * selbst verwaltet; Ketten-Behaenge gibt es nicht mehr. Auf einem gerade geladenen
 * Chunk kann ein markiertes Item daher nur eine Karteileiche sein.</p>
 */
public final class OrphanSweeper implements Listener {

    private final SmokeAndSalt plugin;

    public OrphanSweeper(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    /** Beim Aktivieren alle bereits geladenen Chunks durchsehen. */
    public void sweepLoaded() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                sweep(chunk);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        sweep(event.getChunk());
    }

    private void sweep(Chunk chunk) {
        int removed = 0;
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Item item)) continue;
            var pdc = item.getPersistentDataContainer();
            if (pdc.has(plugin.keys().cookingFloat, PersistentDataType.BYTE)
                    || pdc.has(plugin.keys().chainHung, PersistentDataType.BYTE)) {
                item.remove();
                removed++;
            }
        }
        if (removed > 0) {
            plugin.debug("Orphan-Sweep: " + removed + " verwaiste Float-/Behang-Items entfernt.");
        }
    }
}
