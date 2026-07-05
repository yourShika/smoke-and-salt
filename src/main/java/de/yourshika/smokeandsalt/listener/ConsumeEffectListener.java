package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Kosmetische Zusatz-Effekte beim Essen bestimmter Custom-Items. Aktuell laesst
 * der Creeper-Keks eine harmlose Explosionswolke aufsteigen (nur Partikel/Sound,
 * kein Schaden).
 */
public final class ConsumeEffectListener implements Listener {

    private final SmokeAndSalt plugin;

    public ConsumeEffectListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!"creeper_keks".equalsIgnoreCase(plugin.items().idOf(event.getItem()))) return;
        Player player = event.getPlayer();
        Location loc = player.getLocation().add(0, 1.0, 0);
        World world = loc.getWorld();
        if (world == null) return;
        if (plugin.pluginConfig().particlesEnabled()) {
            world.spawnParticle(Particle.EXPLOSION, loc, 1);
            world.spawnParticle(Particle.SMOKE, loc, 18, 0.3, 0.3, 0.3, 0.02);
        }
        if (plugin.pluginConfig().soundsEnabled()) {
            world.playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 0.6f, 1.4f);
        }
    }
}
