package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Heat;
import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.inventory.ItemStack;

/**
 * Wasserkessel-Station. Sammelt Zutaten in einem persistenten Container (per
 * Rechtsklick-GUI oder durch Hineinwerfen) und kocht Charge fuer Charge, solange
 * Wasser und eine Waermequelle darunter vorhanden sind. Manche Rezepte verbrauchen
 * dabei Wasserlevel.
 */
public final class CauldronManager extends CauldronStation {

    public CauldronManager(SmokeAndSalt plugin) {
        super(plugin, "water_cauldron_stations.yml");
    }

    @Override
    protected Material blockMaterial() {
        return Material.WATER_CAULDRON;
    }

    @Override
    protected boolean canCook(Block block) {
        return block.getType() == Material.WATER_CAULDRON
                && waterLevel(block) > 0
                && Heat.hasHeatSourceBelow(block);
    }

    @Override
    protected void cookParticles(Location center, int count) {
        plugin.effects().boil(center, count);
    }

    @Override
    protected void sizzle(Location center) {
        plugin.effects().sizzle(center, false);
    }

    @Override
    protected void afterBatch(Block block, CauldronRecipe recipe) {
        reduceWater(block, recipe.waterCost());
    }

    @Override
    protected Component menuTitle(Block block) {
        return Text.line("<dark_aqua>Water Cauldron <dark_gray>(" + waterLevel(block) + "/3)");
    }

    @Override
    protected ItemStack infoIcon(Block block) {
        int level = waterLevel(block);
        boolean heat = Heat.hasHeatSourceBelow(block);
        String state = level <= 0 ? "<red>Empty - fill with water"
                : !heat ? "<yellow>No heat source below"
                : "<aqua>Boiling";
        return label(Material.WATER_BUCKET, "<aqua>Water <dark_gray>(" + level + "/3)",
                "<gray>State: " + state,
                "<dark_gray>Needs water + heat (fire/lava/campfire) below.");
    }

    // --- Wasserstand ---------------------------------------------------------

    private int waterLevel(Block block) {
        if (block.getType() == Material.WATER_CAULDRON && block.getBlockData() instanceof Levelled levelled) {
            return levelled.getLevel();
        }
        return 0;
    }

    private void reduceWater(Block block, int levels) {
        if (levels <= 0 || block.getType() != Material.WATER_CAULDRON) return;
        int next = Math.max(0, waterLevel(block) - levels);
        if (next <= 0) {
            block.setType(Material.CAULDRON, false);
            return;
        }
        if (block.getBlockData() instanceof Levelled levelled) {
            levelled.setLevel(next);
            block.setBlockData(levelled, false);
        }
    }
}
