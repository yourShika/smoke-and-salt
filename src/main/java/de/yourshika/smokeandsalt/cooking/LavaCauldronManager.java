package de.yourshika.smokeandsalt.cooking;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Lavakessel-Station (Frittier-/Bratstation). Wie der Wasserkessel sammelt sie
 * Zutaten in einem persistenten Container und arbeitet Mehr-Zutaten-Rezepte
 * Charge fuer Charge ab - die Lava liefert die Hitze, es gibt keinen Wasserstand.
 */
public final class LavaCauldronManager extends CauldronStation {

    public LavaCauldronManager(SmokeAndSalt plugin) {
        super(plugin, "lava_cauldron_stations.yml");
    }

    @Override
    protected boolean isStationBlock(Block block) {
        // Auch der leere Kessel zaehlt weiter (z.B. Lava herausgeschoepft),
        // damit der Inhalt und die GUI erhalten bleiben.
        return block.getType() == Material.LAVA_CAULDRON || block.getType() == Material.CAULDRON;
    }

    @Override
    protected double displayHeight() {
        return 0.7;
    }

    @Override
    protected boolean canCook(Block block) {
        return block.getType() == Material.LAVA_CAULDRON;
    }

    @Override
    protected void cookParticles(Location center, int count) {
        plugin.effects().fry(center, count);
    }

    @Override
    protected void sizzle(Location center) {
        plugin.effects().sizzle(center, true);
    }

    @Override
    protected void afterBatch(Block block, CauldronRecipe recipe) {
        // Lava verbraucht sich nicht.
    }

    @Override
    protected Component menuTitle(Block block) {
        return Text.line("<gold>Lava Cauldron");
    }

    @Override
    protected ItemStack infoIcon(Block block) {
        return label(Material.LAVA_BUCKET, "<gold>Frying heat",
                "<gray>State: <gold>Hot",
                "<dark_gray>Deep-fries and roasts its ingredients.");
    }
}
