package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.content.Ingredient;
import de.yourshika.smokeandsalt.gui.CauldronMenuHolder;
import de.yourshika.smokeandsalt.gui.Icons;
import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistenter Kessel-Container mit kleiner GUI (Schmelzofen-Prinzip). Jeder
 * bespielte Kessel besitzt Eingabe-Slots, Ausgabe-Slots und einen Fortschritt.
 * Zutaten koennen entweder per Rechtsklick-GUI eingelegt oder wie gehabt in den
 * Kessel geworfen werden - beides landet im selben, persistenten Container und
 * wird Charge fuer Charge abgearbeitet.
 *
 * <p>Wasser- und Lavakessel teilen sich diese Logik; die stationsspezifischen
 * Teile (Blocktyp, Waermepruefung, Wasserverbrauch, Partikel) liefern die
 * Unterklassen.</p>
 */
public abstract class CauldronStation {

    /** Der Verarbeitungs-Tick laeuft alle 4 Server-Ticks. */
    protected static final int TICK_INTERVAL = 4;

    /** GUI-Groesse und Slot-Belegung (5 Reihen). */
    public static final int GUI_SIZE = 45;
    public static final int[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int[] OUTPUT_SLOTS = {15, 16, 24, 25, 33, 34};
    public static final int PROGRESS_SLOT = 23;
    public static final int INFO_SLOT = 4;

    protected final SmokeAndSalt plugin;
    private final File file;
    private final List<CauldronRecipe> recipes = new ArrayList<>();
    private final Map<String, Station> stations = new LinkedHashMap<>();

    private BukkitTask task;

    protected CauldronStation(SmokeAndSalt plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    // --- Stationsspezifisch --------------------------------------------------

    /** Der Blocktyp, der diese Station traegt (Wasser-/Lavakessel). */
    protected abstract Material blockMaterial();

    /** Kann der Kessel gerade kochen (Wasser: Wasserstand + Waerme; Lava: immer)? */
    protected abstract boolean canCook(Block block);

    /** Partikel waehrend des Kochens. */
    protected abstract void cookParticles(Location center, int count);

    /** Beim Einlegen (Zisch-Effekt). */
    protected abstract void sizzle(Location center);

    /** Wird nach einer fertigen Charge aufgerufen (z.B. Wasserstand senken). */
    protected abstract void afterBatch(Block block, CauldronRecipe recipe);

    /** GUI-Titel inkl. evtl. Wasserstand. */
    protected abstract Component menuTitle(Block block);

    /** Info-Icon (Wasserstand / Temperatur) fuer den GUI-Kopf. */
    protected abstract ItemStack infoIcon(Block block);

    // --- Rezept-Registry -----------------------------------------------------

    public void register(CauldronRecipe recipe) {
        recipes.add(recipe);
    }

    public void clearRecipes() {
        recipes.clear();
    }

    public List<CauldronRecipe> recipes() {
        return recipes;
    }

    public int size() {
        return recipes.size();
    }

    public boolean contains(String id) {
        if (id == null) return false;
        return recipes.stream().anyMatch(recipe -> recipe.id().equalsIgnoreCase(id));
    }

    // --- Lebenszyklus --------------------------------------------------------

    public void start() {
        load();
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        // Offene GUIs synchronisieren und schliessen.
        for (Station station : stations.values()) {
            for (HumanEntity viewer : new ArrayList<>(viewers(station))) {
                viewer.closeInventory();
            }
        }
        save();
    }

    // --- Eingabe (per Wurf oder GUI) ----------------------------------------

    /**
     * Nimmt einen kompletten Stack in den Container auf (soweit Platz ist) und
     * liefert den nicht aufgenommenen Rest zurueck ({@code null} wenn alles
     * aufgenommen wurde). Wird vom Wurf-Listener genutzt.
     */
    public ItemStack deposit(Block block, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        Station station = station(block, true);
        ItemStack leftover = addToInput(station, stack);
        syncOpenInventory(station);
        save();
        if (leftover == null || leftover.getAmount() < stack.getAmount()) {
            sizzle(block.getLocation());
        }
        return leftover;
    }

    /** Passt der Stack (teilweise) in irgendein hier gefuehrtes Rezept? */
    public boolean acceptsIngredient(ItemStack stack) {
        if (stack == null) return false;
        for (CauldronRecipe recipe : recipes) {
            for (Ingredient ingredient : recipe.ingredients()) {
                if (ingredient.matches(plugin, stack)) return true;
            }
        }
        return false;
    }

    // --- GUI oeffnen ---------------------------------------------------------

    public void open(Player player, Block block) {
        Station station = station(block, true);
        Inventory inv = Bukkit.createInventory(new CauldronMenuHolder(this, key(block)),
                GUI_SIZE, menuTitle(block));
        renderStatic(inv);
        writeContainer(station, inv);
        renderStatus(station, inv);
        player.openInventory(inv);
    }

    /** Beim Schliessen: Inhalt aus der offenen GUI zurueck in den Container lesen. */
    public void handleClose(String stationKey, Inventory inv) {
        Station station = stations.get(stationKey);
        if (station == null) return;
        readContainer(station, inv);
        pruneEmpty(station);
        save();
    }

    /** Nach einem Klick in der GUI: Container aus der GUI aktualisieren. */
    public void handleChange(String stationKey, Inventory inv) {
        Station station = stations.get(stationKey);
        if (station == null) return;
        readContainer(station, inv);
        save();
    }

    /** Gibt den kompletten Inhalt aus und entfernt die Station (Kessel abgebaut). */
    public void dropAndClear(Block block) {
        Station station = stations.get(key(block));
        if (station == null) return;
        Inventory open = openInventory(station);
        if (open != null) readContainer(station, open);
        closeViewers(station);
        dropAll(station);
        stations.remove(key(block));
        save();
    }

    /**
     * Legt einen (Shift-geklickten) Stack direkt in die Eingabe-Slots der offenen
     * GUI und liefert den nicht untergebrachten Rest zurueck. Der eigentliche
     * Container wird ueber {@link #handleChange} aus der GUI nachgezogen.
     */
    public ItemStack shiftIntoInput(Inventory inv, ItemStack stack) {
        ItemStack rest = stack.clone();
        for (int slot : INPUT_SLOTS) {
            ItemStack existing = inv.getItem(slot);
            if (existing != null && existing.isSimilar(rest)) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) continue;
                int move = Math.min(space, rest.getAmount());
                existing.setAmount(existing.getAmount() + move);
                inv.setItem(slot, existing);
                rest.setAmount(rest.getAmount() - move);
                if (rest.getAmount() <= 0) return null;
            }
        }
        for (int slot : INPUT_SLOTS) {
            ItemStack existing = inv.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                inv.setItem(slot, rest.clone());
                return null;
            }
        }
        return rest;
    }

    public boolean isInputSlot(int slot) {
        for (int s : INPUT_SLOTS) if (s == slot) return true;
        return false;
    }

    public boolean isOutputSlot(int slot) {
        for (int s : OUTPUT_SLOTS) if (s == slot) return true;
        return false;
    }

    // --- Tick / Verarbeitung -------------------------------------------------

    private void tick() {
        if (stations.isEmpty()) return;
        Iterator<Map.Entry<String, Station>> it = stations.entrySet().iterator();
        while (it.hasNext()) {
            Station station = it.next().getValue();
            Block block = station.block;

            // Entladene Chunks ueberspringen (Zustand bleibt erhalten).
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                continue;
            }
            // Kessel entfernt -> Inhalt sicher ausgeben und Station verwerfen.
            if (block.getType() != blockMaterial()) {
                Inventory openNow = openInventory(station);
                if (openNow != null) readContainer(station, openNow);
                closeViewers(station);
                dropAll(station);
                it.remove();
                save();
                continue;
            }

            // Offene GUI ist massgeblich: erst einlesen.
            Inventory open = openInventory(station);
            if (open != null) readContainer(station, open);

            process(station);

            if (open != null) {
                writeContainer(station, open);
                renderStatus(station, open);
            }

            if (isEmpty(station) && open == null) {
                it.remove();
            }
        }
    }

    private void process(Station station) {
        Block block = station.block;

        if (station.active == null) {
            if (!canCook(block)) return;
            CauldronRecipe match = findMatch(station);
            if (match == null) return;
            station.active = match;
            station.elapsed = 0;
            sizzle(block.getLocation());
        }

        if (!canCook(block)) {
            // Nicht mehr kochbereit (Wasser leer / Waerme weg) -> pausieren.
            station.active = null;
            station.elapsed = 0;
            return;
        }

        cookParticles(block.getLocation(), 3);
        station.elapsed += TICK_INTERVAL;
        if (station.elapsed < station.active.duration()) return;

        // Charge fertig: pruefen, ob die Zutaten noch da sind und Platz im Ausgang ist.
        CauldronRecipe recipe = station.active;
        List<int[]> plan = matchPlan(station, recipe);
        ItemStack result = recipe.result().build(plugin);
        if (plan == null || result == null) {
            station.active = null;
            station.elapsed = 0;
            return;
        }
        if (!hasOutputRoom(station, result)) {
            // Ausgang voll -> fertig halten und im naechsten Tick erneut versuchen.
            station.elapsed = station.active.duration();
            return;
        }

        // Zutaten verbrauchen (inkl. Behaelter-Reste), Ergebnis ausgeben.
        List<ItemStack> remainders = consume(station, plan);
        addToOutput(station, result);
        for (ItemStack remainder : remainders) addToOutput(station, remainder);
        afterBatch(block, recipe);
        plugin.effects().finish(block.getLocation());

        station.active = null;
        station.elapsed = 0;
        save();
    }

    /** Findet das erste Rezept, dessen Zutaten die Eingabe-Slots erfuellen. */
    private CauldronRecipe findMatch(Station station) {
        for (CauldronRecipe recipe : recipes) {
            if (matchPlan(station, recipe) != null) return recipe;
        }
        return null;
    }

    /**
     * Ermittelt, aus welchen Eingabe-Slots je Zutat ein Stueck entnommen wird.
     * Liefert eine Liste {slotIndex} je Zutat oder {@code null}, wenn das Rezept
     * mit dem aktuellen Inhalt nicht erfuellbar ist.
     */
    private List<int[]> matchPlan(Station station, CauldronRecipe recipe) {
        int[] remaining = new int[station.input.length];
        for (int i = 0; i < station.input.length; i++) {
            ItemStack s = station.input[i];
            remaining[i] = (s == null || s.getType().isAir()) ? 0 : s.getAmount();
        }
        List<int[]> plan = new ArrayList<>();
        for (Ingredient ingredient : recipe.ingredients()) {
            int chosen = -1;
            for (int i = 0; i < station.input.length; i++) {
                if (remaining[i] <= 0) continue;
                if (ingredient.matches(plugin, station.input[i])) {
                    chosen = i;
                    break;
                }
            }
            if (chosen < 0) return null;
            remaining[chosen]--;
            plan.add(new int[]{chosen});
        }
        return plan;
    }

    /** Entnimmt je Zutat ein Stueck und sammelt Behaelter-Reste (leere Eimer). */
    private List<ItemStack> consume(Station station, List<int[]> plan) {
        List<ItemStack> remainders = new ArrayList<>();
        for (int[] entry : plan) {
            int slot = entry[0];
            ItemStack stack = station.input[slot];
            if (stack == null || stack.getType().isAir()) continue;
            ItemStack remainder = remainderOf(stack);
            if (remainder != null) remainders.add(remainder);
            stack.setAmount(stack.getAmount() - 1);
            if (stack.getAmount() <= 0) station.input[slot] = null;
        }
        return remainders;
    }

    private boolean hasOutputRoom(Station station, ItemStack stack) {
        int need = stack.getAmount();
        for (int slot : OUTPUT_SLOTS) {
            ItemStack existing = station.outputAt(slot);
            if (existing == null || existing.getType().isAir()) return true;
            if (existing.isSimilar(stack) && existing.getAmount() < existing.getMaxStackSize()) {
                need -= (existing.getMaxStackSize() - existing.getAmount());
                if (need <= 0) return true;
            }
        }
        return need <= 0;
    }

    private void addToOutput(Station station, ItemStack stack) {
        ItemStack rest = stack.clone();
        // Zuerst in gleichartige Stacks, dann in leere Ausgabe-Slots.
        for (int slot : OUTPUT_SLOTS) {
            ItemStack existing = station.outputAt(slot);
            if (existing != null && existing.isSimilar(rest)) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) continue;
                int move = Math.min(space, rest.getAmount());
                existing.setAmount(existing.getAmount() + move);
                rest.setAmount(rest.getAmount() - move);
                if (rest.getAmount() <= 0) return;
            }
        }
        for (int slot : OUTPUT_SLOTS) {
            ItemStack existing = station.outputAt(slot);
            if (existing == null || existing.getType().isAir()) {
                station.setOutput(slot, rest.clone());
                return;
            }
        }
        // Kein Platz mehr (sollte durch hasOutputRoom verhindert sein) -> ausgeben.
        Location out = station.block.getLocation().add(0.5, 1.1, 0.5);
        if (out.getWorld() != null) {
            org.bukkit.entity.Item drop = out.getWorld().dropItem(out, rest);
            drop.setInvulnerable(true);
        }
    }

    /** Legt einen Stack in die Eingabe-Slots (merge + freie Slots). Rest zurueck. */
    private ItemStack addToInput(Station station, ItemStack stack) {
        ItemStack rest = stack.clone();
        for (int i = 0; i < station.input.length; i++) {
            ItemStack existing = station.input[i];
            if (existing != null && existing.isSimilar(rest)) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) continue;
                int move = Math.min(space, rest.getAmount());
                existing.setAmount(existing.getAmount() + move);
                rest.setAmount(rest.getAmount() - move);
                if (rest.getAmount() <= 0) return null;
            }
        }
        for (int i = 0; i < station.input.length; i++) {
            if (station.input[i] == null || station.input[i].getType().isAir()) {
                station.input[i] = rest.clone();
                return null;
            }
        }
        return rest;
    }

    /** Behaelter-Rest einer Zutat (Milch-/Wassereimer -> leerer Eimer). */
    private ItemStack remainderOf(ItemStack stack) {
        return switch (stack.getType()) {
            case MILK_BUCKET, WATER_BUCKET, LAVA_BUCKET, POWDER_SNOW_BUCKET -> new ItemStack(Material.BUCKET);
            default -> null;
        };
    }

    // --- Container <-> Inventar ----------------------------------------------

    private void writeContainer(Station station, Inventory inv) {
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            inv.setItem(INPUT_SLOTS[i], station.input[i]);
        }
        for (int slot : OUTPUT_SLOTS) {
            inv.setItem(slot, station.outputAt(slot));
        }
    }

    private void readContainer(Station station, Inventory inv) {
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            ItemStack s = inv.getItem(INPUT_SLOTS[i]);
            station.input[i] = (s == null || s.getType().isAir()) ? null : s.clone();
        }
        station.output.clear();
        for (int slot : OUTPUT_SLOTS) {
            ItemStack s = inv.getItem(slot);
            if (s != null && !s.getType().isAir()) station.output.put(slot, s.clone());
        }
    }

    private void syncOpenInventory(Station station) {
        Inventory open = openInventory(station);
        if (open != null) {
            writeContainer(station, open);
            renderStatus(station, open);
        }
    }

    // --- Rendering -----------------------------------------------------------

    private void renderStatic(Inventory inv) {
        ItemStack filler = Icons.accent();
        for (int i = 0; i < GUI_SIZE; i++) {
            if (isInputSlot(i) || isOutputSlot(i) || i == PROGRESS_SLOT || i == INFO_SLOT) continue;
            inv.setItem(i, filler);
        }
    }

    private void renderStatus(Station station, Inventory inv) {
        inv.setItem(INFO_SLOT, infoIcon(station.block));
        inv.setItem(PROGRESS_SLOT, progressIcon(station));
    }

    private ItemStack progressIcon(Station station) {
        if (station.active == null) {
            return Icons.of(Material.GRAY_STAINED_GLASS_PANE, "<gray>Waiting for ingredients",
                    "<dark_gray>Add matching ingredients to the left.");
        }
        int percent = (int) Math.round(100.0 * Math.min(1.0, (double) station.elapsed / station.active.duration()));
        String result = station.active.result().display(plugin);
        return Icons.of(Material.LIME_STAINED_GLASS_PANE, "<green>Cooking... <white>" + percent + "%",
                "<gray>Next: " + result);
    }

    // --- Persistenz ----------------------------------------------------------

    private void load() {
        stations.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            Block block = block(key);
            if (block == null) continue;
            Station station = new Station(block);
            List<?> inputs = yml.getList(key + ".input");
            if (inputs != null) {
                int idx = 0;
                for (Object raw : inputs) {
                    if (idx >= station.input.length) break;
                    if (raw instanceof ItemStack s && !s.getType().isAir()) {
                        station.input[idx++] = s;
                    }
                }
            }
            for (String slotKey : childKeys(yml, key + ".output")) {
                ItemStack s = yml.getItemStack(key + ".output." + slotKey);
                if (s != null) {
                    try {
                        station.output.put(Integer.parseInt(slotKey), s);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            stations.put(key, station);
        }
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, Station> entry : stations.entrySet()) {
            Station station = entry.getValue();
            if (isEmpty(station)) continue;
            List<ItemStack> inputs = new ArrayList<>();
            for (ItemStack s : station.input) {
                if (s != null && !s.getType().isAir()) inputs.add(s);
            }
            yml.set(entry.getKey() + ".input", inputs);
            for (Map.Entry<Integer, ItemStack> out : station.output.entrySet()) {
                yml.set(entry.getKey() + ".output." + out.getKey(), out.getValue());
            }
        }
        try {
            yml.save(file);
        } catch (Exception ex) {
            plugin.getLogger().warning(file.getName() + " konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private java.util.Set<String> childKeys(YamlConfiguration yml, String path) {
        var section = yml.getConfigurationSection(path);
        return section == null ? java.util.Set.of() : section.getKeys(false);
    }

    // --- Hilfen --------------------------------------------------------------

    private Station station(Block block, boolean create) {
        String key = key(block);
        Station station = stations.get(key);
        if (station == null && create) {
            station = new Station(block);
            stations.put(key, station);
        }
        return station;
    }

    private boolean isEmpty(Station station) {
        if (station.active != null) return false;
        for (ItemStack s : station.input) {
            if (s != null && !s.getType().isAir()) return false;
        }
        return station.output.isEmpty();
    }

    private void pruneEmpty(Station station) {
        if (isEmpty(station) && openInventory(station) == null) {
            stations.remove(key(station.block));
        }
    }

    private void dropAll(Station station) {
        Location out = station.block.getLocation().add(0.5, 0.5, 0.5);
        if (out.getWorld() != null) {
            for (ItemStack s : station.input) {
                if (s != null && !s.getType().isAir()) out.getWorld().dropItemNaturally(out, s);
            }
            for (ItemStack s : station.output.values()) {
                if (s != null && !s.getType().isAir()) out.getWorld().dropItemNaturally(out, s);
            }
        }
        // Nach dem Ausgeben leeren, damit nichts doppelt persistiert/gedroppt wird.
        java.util.Arrays.fill(station.input, null);
        station.output.clear();
        station.active = null;
        station.elapsed = 0;
    }

    private Inventory openInventory(Station station) {
        for (HumanEntity viewer : Bukkit.getOnlinePlayers()) {
            Inventory top = viewer.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof CauldronMenuHolder holder
                    && holder.stationKey().equals(key(station.block))
                    && holder.station() == this) {
                return top;
            }
        }
        return null;
    }

    private List<HumanEntity> viewers(Station station) {
        Inventory open = openInventory(station);
        return open == null ? List.of() : open.getViewers();
    }

    private void closeViewers(Station station) {
        for (HumanEntity viewer : new ArrayList<>(viewers(station))) {
            viewer.closeInventory();
        }
    }

    protected String key(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private Block block(String key) {
        String[] p = key.split(":");
        if (p.length < 4) return null;
        org.bukkit.World world = plugin.getServer().getWorld(p[0]);
        if (world == null) return null;
        try {
            return world.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Baut ein einfaches Icon mit Namen/Lore (fuer Unterklassen). */
    protected ItemStack label(Material material, String name, String... lore) {
        return Icons.of(material, name, lore);
    }

    /** Setzt nur einen Anzeigenamen auf ein Item (fuer Info-Icons). */
    protected ItemStack named(ItemStack item, String name, String... lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.line(name));
            if (lore.length > 0) {
                List<Component> lines = new ArrayList<>();
                for (String s : lore) lines.add(Text.line(s));
                meta.lore(lines);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Zustand eines einzelnen Kessels. */
    private static final class Station {
        private final Block block;
        private final ItemStack[] input = new ItemStack[INPUT_SLOTS.length];
        private final Map<Integer, ItemStack> output = new LinkedHashMap<>();
        private CauldronRecipe active;
        private int elapsed;

        private Station(Block block) {
            this.block = block;
        }

        private ItemStack outputAt(int slot) {
            return output.get(slot);
        }

        private void setOutput(int slot, ItemStack stack) {
            if (stack == null || stack.getType().isAir()) output.remove(slot);
            else output.put(slot, stack);
        }
    }
}
