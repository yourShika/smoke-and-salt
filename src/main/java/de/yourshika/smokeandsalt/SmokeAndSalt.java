package de.yourshika.smokeandsalt;

import de.yourshika.smokeandsalt.chain.ChainManager;
import de.yourshika.smokeandsalt.command.SmokeAndSaltCommand;
import de.yourshika.smokeandsalt.config.MessageManager;
import de.yourshika.smokeandsalt.config.PluginConfig;
import de.yourshika.smokeandsalt.content.ContentFileLoader;
import de.yourshika.smokeandsalt.cooking.CookingManager;
import de.yourshika.smokeandsalt.cooking.CookingRegistry;
import de.yourshika.smokeandsalt.item.ItemKeys;
import de.yourshika.smokeandsalt.item.ItemRegistry;
import de.yourshika.smokeandsalt.listener.BoilingAmbientTask;
import de.yourshika.smokeandsalt.listener.CauldronListener;
import de.yourshika.smokeandsalt.listener.ChainListener;
import de.yourshika.smokeandsalt.listener.CustomItemSafetyListener;
import de.yourshika.smokeandsalt.listener.CuttingListener;
import de.yourshika.smokeandsalt.listener.GuiListener;
import de.yourshika.smokeandsalt.listener.SeedListener;
import de.yourshika.smokeandsalt.module.ModuleManager;
import de.yourshika.smokeandsalt.seed.SeedManager;
import de.yourshika.smokeandsalt.update.GitHubUpdater;
import de.yourshika.smokeandsalt.util.Effects;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Hauptklasse von Smoke &amp; Salt. Initialisiert Konfiguration, Nachrichten,
 * Item-/Seed-/Rezept-Registries, das Koch-System (Stationen + Partikel), das
 * Ketten-System sowie das externe Modul-System (Oraxen, PlaceholderAPI).
 *
 * <p>Konkrete Rezepte, Zutaten und Gerichte werden bewusst nicht mitgeliefert -
 * die Funktionen greifen automatisch, sobald sie ueber die config.yml definiert
 * werden.</p>
 */
public final class SmokeAndSalt extends JavaPlugin {

    /** Aktuelle Struktur-Version der config.yml. */
    private static final int CONFIG_VERSION = 2;

    private PluginConfig pluginConfig;
    private MessageManager messages;
    private ItemKeys keys;
    private ItemRegistry items;
    private SeedManager seeds;
    private de.yourshika.smokeandsalt.seed.CropManager crops;
    private ModuleManager moduleManager;
    private CookingRegistry cookingRegistry;
    private CookingManager cooking;
    private de.yourshika.smokeandsalt.cooking.StationRecipeManager stationRecipes;
    private de.yourshika.smokeandsalt.cooking.CauldronManager cauldron;
    private de.yourshika.smokeandsalt.cooking.LavaCauldronManager lavaCauldron;
    private de.yourshika.smokeandsalt.crafting.CraftingManager crafting;
    private ChainManager chains;
    private Effects effects;
    private GitHubUpdater updater;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
        saveResourceIfMissing("messages_de.yml");
        saveResourceIfMissing("messages_en.yml");
        ContentFileLoader.saveDefaults(this);

        this.pluginConfig = new PluginConfig(this);
        this.pluginConfig.load();

        this.messages = new MessageManager(this);
        this.messages.load(pluginConfig.language());

        this.keys = new ItemKeys(this);
        this.effects = new Effects(this);
        this.items = new ItemRegistry(this, keys);
        this.seeds = new SeedManager(this, keys);
        this.crops = new de.yourshika.smokeandsalt.seed.CropManager(this);
        this.chains = new ChainManager(this, keys);
        this.cookingRegistry = new CookingRegistry(this, items);
        this.cooking = new CookingManager(this, cookingRegistry, effects, keys);
        this.stationRecipes = new de.yourshika.smokeandsalt.cooking.StationRecipeManager(this);
        this.cauldron = new de.yourshika.smokeandsalt.cooking.CauldronManager(this);
        this.lavaCauldron = new de.yourshika.smokeandsalt.cooking.LavaCauldronManager(this);
        this.crafting = new de.yourshika.smokeandsalt.crafting.CraftingManager(this);
        this.updater = new GitHubUpdater(this);

