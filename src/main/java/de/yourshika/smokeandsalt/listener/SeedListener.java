package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.seed.SeedDefinition;
import de.yourshika.smokeandsalt.seed.SeedManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Custom-Seed-Funktionen: Anpflanzen auf Ackerland, Ernte des Custom-Ergebnisses
 * bei voller Reife sowie Drops beim Abbauen von Gras und an vollen Komposter.
 * Ohne konfigurierte Seeds passiert nichts.
 */
public final class SeedListener implements Listener {

    private final SmokeAndSalt plugin;
    private final SeedManager seeds;

    public SeedListener(SmokeAndSalt plugin, SeedManager seeds) {
        this.plugin = plugin;
        this.seeds = seeds;
    }

    // --- Anpflanzen ---------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onPlant(PlayerInteractEvent event) {
        if (!plugin.pluginConfig().seedsEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.FARMLAND) return;

        Player player = event.getPlayer();
        if (!plugin.pluginConfig().isWorldAllowed(clicked.getWorld().getName())) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        Block above = clicked.getRelative(0, 1, 0);
        var crops = plugin.crops();

        if (crops.enabled()) {
            // Bestehender Custom-Crop: ernten (reif) oder duengen (Bonemeal).
            if (crops.contains(above)) {
                event.setCancelled(true);
                if (hand.getType() == Material.BONE_MEAL) {
                    if (crops.bonemeal(above) && player.getGameMode() != GameMode.CREATIVE) {
                        hand.setAmount(hand.getAmount() - 1);
                    }
                } else {
                    crops.interact(player, above);
                }
                return;
            }
            // Neu pflanzen (ohne Weizenblock -> keine doppelte Textur).
            String seedId = seeds.idOf(hand);
            if (seedId == null) return;
            SeedDefinition def = seeds.definition(seedId);
            if (def == null || !def.plantable()) return;
            if (!player.hasPermission("smokeandsalt.seed.plant")) return;
            if (!above.getType().isAir()) return;
            event.setCancelled(true);
            if (crops.plant(above, def.id()) && player.getGameMode() != GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
                plugin.effects().sizzle(above.getLocation(), false);
            }
            return;
        }

