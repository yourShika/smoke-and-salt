package de.yourshika.smokeandsalt.module;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

/**
 * Basis fuer externe Custom-Item-Module (aktuell nur Oraxen). Liefert optional
 * Modell/Texturen und ueberlagert die eingebaute Vanilla-Optik.
 *
 * <p>Die Anbindung erfolgt bewusst per Reflection, damit das Plugin ohne Oraxen
 * auf dem Klassenpfad kompiliert und startet. Schlaegt etwas fehl, bleibt das
 * Item unveraendert (Vanilla-Optik) - nichts bricht.</p>
 */
public abstract class ExternalItemModule extends AbstractModule implements CustomItemProvider {

    protected ExternalItemModule(SmokeAndSalt plugin, String id, String displayName,
                                 String description, String requiredPlugin, boolean required) {
        super(plugin, id, displayName, description, requiredPlugin, required);
    }

    @Override
    protected void onEnable() throws Throwable {
        verifyApi();
    }

    /** Wirft, wenn die erwartete API-Klasse fehlt. */
    protected abstract void verifyApi() throws Throwable;

    /** Holt ein Custom-Item per ID aus dem externen Plugin (oder {@code null}). */
    protected abstract ItemStack fetchById(String id) throws Throwable;

    @Override
    public void apply(ItemStack item, String providerId) {
        if (!isActive()) return;
        if (providerId == null || providerId.isBlank()) return;
        try {
            ItemStack ext = fetchById(providerId);
            if (ext == null) return;
            ItemMeta extMeta = ext.getItemMeta();
            ItemMeta ourMeta = item.getItemMeta();
            if (extMeta == null || ourMeta == null) return;

            // Nur die modell-bestimmenden Bestandteile uebernehmen - Identitaet
            // (PDC), Name und Lore bleiben unsere.
            if (extMeta.hasItemModel()) {
                ourMeta.setItemModel(extMeta.getItemModel());
            }
            CustomModelDataComponent extCmd = extMeta.getCustomModelDataComponent();
            if (!extCmd.getFloats().isEmpty() || !extCmd.getStrings().isEmpty()) {
                ourMeta.setCustomModelDataComponent(extCmd);
            }
            item.setItemMeta(ourMeta);
        } catch (Throwable t) {
            plugin.debug("Externes Modell ('" + id() + "', id=" + providerId + ") nicht anwendbar: " + t.getMessage());
        }
    }
}
