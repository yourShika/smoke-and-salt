package de.yourshika.smokeandsalt.module;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Bukkit;

/**
 * Gemeinsame Basis fuer {@link Module}-Implementierungen. Kuemmert sich um
 * Plugin-Erkennung, Config-Flag und Aktiv-Status. Die konkrete Aktivierungs-
 * logik liefern Unterklassen ueber {@link #onEnable()} / {@link #onDisable()}.
 */
public abstract class AbstractModule implements Module {

    protected final SmokeAndSalt plugin;
    private final String id;
    private final String displayName;
    private final String description;
    private final String requiredPlugin;
    private final boolean required;

    private boolean active;

    protected AbstractModule(SmokeAndSalt plugin, String id, String displayName,
                             String description, String requiredPlugin, boolean required) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.requiredPlugin = requiredPlugin;
        this.required = required;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String description() { return description; }
    @Override public String requiredPlugin() { return requiredPlugin; }
    @Override public boolean required() { return required; }

    @Override
    public boolean isPluginPresent() {
        return requiredPlugin != null && Bukkit.getPluginManager().getPlugin(requiredPlugin) != null;
    }

    @Override
    public boolean isEnabledInConfig() {
        return plugin.pluginConfig().isModuleEnabled(id);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public final void enable() throws Throwable {
        if (active) return;
        onEnable();
        active = true;
    }

    @Override
    public final void disable() {
        if (!active) return;
        try {
            onDisable();
        } catch (Throwable t) {
            plugin.getLogger().warning("Modul '" + id + "' konnte nicht sauber deaktiviert werden: " + t.getMessage());
        } finally {
            active = false;
        }
    }

    /** Tatsaechliche Aktivierung - darf werfen. */
    protected abstract void onEnable() throws Throwable;

    /** Tatsaechliche Deaktivierung. */
    protected void onDisable() {
    }
}
