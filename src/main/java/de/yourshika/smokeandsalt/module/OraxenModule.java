package de.yourshika.smokeandsalt.module;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.inventory.ItemStack;

/**
 * Oraxen-Anbindung. Liefert optional Custom-Modelle/Texturen fuer Smoke &amp; Salt
 * Items ueber deren {@code provider-id}. Einziger unterstuetzter Custom-Item-Hook
 * (frei nutzbar, reife API).
 */
public final class OraxenModule extends ExternalItemModule {

    /** Dynamisch registrierter Listener fuer Oraxens Reload-Event (per Reflection). */
    private org.bukkit.event.Listener reloadListener;

    public OraxenModule(SmokeAndSalt plugin) {
        super(plugin, "oraxen", "Oraxen",
                "Custom-Modelle/Texturen fuer alle Smoke & Salt Items", "Oraxen", false);
    }

    @Override
    protected void onEnable() throws Throwable {
        verifyApi();
        new OraxenAssetDeployer(plugin).deploy();
        registerReloadListener();
    }

    @Override
    protected void onDisable() {
        if (reloadListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(reloadListener);
            reloadListener = null;
        }
    }

    /**
     * Registriert - nur wenn Oraxen vorhanden ist - einen Listener auf Oraxens
     * {@code OraxenItemsLoadedEvent}. Nach einem {@code /oraxen reload} werden die
     * vom Hook abhaengigen Items/Modelle automatisch neu synchronisiert. Bewusst
     * per Reflection, damit das Plugin ohne Oraxen kompiliert und laeuft.
     */
    @SuppressWarnings("unchecked")
    private void registerReloadListener() {
        try {
            Class<? extends org.bukkit.event.Event> eventClass =
                    (Class<? extends org.bukkit.event.Event>) Class.forName(
                            "io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent");
            reloadListener = new org.bukkit.event.Listener() {
            };
            org.bukkit.plugin.EventExecutor executor = (listener, event) ->
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, plugin::resyncExternalAssets);
            org.bukkit.Bukkit.getPluginManager().registerEvent(
                    eventClass, reloadListener, org.bukkit.event.EventPriority.MONITOR,
                    executor, plugin);
            plugin.debug("Oraxen-Reload-Listener aktiv (Auto-Resync nach /oraxen reload).");
        } catch (Throwable t) {
            plugin.debug("Oraxen-Reload-Listener nicht verfuegbar: " + t.getMessage());
        }
    }

    @Override
    protected void verifyApi() throws Throwable {
        Class.forName("io.th0rgal.oraxen.api.OraxenItems");
    }

    @Override
    protected ItemStack fetchById(String id) throws Throwable {
        Class<?> oraxenItems = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
        Object builder = oraxenItems.getMethod("getItemById", String.class).invoke(null, id);
        if (builder == null) return null;
        Object stack = builder.getClass().getMethod("build").invoke(builder);
        return (ItemStack) stack;
    }
}
