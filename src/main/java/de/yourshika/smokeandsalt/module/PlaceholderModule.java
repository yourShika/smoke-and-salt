package de.yourshika.smokeandsalt.module;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.hook.PlaceholderHook;

/**
 * PlaceholderAPI-Anbindung. Registriert die {@code %sas_*%}-Platzhalter, sobald
 * PlaceholderAPI installiert und das Modul aktiviert ist.
 */
public final class PlaceholderModule extends AbstractModule {

    private PlaceholderHook hook;

    public PlaceholderModule(SmokeAndSalt plugin) {
        super(plugin, "placeholderapi", "PlaceholderAPI",
                "Platzhalter (%sas_*%) fuer andere Plugins", "PlaceholderAPI", false);
    }

    @Override
    protected void onEnable() throws Throwable {
        hook = new PlaceholderHook(plugin);
        hook.register();
    }

    @Override
    protected void onDisable() {
        if (hook != null) {
            try {
                hook.unregister();
            } catch (Throwable ignored) {
            }
            hook = null;
        }
    }
}
