package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Heat;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Laesst Wasserkessel mit Waermequelle darunter sichtbar "kochen" (Dampf und
 * Blasen), damit die Funktion auch ohne laufendes Rezept lebendig wirkt. Scannt
 * nur eine kleine Umgebung um Online-Spieler und ist ueber {@code cooking.particles}
 * abschaltbar.
 */
public final class BoilingAmbientTask extends BukkitRunnable {

    private static final int RADIUS = 5;
    private static final int MAX_PER_TICK = 32;

    private final SmokeAndSalt plugin;

    public BoilingAmbientTask(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        var config = plugin.pluginConfig();
        if (!config.cookingEnabled() || !config.particlesEnabled() || !config.cauldronWaterEnabled()) return;

        Set<Block> seen = new HashSet<>();
        int emitted = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!config.isWorldAllowed(player.getWorld().getName())) continue;
            Block origin = player.getLocation().getBlock();
            for (int dx = -RADIUS; dx <= RADIUS && emitted < MAX_PER_TICK; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS && emitted < MAX_PER_TICK; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        Block block = origin.getRelative(dx, dy, dz);
                        if (block.getType() != Material.WATER_CAULDRON) continue;
                        if (!seen.add(block)) continue;
                        if (!Heat.hasHeatSourceBelow(block)) continue;
                        plugin.effects().boil(block.getLocation(), 2);
                        emitted++;
                        break;
                    }
                }
            }
            if (emitted >= MAX_PER_TICK) break;
        }
    }
}
