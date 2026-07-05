package de.yourshika.smokeandsalt.seed;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Set;

/**
 * Beschreibt, aus welchen Bloecken (optional in bestimmten Biomen) ein Seed mit
 * welcher Chance droppt. Geladen aus der Seed-Config
 * ({@code drops-from} / {@code biomes} / {@code chance}).
 *
 * @param blocks Bloecke, aus denen gedroppt wird
 * @param biomes Biome-Namen (leer = alle Biome), z.B. "plains"
 * @param chance Drop-Wahrscheinlichkeit 0..1
 */
public record SeedDrop(Set<Material> blocks, Set<String> biomes, double chance) {

    public SeedDrop {
        blocks = Set.copyOf(blocks);
        biomes = Set.copyOf(biomes);
    }

    /** Passt dieser Drop auf den abgebauten Block (Material + Biom)? */
    public boolean matches(Block block) {
        if (!blocks.contains(block.getType())) return false;
        if (biomes.isEmpty()) return true;
        return biomes.contains(biomeName(block));
    }

    public static String biomeName(Block block) {
        try {
            return block.getBiome().getKey().getKey().toLowerCase(java.util.Locale.ROOT);
        } catch (Throwable t) {
            return block.getBiome().toString().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
