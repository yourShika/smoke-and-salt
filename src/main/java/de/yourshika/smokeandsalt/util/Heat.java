package de.yourshika.smokeandsalt.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;

/**
 * Erkennt Waermequellen unter einem Block. Ein Wasserkessel gilt nur dann als
 * "kochend", wenn direkt darunter eine aktive Waermequelle liegt (Lagerfeuer,
 * Feuer, Lava oder Magmablock).
 */
public final class Heat {

    private Heat() {
    }

    /** Liegt direkt unter {@code block} eine aktive Waermequelle? */
    public static boolean hasHeatSourceBelow(Block block) {
        return isHeatSource(block.getRelative(0, -1, 0));
    }

    /** Ist der Block selbst eine aktive Waermequelle? */
    public static boolean isHeatSource(Block block) {
        Material type = block.getType();
        switch (type) {
            case FIRE:
            case SOUL_FIRE:
            case LAVA:
            case MAGMA_BLOCK:
                return true;
            case CAMPFIRE:
            case SOUL_CAMPFIRE:
                BlockData data = block.getBlockData();
                return !(data instanceof Lightable light) || light.isLit();
            default:
                return false;
        }
    }
}
