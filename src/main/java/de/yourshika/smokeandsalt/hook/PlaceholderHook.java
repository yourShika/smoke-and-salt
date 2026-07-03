package de.yourshika.smokeandsalt.hook;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI-Expansion fuer Smoke &amp; Salt. Wird nur geladen, wenn das
 * PlaceholderAPI-Modul aktiv ist.
 *
 * <ul>
 *   <li>{@code %sas_version%} - Plugin-Version</li>
 *   <li>{@code %sas_items%} - Anzahl registrierter Custom-Items</li>
 *   <li>{@code %sas_recipes%} - Anzahl registrierter Koch-Rezepte</li>
 *   <li>{@code %sas_active_cooks%} - aktuell laufende Koch-Vorgaenge</li>
 * </ul>
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final SmokeAndSalt plugin;

    public PlaceholderHook(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sas";
    }

    @Override
    public @NotNull String getAuthor() {
        return "yourShika";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "version" -> plugin.getPluginMeta().getVersion();
            case "items" -> String.valueOf(plugin.items().all().size());
            case "recipes" -> String.valueOf(plugin.cooking().registry().size());
            case "active_cooks" -> String.valueOf(plugin.cooking().activeCount());
            default -> null;
        };
    }
}
