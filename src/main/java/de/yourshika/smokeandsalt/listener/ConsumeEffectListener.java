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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Zusatz-Effekte beim Essen/Trinken bestimmter Custom-Items:
 *
 * <ul>
 *   <li><b>Creeper-Keks</b>: harmlose Explosionswolke (nur Partikel/Sound).</li>
 *   <li><b>Wein</b>: gibt <i>Nausea</i> und stapelbares <i>Luck</i> - je mehr man
 *       trinkt, desto laenger haelt das Glueck (bis max. 10 Minuten).</li>
 * </ul>
 */
public final class ConsumeEffectListener implements Listener {

    /** Wein: Glueck-Zuwachs pro Schluck (2 min) und Deckel (10 min). */
    private static final int WINE_LUCK_STEP = 20 * 60 * 2;
    private static final int WINE_LUCK_MAX = 20 * 60 * 10;
    private static final int WINE_NAUSEA = 20 * 8;

    private final SmokeAndSalt plugin;

    public ConsumeEffectListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        String id = plugin.items().idOf(event.getItem());
        if (id == null) return;
        Player player = event.getPlayer();

        switch (id.toLowerCase(java.util.Locale.ROOT)) {
            case "creeper_keks" -> creeperPuff(player);
            case "wein" -> drinkWine(player);
            default -> {
            }
        }
    }

    private void creeperPuff(Player player) {
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

    /** Wein: Nausea + stapelndes Glueck (verlaengert bis 10 min). */
    private void drinkWine(Player player) {
        // Effekte erst nach dem Konsum anwenden (Event feuert davor).
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isValid() || player.isDead()) return;
            PotionEffect luck = player.getPotionEffect(PotionEffectType.LUCK);
            int base = luck != null ? luck.getDuration() : 0;
            int next = Math.min(WINE_LUCK_MAX, base + WINE_LUCK_STEP);
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, next, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, WINE_NAUSEA, 0, true, true, true));
            Location loc = player.getLocation();
            if (loc.getWorld() != null && plugin.pluginConfig().soundsEnabled()) {
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, 0.7f, 1.1f);
            }
        });
    }
}
