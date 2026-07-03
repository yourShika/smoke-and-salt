package de.yourshika.smokeandsalt.util;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

/**
 * Gebuendelte Partikel- und Sound-Funktionen fuer die Koch-Stationen. Alle
 * Aufrufe respektieren die globalen Schalter {@code cooking.particles} und
 * {@code cooking.sounds} aus der Config, sodass sie serverweit ab-/anschaltbar
 * sind.
 */
public final class Effects {

    private final SmokeAndSalt plugin;

    public Effects(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    private boolean particles() {
        return plugin.pluginConfig().particlesEnabled();
    }

    private boolean sounds() {
        return plugin.pluginConfig().soundsEnabled();
    }

    /** Aufsteigender Rauch/Dampf ueber einer Station (Smoker, Lagerfeuer). */
    public void smoke(Location center, int count) {
        if (!particles() || center.getWorld() == null) return;
        World w = center.getWorld();
        Location above = center.clone().add(0.5, 1.0, 0.5);
        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, above, count, 0.15, 0.05, 0.15, 0.01);
        w.spawnParticle(Particle.SMOKE, above, Math.max(1, count / 2), 0.2, 0.1, 0.2, 0.005);
    }

    /** Kochblasen und Dampf ueber kochendem Wasser im Kessel. */
    public void boil(Location cauldronCenter, int count) {
        if (!particles() || cauldronCenter.getWorld() == null) return;
        World w = cauldronCenter.getWorld();
        Location surface = cauldronCenter.clone().add(0.5, 0.9, 0.5);
        w.spawnParticle(Particle.BUBBLE_POP, surface, count, 0.18, 0.02, 0.18, 0.0);
        w.spawnParticle(Particle.SPLASH, surface, Math.max(1, count / 2), 0.18, 0.02, 0.18, 0.0);
        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, surface.clone().add(0, 0.3, 0),
                Math.max(1, count / 3), 0.1, 0.05, 0.1, 0.008);
    }

    /** Heisses Braten/Frittieren ueber Lava im Kessel. */
    public void fry(Location cauldronCenter, int count) {
        if (!particles() || cauldronCenter.getWorld() == null) return;
        World w = cauldronCenter.getWorld();
        Location surface = cauldronCenter.clone().add(0.5, 0.9, 0.5);
        w.spawnParticle(Particle.FLAME, surface, count, 0.16, 0.02, 0.16, 0.005);
        w.spawnParticle(Particle.LAVA, surface, Math.max(1, count / 3), 0.1, 0.02, 0.1, 0.0);
        w.spawnParticle(Particle.SMOKE, surface.clone().add(0, 0.25, 0),
                Math.max(1, count / 2), 0.12, 0.05, 0.12, 0.01);
    }

    /** Schnitt-Effekt beim Verarbeiten mit der Axt (Hand-Station). */
    public void cut(Location at) {
        if (center(at) == null) return;
        World w = at.getWorld();
        if (particles() && w != null) {
            w.spawnParticle(Particle.CRIT, at, 8, 0.2, 0.2, 0.2, 0.05);
            w.spawnParticle(Particle.SWEEP_ATTACK, at, 1, 0.0, 0.0, 0.0, 0.0);
            w.spawnParticle(Particle.ITEM_SLIME, at, 4, 0.15, 0.15, 0.15, 0.0);
        }
        if (sounds() && w != null) {
            w.playSound(at, Sound.BLOCK_WOOD_HIT, 0.7f, 1.3f);
            w.playSound(at, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.4f);
        }
    }

    /** Abschluss eines Koch-Vorgangs an einer Station. */
    public void finish(Location at) {
        World w = at.getWorld();
        if (w == null) return;
        if (particles()) {
            w.spawnParticle(Particle.DUST, at.clone().add(0.5, 1.0, 0.5), 12, 0.2, 0.2, 0.2, 0.0,
                    new Particle.DustOptions(Color.fromRGB(0xF2, 0xC8, 0x7A), 1.1f));
        }
        if (sounds()) {
            w.playSound(at, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.6f);
        }
    }

    /** Kurzer Zischlaut beim Einlegen in kochendes Wasser oder Lava. */
    public void sizzle(Location at, boolean lava) {
        World w = at.getWorld();
        if (w == null || !sounds()) return;
        if (lava) {
            w.playSound(at, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.4f);
        } else {
            w.playSound(at, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.7f);
        }
    }

    private Location center(Location at) {
        return at == null || at.getWorld() == null ? null : at;
    }
}
