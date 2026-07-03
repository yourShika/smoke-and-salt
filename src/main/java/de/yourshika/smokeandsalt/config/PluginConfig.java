package de.yourshika.smokeandsalt.config;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;

/**
 * Typisierter Zugriff auf die config.yml. Wird bei {@code /sas reload} neu
 * geladen. Enthaelt nur globale Schalter - konkrete Rezepte, Items und Seeds
 * kommen ueber die entsprechenden Registries hinzu.
 */
public final class PluginConfig {

    private final SmokeAndSalt plugin;

    private String language;
    private boolean debug;
    private boolean cookingEnabled;
    private boolean particlesEnabled;
    private boolean soundsEnabled;
    private boolean smokerEnabled;
    private boolean campfireEnabled;
    private boolean cauldronWaterEnabled;
    private boolean cauldronLavaEnabled;
    private boolean cuttingEnabled;
    private boolean seedsEnabled;
    private boolean chainEnabled;
    private List<String> worldWhitelist;
    private List<String> worldBlacklist;

    public PluginConfig(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();
        language = c.getString("language", "de");
        debug = c.getBoolean("debug", false);
        cookingEnabled = c.getBoolean("cooking.enabled", true);
        particlesEnabled = c.getBoolean("cooking.particles", true);
        soundsEnabled = c.getBoolean("cooking.sounds", true);
        smokerEnabled = c.getBoolean("cooking.stations.smoker", true);
        campfireEnabled = c.getBoolean("cooking.stations.campfire", true);
        cauldronWaterEnabled = c.getBoolean("cooking.stations.cauldron-water", true);
        cauldronLavaEnabled = c.getBoolean("cooking.stations.cauldron-lava", true);
        cuttingEnabled = c.getBoolean("cooking.stations.cutting", true);
        seedsEnabled = c.getBoolean("seeds.enabled", true);
        chainEnabled = c.getBoolean("chain.enabled", true);
        worldWhitelist = c.getStringList("worlds.whitelist");
        worldBlacklist = c.getStringList("worlds.blacklist");
    }

    /** Ist ein einzelnes externes Modul in der Config aktiviert? (Standard: ja) */
    public boolean isModuleEnabled(String id) {
        return plugin.getConfig().getBoolean("hooks.modules." + id, true);
    }

    public boolean isWorldAllowed(String world) {
        if (worldWhitelist != null && !worldWhitelist.isEmpty()) {
            return worldWhitelist.contains(world);
        }
        if (worldBlacklist != null && worldBlacklist.contains(world)) {
            return false;
        }
        return true;
    }

    public String language() { return language; }
    public boolean debug() { return debug; }
    public boolean cookingEnabled() { return cookingEnabled; }
    public boolean particlesEnabled() { return particlesEnabled; }
    public boolean soundsEnabled() { return soundsEnabled; }
    public boolean smokerEnabled() { return smokerEnabled; }
    public boolean campfireEnabled() { return campfireEnabled; }
    public boolean cauldronWaterEnabled() { return cauldronWaterEnabled; }
    public boolean cauldronLavaEnabled() { return cauldronLavaEnabled; }
    public boolean cuttingEnabled() { return cuttingEnabled; }
    public boolean seedsEnabled() { return seedsEnabled; }
    public boolean chainEnabled() { return chainEnabled; }

    /** Bequemer, tolerant getippter Language-Key in Kleinbuchstaben. */
    public String languageKey() {
        return language == null ? "de" : language.toLowerCase(Locale.ROOT);
    }
}
