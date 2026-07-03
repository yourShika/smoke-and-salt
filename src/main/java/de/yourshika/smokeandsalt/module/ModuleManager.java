package de.yourshika.smokeandsalt.module;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Verwaltet alle externen, optionalen Module (Hooks).
 *
 * <p>Module aktivieren sich <strong>automatisch</strong>, sobald das benoetigte
 * Plugin installiert und das Modul in der Config aktiviert ist (Standard:
 * aktiviert). Fehlt das Plugin, bleibt das Modul still inaktiv - das Plugin
 * laeuft weiterhin vollstaendig eigenstaendig. Einzelne Module lassen sich ueber
 * {@code /sas modules} live ab-/anschalten.</p>
 */
public final class ModuleManager {

    private final SmokeAndSalt plugin;
    private final List<Module> modules = new ArrayList<>();
    private final List<ExternalItemModule> itemModules = new ArrayList<>();

    public ModuleManager(SmokeAndSalt plugin) {
        this.plugin = plugin;
        register(new PlaceholderModule(plugin));
        // Custom-Item-Anbindung: bewusst nur Oraxen (frei nutzbar, reife API).
        register(new OraxenModule(plugin));
    }

    private void register(Module module) {
        modules.add(module);
        if (module instanceof ExternalItemModule item) {
            itemModules.add(item);
        }
    }

    /** (De-)Aktiviert alle Module gemaess aktueller Konfiguration (automatisch). */
    public void reload() {
        for (Module module : modules) {
            module.disable();
        }
        int active = 0;
        for (Module module : modules) {
            if (!module.isEnabledInConfig()) continue;
            if (!module.isPluginPresent()) continue;
            try {
                module.enable();
                active++;
                plugin.getLogger().info("Hook aktiv: " + module.displayName());
            } catch (Throwable t) {
                plugin.getLogger().warning("Modul '" + module.displayName() + "' konnte nicht aktiviert werden: " + t.getMessage());
            }
        }
        plugin.getLogger().info("Aktive Hooks: " + active + "/" + modules.size());
    }

    public void shutdown() {
        for (Module module : modules) {
            module.disable();
        }
    }

    public List<Module> modules() {
        return Collections.unmodifiableList(modules);
    }

    public Module byId(String id) {
        for (Module module : modules) {
            if (module.id().equalsIgnoreCase(id)) return module;
        }
        return null;
    }

    /** Ist das Modul mit dieser ID aktuell aktiv? */
    public boolean isActive(String id) {
        Module m = byId(id);
        return m != null && m.isActive();
    }

    /** Aktuell aktiver externer Item-Anbieter (oder {@code null} = Vanilla). */
    public CustomItemProvider activeItemProvider() {
        for (ExternalItemModule module : itemModules) {
            if (module.isActive()) return module;
        }
        return null;
    }

    /** Ueberlagert ein Plugin-Item anhand einer externen Provider-ID (falls aktiv). */
    public void applyExternalModel(ItemStack item, String providerId) {
        if (providerId == null || providerId.isBlank() || item == null) return;
        CustomItemProvider provider = activeItemProvider();
        if (provider != null) {
            provider.apply(item, providerId);
        }
    }
}
