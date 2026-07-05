package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Weist Admins beim Beitreten auf ein verfuegbares Update hin (nur wenn
 * {@code notify-updates} aktiv ist und die Update-Permission vorliegt).
 */
public final class UpdateNotifyListener implements Listener {

    private final SmokeAndSalt plugin;

    public UpdateNotifyListener(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("notify-updates", true)) return;
        if (!event.getPlayer().hasPermission("smokeandsalt.admin.update")) return;
        // Kurz verzoegern, damit der Join sauber abgeschlossen ist.
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.updater().notifyIfOutdated(event.getPlayer()), 40L);
    }
}