        // --- Fallback: Vanilla-Weizen-Crop (seeds.custom-crops = false) ---
        String seedId = seeds.idOf(hand);
        if (seedId == null) return;
        SeedDefinition def = seeds.definition(seedId);
        if (def == null || !def.plantable()) return;
        if (!player.hasPermission("smokeandsalt.seed.plant")) return;
        if (!above.getType().isAir()) return;
        event.setCancelled(true);
        try {
            above.setType(def.cropMaterial(), false);
            if (above.getBlockData() instanceof Ageable ageable) {
                ageable.setAge(0);
                above.setBlockData(ageable, false);
            }
            seeds.cropStore().put(above, def.id());
            spawnOrUpdateCropDisplay(above, def, 0);
            if (player.getGameMode() != GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
            }
            plugin.effects().sizzle(above.getLocation(), false);
        } catch (Exception ex) {
            plugin.debug("Seed '" + def.id() + "' konnte nicht gepflanzt werden: " + ex.getMessage());
        }
    }

    // --- Identifizieren (Rechtsklick auf Weizen-Custom-Crop) ----------------

    @EventHandler(ignoreCancelled = true)
    public void onIdentify(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("seeds.crop-identify", true)) return;
        if (plugin.crops().enabled()) return; // nur im Vanilla-Weizen-Modus
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || !seeds.cropStore().contains(block)) return;
        SeedDefinition def = seeds.definition(seeds.cropStore().get(block));
        if (def == null) return;

        String display = def.resultItemId() != null && plugin.items().definition(def.resultItemId()) != null
                ? plugin.items().definition(def.resultItemId()).displayName()
                : def.displayName();
        int stage = 0, max = 7;
        if (block.getBlockData() instanceof Ageable a) {
            stage = a.getAge();
            max = a.getMaximumAge();
        }
        event.getPlayer().sendActionBar(plugin.messages().mini().deserialize(
                display + " <dark_gray>-</dark_gray> <gray>Stage " + stage + "/" + max));
    }

    // --- Ernte + Gras-Drops -------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.pluginConfig().seedsEnabled()) return;
        Block block = event.getBlock();

        // Custom-Crop-System: Ackerland abgebaut -> Crop entfernen, Samen zurueck.
        if (plugin.crops().enabled()) {
            if (block.getType() == Material.FARMLAND) {
                Block above = block.getRelative(0, 1, 0);
                if (plugin.crops().contains(above)) {
                    plugin.crops().removeAndDropSeed(above);
                }
            }
        }

        // Ernte eines Vanilla-Weizen-Crops (nur ohne Custom-Crop-System).
        if (!plugin.crops().enabled() && seeds.cropStore().contains(block)) {
            String seedId = seeds.cropStore().remove(block);
            SeedDefinition def = seeds.definition(seedId);
            if (def != null) {
                removeCropDisplays(block);
                event.setDropItems(false);
                var loc = block.getLocation().add(0.5, 0.3, 0.5);
                boolean ripe = !(block.getBlockData() instanceof Ageable a) || a.getAge() >= a.getMaximumAge();
                if (ripe) {
                    ItemStack harvest = harvestResult(def);
                    if (harvest != null) block.getWorld().dropItemNaturally(loc, harvest);
                    // Ein paar Samen zurueckgeben (wie bei Weizen).
                    int seedCount = seedReturn(def);
                    if (seedCount > 0) {
                        ItemStack back = seeds.create(def.id(), seedCount);
                        if (back != null) block.getWorld().dropItemNaturally(loc, back);
                    }
                } else {
                    ItemStack back = seeds.create(def.id(), 1);
                    if (back != null) block.getWorld().dropItemNaturally(loc, back);
                }
                plugin.effects().finish(block.getLocation());
            }
            return;
        }

        // Konfigurierte Seed-Drops (drops-from / biomes / chance).
        // Nicht im Kreativ-Modus und nicht mit Silk Touch (dann bleibt der Block heil).
        Player breaker = event.getPlayer();
        boolean silk = breaker.getInventory().getItemInMainHand()
                .containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH);
        if (breaker.getGameMode() != GameMode.CREATIVE && !silk && !seeds.isEmpty()) {
            for (SeedDefinition def : seeds.all()) {
                for (de.yourshika.smokeandsalt.seed.SeedDrop drop : seeds.dropsFor(def.id())) {
                    if (drop.matches(block) && Math.random() < drop.chance()) {
                        ItemStack seed = seeds.create(def.id(), 1);
                        if (seed != null) {
                            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), seed);
                        }
                        break; // pro Seed nur ein passender Drop-Wurf
                    }
                }
            }
            // Rueckwaerts-Kompatibilitaet: alte grass-/seagrass-chance.
            boolean land = isGrass(block.getType());
            boolean sea = isSeagrass(block.getType());
            if (land || sea) {
                for (SeedDefinition def : seeds.all()) {
                    double chance = sea ? def.seagrassChance() : def.grassChance();
                    if (chance > 0 && Math.random() < chance) {
                        ItemStack drop = seeds.create(def.id(), 1);
                        if (drop != null) {
                            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), drop);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        updateCropDisplayNextTick(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        for (BlockState state : event.getBlocks()) {
            updateCropDisplayNextTick(state.getBlock());
        }
    }

    private int seedReturn(SeedDefinition def) {
        if (def.seedReturnMax() <= 0) return 0;
        int min = def.seedReturnMin();
        int max = def.seedReturnMax();
        return min + (int) Math.floor(Math.random() * (max - min + 1));
    }

    // --- Komposter ----------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onComposter(PlayerInteractEvent event) {
        if (!plugin.pluginConfig().seedsEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COMPOSTER) return;
        if (seeds.isEmpty()) return;
        if (!(block.getBlockData() instanceof Levelled levelled)) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Custom-Produkt in den Komposter -> Chance auf das passende Saatgut.
        String produceId = plugin.items().idOf(hand);
        if (produceId != null && levelled.getLevel() < levelled.getMaximumLevel()) {
            SeedDefinition match = null;
            for (SeedDefinition def : seeds.all()) {
                if (produceId.equalsIgnoreCase(def.resultItemId())) {
                    match = def;
                    break;
                }
            }
            if (match != null) {
                event.setCancelled(true);
                if (player.getGameMode() != GameMode.CREATIVE) hand.setAmount(hand.getAmount() - 1);
                double chance = plugin.getConfig().getDouble("seeds.compost-seed-chance", 0.4);
                if (Math.random() < chance) {
                    ItemStack seed = seeds.create(match.id(), 1);
                    if (seed != null) {
                        player.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5), seed);
                    }
                }
                plugin.effects().finish(block.getLocation());
                return;
            }
        }

        // Voller Komposter: konfigurierte composter-chance-Seeds.
        if (levelled.getLevel() >= levelled.getMaximumLevel()) {
            for (SeedDefinition def : seeds.all()) {
                if (def.composterChance() > 0 && Math.random() < def.composterChance()) {
                    ItemStack drop = seeds.create(def.id(), 1);
                    if (drop != null) {
                        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5), drop);
                    }
                }
            }
        }
    }

    private ItemStack harvestResult(SeedDefinition def) {
        if (def.resultItemId() != null) {
            ItemStack item = plugin.items().create(def.resultItemId(), def.resultAmount());
            if (item != null) return item;
        }
        if (def.resultMaterial() != null) {
            return new ItemStack(def.resultMaterial(), def.resultAmount());
        }
        return seeds.create(def.id(), def.resultAmount());
    }

    private boolean isGrass(Material material) {
        return material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.FERN
                || material == Material.LARGE_FERN;
    }

    private boolean isSeagrass(Material material) {
        return material == Material.SEAGRASS || material == Material.TALL_SEAGRASS;
    }

    private void updateCropDisplayNextTick(Block block) {
        if (!seeds.cropStore().contains(block)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String seedId = seeds.cropStore().get(block);
            SeedDefinition def = seeds.definition(seedId);
            if (def != null) {
                spawnOrUpdateCropDisplay(block, def, cropStage(block));
            }
        });
    }

    private void spawnOrUpdateCropDisplay(Block crop, SeedDefinition def, int stage) {
        removeCropDisplays(crop);
        // Standardmaessig aus: sonst sieht man Weizen UND das schwebende Custom-Modell
        // (doppelte Textur). Wer die Custom-Optik will, aktiviert seeds.crop-display.
        if (!plugin.getConfig().getBoolean("seeds.crop-display", false)) return;
        String providerId = cropProviderId(def, stage);
        if (providerId == null || !plugin.moduleManager().isActive("oraxen")) return;

        ItemStack visual = new ItemStack(Material.PAPER);
        plugin.moduleManager().applyExternalModel(visual, providerId);
        Location loc = crop.getLocation().add(0.5, 0.42, 0.5);
        crop.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(visual);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setShadowRadius(0.0f);
            display.setShadowStrength(0.0f);
            display.setViewRange(24.0f);
            display.getPersistentDataContainer().set(plugin.keys().cropDisplay,
                    PersistentDataType.STRING, cropKey(crop));
        });
    }

    private void removeCropDisplays(Block crop) {
        String key = cropKey(crop);
        Location center = crop.getLocation().add(0.5, 0.5, 0.5);
        for (Entity entity : crop.getWorld().getNearbyEntities(center, 0.9, 1.2, 0.9)) {
            if (!(entity instanceof ItemDisplay display)) continue;
            String displayKey = display.getPersistentDataContainer()
                    .get(plugin.keys().cropDisplay, PersistentDataType.STRING);
            if (key.equals(displayKey)) {
                display.remove();
            }
        }
    }

    /** Highest available crop-display stage (textures reis_stage0..reis_stage7). */
    private static final int MAX_CROP_STAGE = 7;

    private String cropProviderId(SeedDefinition def, int stage) {
        if (!def.id().equalsIgnoreCase("reis_samen")) return null;
        return "sas_reis_crop_" + Math.max(0, Math.min(MAX_CROP_STAGE, stage));
    }

    private int cropStage(Block crop) {
        if (crop.getBlockData() instanceof Ageable ageable && ageable.getMaximumAge() > 0) {
            // Map the crop's age (wheat: 0..7) onto the available display stages.
            return Math.min(MAX_CROP_STAGE, (ageable.getAge() * MAX_CROP_STAGE) / ageable.getMaximumAge());
        }
        return 0;
    }

    private String cropKey(Block block) {
        return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }
}
