package de.yourshika.smokeandsalt.listener;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.seed.SeedDefinition;
import de.yourshika.smokeandsalt.seed.SeedManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
        if (!player.hasPermission("smokeandsalt.seed.plant")) return;
        if (!plugin.pluginConfig().isWorldAllowed(clicked.getWorld().getName())) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        String seedId = seeds.idOf(hand);
        if (seedId == null) return;
        SeedDefinition def = seeds.definition(seedId);
        if (def == null || !def.plantable()) return;

        Block above = clicked.getRelative(0, 1, 0);
        if (!above.getType().isAir()) return;

        event.setCancelled(true);
        try {
            above.setType(def.cropMaterial(), false);
            if (above.getBlockData() instanceof Ageable ageable) {
                ageable.setAge(0);
                above.setBlockData(ageable, false);
            }
            seeds.cropStore().put(above, def.id());
            if (player.getGameMode() != GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
            }
            plugin.effects().sizzle(above.getLocation(), false);
        } catch (Exception ex) {
            plugin.debug("Seed '" + def.id() + "' konnte nicht gepflanzt werden: " + ex.getMessage());
        }
    }

    // --- Ernte + Gras-Drops -------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.pluginConfig().seedsEnabled()) return;
        Block block = event.getBlock();

        // Ernte eines Custom-Crops.
        if (seeds.cropStore().contains(block)) {
            String seedId = seeds.cropStore().remove(block);
            SeedDefinition def = seeds.definition(seedId);
            if (def != null) {
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

        // Drops beim Abbauen von Land-Gras bzw. Seegras.
        if (!seeds.isEmpty()) {
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

    private int seedReturn(SeedDefinition def) {
        if (def.seedReturnMax() <= 0) return 0;
        int min = def.seedReturnMin();
        int max = def.seedReturnMax();
        return min + (int) Math.floor(Math.random() * (max - min + 1));
    }

    // --- Komposter-Drops ----------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onComposter(PlayerInteractEvent event) {
        if (!plugin.pluginConfig().seedsEnabled()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COMPOSTER) return;
        if (seeds.isEmpty()) return;
        if (!(block.getBlockData() instanceof Levelled levelled)) return;
        if (levelled.getLevel() < levelled.getMaximumLevel()) return; // nur voller Komposter

        for (SeedDefinition def : seeds.all()) {
            if (def.composterChance() > 0 && Math.random() < def.composterChance()) {
                ItemStack drop = seeds.create(def.id(), 1);
                if (drop != null) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1.0, 0.5), drop);
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
}
