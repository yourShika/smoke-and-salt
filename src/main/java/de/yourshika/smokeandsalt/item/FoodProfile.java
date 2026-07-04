package de.yourshika.smokeandsalt.item;

import org.bukkit.potion.PotionEffect;

import java.util.List;

/**
 * Nahrungseigenschaften fuer Custom-Items. Nutrition entspricht halben
 * Hungerkeulen, saturation ist der konkrete Saturation-Wert des Items.
 */
public record FoodProfile(
        int nutrition,
        float saturation,
        boolean canAlwaysEat,
        List<PotionEffect> effects
) {
    public FoodProfile {
        nutrition = Math.max(0, nutrition);
        saturation = Math.max(0.0f, saturation);
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    public static FoodProfile of(int nutrition, float saturation) {
        return new FoodProfile(nutrition, saturation, false, List.of());
    }

    public static FoodProfile withEffect(int nutrition, float saturation, PotionEffect effect) {
        return new FoodProfile(nutrition, saturation, false, List.of(effect));
    }
}
