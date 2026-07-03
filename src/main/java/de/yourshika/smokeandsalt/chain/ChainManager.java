package de.yourshika.smokeandsalt.chain;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.item.ItemKeys;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * Verwaltet an Ketten aufgehaengte Items (Kessel-Behang, Raeucherware). Die
 * aufgehaengten Items sind schwebende, eingefrorene {@link Item}-Entities direkt
 * unter dem Ketten-Block.
 *
 * <p>Die Zuordnung wird im Speicher gehalten; beim Herunterfahren werden alle
 * aufgehaengten Items sauber zurueckgegeben, damit nichts verloren geht.</p>
 */
public final class ChainManager {

    private final SmokeAndSalt plugin;
    private final ItemKeys keys;
    private final Map<String, Item> hung = new HashMap<>();

    public ChainManager(SmokeAndSalt plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public boolean isHung(Block chain) {
        Item item = hung.get(key(chain));
        return item != null && item.isValid() && !item.isDead();
    }

    /** Haengt einen Stack unter die Kette. Gibt {@code true} bei Erfolg. */
    public boolean hang(Block chain, ItemStack stack) {
        if (isHung(chain)) return false;
        Location at = chain.getLocation().add(0.5, -0.35, 0.5);
        if (at.getWorld() == null) return false;
        Item item = at.getWorld().dropItem(at, stack.clone());
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setUnlimitedLifetime(true);
        item.setCanMobPickup(false);
        item.getPersistentDataContainer().set(keys.chainHung, PersistentDataType.BYTE, (byte) 1);
        hung.put(key(chain), item);
        return true;
    }

    /** Nimmt das aufgehaengte Item wieder ab und liefert dessen Stack (oder {@code null}). */
    public ItemStack retrieve(Block chain) {
        Item item = hung.remove(key(chain));
        if (item == null || !item.isValid()) return null;
        ItemStack stack = item.getItemStack();
        item.remove();
        return stack;
    }

    /** Ist diese Entity ein aufgehaengtes Item? */
    public boolean isHungEntity(Item item) {
        return item.getPersistentDataContainer().has(keys.chainHung, PersistentDataType.BYTE);
    }

    /** Gibt alle aufgehaengten Items zurueck (beim Herunterfahren). */
    public void releaseAll() {
        for (Item item : hung.values()) {
            if (item != null && item.isValid()) {
                Location loc = item.getLocation();
                ItemStack stack = item.getItemStack();
                item.remove();
                if (loc.getWorld() != null) {
                    loc.getWorld().dropItemNaturally(loc, stack);
                }
            }
        }
        hung.clear();
    }

    private String key(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