        // Modul-System zuerst - Item-Erstellung nutzt den aktiven Item-Provider.
        this.moduleManager = new ModuleManager(this);
        moduleManager.reload();

        // Registries laden (Config + mitgelieferter Standard-Inhalt).
        loadContent();

        // Listener registrieren.
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new CuttingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CauldronListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.smokeandsalt.listener.CauldronMenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.smokeandsalt.listener.ConsumeEffectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CustomItemSafetyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SeedListener(this, seeds), this);
        Bukkit.getPluginManager().registerEvents(new ChainListener(this, chains), this);
        Bukkit.getPluginManager().registerEvents(
                new de.yourshika.smokeandsalt.listener.UpdateNotifyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new de.yourshika.smokeandsalt.listener.LeafDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(crafting, this);

        // Befehl.
        SmokeAndSaltCommand command = new SmokeAndSaltCommand(this);
        PluginCommand pc = getCommand("smokeandsalt");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        // Koch-Tick + Ambient-Kochen.
        cooking.start();
        cauldron.start();
        lavaCauldron.start();
        crops.start();
        chains.start();
        // Verwaiste Float-/Behang-Items aus fruehen Crashes aufraeumen.
        de.yourshika.smokeandsalt.listener.OrphanSweeper sweeper =
                new de.yourshika.smokeandsalt.listener.OrphanSweeper(this);
        Bukkit.getPluginManager().registerEvents(sweeper, this);
        sweeper.sweepLoaded();
        new BoilingAmbientTask(this).runTaskTimer(this, 40L, 10L);

        getLogger().info("Smoke & Salt v" + getPluginMeta().getVersion()
                + " aktiviert (Paper 26.1.x / Java 25).");
    }

    @Override
    public void onDisable() {
        if (cooking != null) cooking.stop();
        if (crops != null) crops.stop();
        if (cauldron != null) cauldron.stop();
        if (lavaCauldron != null) lavaCauldron.stop();
        if (crafting != null) crafting.unregisterAll();
        if (stationRecipes != null) stationRecipes.unregisterAll();
        if (chains != null) chains.stop();
        if (seeds != null) seeds.cropStore().save();
        if (moduleManager != null) moduleManager.shutdown();
        getLogger().info("Smoke & Salt deaktiviert.");
    }

    /** Laedt Config, Nachrichten, Registries und Module neu (fuer /sas reload). */
    public void reloadAll() {
        reloadConfig();
        pluginConfig.load();
        messages.load(pluginConfig.language());
        moduleManager.reload();
        loadContent();
        refreshOnlineItems();
    }

    /**
     * Laedt Items, Seeds und Rezepte: zuerst aus der config.yml, dann der
     * mitgelieferte Standard-Inhalt ({@link de.yourshika.smokeandsalt.content.DefaultContent}).
     * Config-Eintraege mit gleicher ID haben Vorrang.
     */
    private void loadContent() {
        items.loadFromConfig();
        seeds.loadFromConfig();
        cookingRegistry.loadFromConfig();
        cauldron.clearRecipes();
        lavaCauldron.clearRecipes();
        crafting.unregisterAll();
        crafting.disableVanillaBreadRecipe();
        stationRecipes.unregisterAll();
        ContentFileLoader.load(this);
        de.yourshika.smokeandsalt.content.DefaultContent.register(this);
        ContentFileLoader.validateReferences(this);
        // Smoker-/Lagerfeuer-Rezepte als Vanilla-Rezepte registrieren (nach DefaultContent).
        stationRecipes.registerAll();
    }

    /**
     * Schaltet ein einzelnes Modul um (persistiert + Live-Reload) und gleicht die
     * sichtbaren Items an den neuen Hook-Status an.
     */
    public void setModuleEnabled(String id, boolean value) {
        getConfig().set("hooks.modules." + id, value);
        saveConfig();
        pluginConfig.load();
        moduleManager.reload();
        refreshOnlineItems();
    }

    /**
     * Gleicht die vom externen Item-Hook (Oraxen) abhaengigen Items an den
     * aktuellen Stand an - wird nach einem Oraxen-Reload aufgerufen.
     */
    public void resyncExternalAssets() {
        refreshOnlineItems();
        getLogger().info("Externe Assets neu synchronisiert (Oraxen-Reload).");
    }

    /** Baut alle Custom-Items/Seeds in Spieler-Inventaren neu auf (Texturen an/aus). */
    public void refreshOnlineItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayerItems(player);
        }
    }

    private void refreshPlayerItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            String itemId = items.idOf(item);
            if (itemId != null && items.contains(itemId)) {
                ItemStack fresh = items.create(itemId, item.getAmount());
                if (fresh != null) {
                    contents[i] = fresh;
                    changed = true;
                }
                continue;
            }
            String seedId = seeds.idOf(item);
            if (seedId != null && seeds.definition(seedId) != null) {
                ItemStack fresh = seeds.create(seedId, item.getAmount());
                if (fresh != null) {
                    contents[i] = fresh;
                    changed = true;
                }
            }
        }
        if (changed) {
            player.getInventory().setContents(contents);
            player.updateInventory();
        }
    }

    /**
     * Aktualisiert eine veraltete config.yml automatisch. Bei einer
     * Struktur-Aenderung wird die alte Datei gesichert und die neue Standard-
     * konfiguration eingespielt.
     */
    private void migrateConfig() {
        int version = getConfig().getInt("config-version", 1);
        if (version >= CONFIG_VERSION) return;

        File cfg = new File(getDataFolder(), "config.yml");
        String backupName = "config-backup-v" + version + "-" + System.currentTimeMillis() + ".yml";
        try {
            if (cfg.exists()) {
                java.nio.file.Files.copy(cfg.toPath(),
                        new File(getDataFolder(), backupName).toPath());
            }
            saveResource("config.yml", true);
            reloadConfig();
            getLogger().warning("config.yml war veraltet (v" + version + ") und wurde auf v"
                    + CONFIG_VERSION + " aktualisiert. Alte Datei gesichert als '" + backupName + "'.");
        } catch (Exception ex) {
            getLogger().severe("config.yml konnte nicht migriert werden: " + ex.getMessage());
        }
    }

    private void saveResourceIfMissing(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists() && getResource(name) != null) {
            saveResource(name, false);
        }
    }

    public void debug(String message) {
        if (pluginConfig != null && pluginConfig.debug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /** Pfad der eigenen Plugin-JAR (fuer den Self-Updater und Asset-Deployer). */
    public File pluginJarFile() {
        return getFile();
    }

    public PluginConfig pluginConfig() { return pluginConfig; }
    public MessageManager messages() { return messages; }
    public ItemKeys keys() { return keys; }
    public ItemRegistry items() { return items; }
    public SeedManager seeds() { return seeds; }
    public de.yourshika.smokeandsalt.seed.CropManager crops() { return crops; }
    public ModuleManager moduleManager() { return moduleManager; }
    public CookingManager cooking() { return cooking; }
    public de.yourshika.smokeandsalt.cooking.CauldronManager cauldron() { return cauldron; }
    public de.yourshika.smokeandsalt.cooking.LavaCauldronManager lavaCauldron() { return lavaCauldron; }
    public de.yourshika.smokeandsalt.crafting.CraftingManager crafting() { return crafting; }
    public ChainManager chains() { return chains; }
    public Effects effects() { return effects; }
    public GitHubUpdater updater() { return updater; }
}
