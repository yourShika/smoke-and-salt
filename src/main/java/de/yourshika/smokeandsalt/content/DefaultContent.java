package de.yourshika.smokeandsalt.content;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CauldronRecipe;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.crafting.CraftingRecipe;
import de.yourshika.smokeandsalt.item.FoodProfile;
import de.yourshika.smokeandsalt.item.ItemDefinition;
import de.yourshika.smokeandsalt.seed.SeedDefinition;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Registriert den mitgelieferten Standard-Inhalt: Custom-Items, Custom-Seeds und
 * alle Rezepte (Smoker, Lagerfeuer, Kessel, Werkbank). Wird nach dem Laden der
 * config.yml aufgerufen, sodass eigene Config-Eintraege mit gleicher ID Vorrang
 * haben.
 */
public final class DefaultContent {

    private DefaultContent() {
    }

    public static void register(SmokeAndSalt plugin) {
        registerItems(plugin);
        registerSeeds(plugin);
        registerStationRecipes(plugin);
        registerCauldronRecipes(plugin);
        registerCraftingRecipes(plugin);
    }

    // --- Custom-Items -------------------------------------------------------

    private static void registerItems(SmokeAndSalt plugin) {
        // Fallback-Werte (falls content/items.yml fehlt) - identisch zur config.
        item(plugin, "teig", Material.PAPER, "<#e8d8a8>Dough", "Ingredient",
                FoodProfile.withEffect(1, 0.4f, effect(PotionEffectType.HUNGER, 100)));
        item(plugin, "sourcream", Material.PAPER, "<#fff8e7>Sour Cream", "Ingredient",
                FoodProfile.of(2, 1.2f));
        item(plugin, "spiegelei", Material.PAPER, "<#fff3c0>Fried Egg", "Dish",
                FoodProfile.of(4, 4.8f));
        item(plugin, "rotebeete_chips", Material.BEETROOT, "<#c0392b>Beetroot Chips", "Snack",
                FoodProfile.of(3, 2.4f));
        item(plugin, "geroestete_karotte", Material.CARROT, "<#e67e22>Roasted Carrot", "Dish",
                FoodProfile.withEffect(4, 4.8f, effect(PotionEffectType.NIGHT_VISION, 100)));
        item(plugin, "pommes", Material.POTATO, "<#f1c40f>Fries", "Snack",
                FoodProfile.of(5, 6.0f));
        item(plugin, "marshmallow", Material.PAPER, "<#ffeef2>Marshmallow", "Snack",
                FoodProfile.withEffect(3, 1.8f, effect(PotionEffectType.SPEED, 80)));
        item(plugin, "kaiserbroetchen", Material.BREAD, "<#d9a441>Kaiser Roll", "Dish",
                FoodProfile.of(5, 6.0f));
        item(plugin, "nudeln", Material.PAPER, "<#f0e2b0>Noodles", "Ingredient",
                FoodProfile.of(4, 4.8f));
        item(plugin, "kaese", Material.HONEYCOMB, "<#f2c94c>Cheese", "Ingredient",
                FoodProfile.of(3, 3.6f));
        item(plugin, "sauce", Material.BRICK, "<#c0392b>Sauce", "Ingredient",
                FoodProfile.of(2, 1.6f));
        item(plugin, "burger", Material.BREAD, "<#e2a76f>Burger", "Dish",
                FoodProfile.withEffect(9, 11.5f, effect(PotionEffectType.STRENGTH, 100)));
        item(plugin, "cheeseburger", Material.BREAD, "<#f2c94c>Cheeseburger", "Dish",
                FoodProfile.withEffect(10, 13.0f, effect(PotionEffectType.STRENGTH, 120)));
        item(plugin, "chicken_nuggets", Material.COOKED_CHICKEN, "<#e6b566>Chicken Nuggets", "Snack",
                FoodProfile.of(6, 7.2f));
        item(plugin, "schaschlik", Material.COOKED_BEEF, "<#b5651d>Shashlik", "Dish",
                FoodProfile.withEffect(8, 10.0f, effect(PotionEffectType.STRENGTH, 100)));
        item(plugin, "ofenkartoffel_sourcream", Material.BAKED_POTATO, "<#e9d8a6>Baked Potato with Sour Cream", "Dish",
                FoodProfile.withEffect(8, 10.0f, effect(PotionEffectType.RESISTANCE, 80)));
        item(plugin, "spaghetti", Material.PAPER, "<#f0e2b0>Spaghetti", "Dish",
                FoodProfile.of(8, 9.6f));
        item(plugin, "misosuppe", Material.MUSHROOM_STEW, "<#c98a3a>Miso Soup", "Soup",
                FoodProfile.withEffect(7, 8.4f, effect(PotionEffectType.REGENERATION, 80)));
        item(plugin, "kandierter_apfel", Material.APPLE, "<#e74c3c>Candy Apple", "Snack",
                FoodProfile.withEffect(5, 4.0f, effect(PotionEffectType.SPEED, 120)));
        item(plugin, "reis", Material.PAPER, "<#f7f3e3>Rice", "Ingredient");
    }

    private static PotionEffect effect(PotionEffectType type, int durationTicks) {
        return new PotionEffect(type, durationTicks, 0, true, true, true);
    }

    private static void item(SmokeAndSalt plugin, String id, Material base, String name, String kind) {
        item(plugin, id, base, name, kind, null);
    }

    private static void item(SmokeAndSalt plugin, String id, Material base, String name, String kind, FoodProfile food) {
        if (plugin.items().contains(id)) return; // config takes priority
        plugin.items().register(new ItemDefinition(
                id, base, name,
                List.of("<dark_gray>Smoke & Salt - " + kind),
                "sas_" + id, false, food));
    }

    // --- Custom-Seeds -------------------------------------------------------

    private static void registerSeeds(SmokeAndSalt plugin) {
        if (plugin.seeds().definition("reis_samen") == null) {
            plugin.seeds().register(new SeedDefinition(
                    "reis_samen", Material.WHEAT_SEEDS, "<#e6dfbf>Rice Seeds", "sas_reis_samen",
                    Material.WHEAT,          // grows like wheat
                    "reis", null, 1,          // harvest: 1 rice
                    1, 2,                     // + 1..2 seeds back
                    0.0, 0.35, 0.0));         // drop only from seagrass (35%)
        }
    }

    // --- Smoker & Lagerfeuer (Einzel-Zutat) --------------------------------

    private static void registerStationRecipes(SmokeAndSalt plugin) {
        var reg = plugin.cooking().registry();

        // Smoker
        smoker(reg, "smoker_spiegelei", b -> b.inputMaterial(Material.EGG).resultItem("spiegelei").duration(140));
        smoker(reg, "smoker_rotebeete_chips", b -> b.inputMaterial(Material.BEETROOT).resultItem("rotebeete_chips").duration(140));
        smoker(reg, "smoker_geroestete_karotte", b -> b.inputMaterial(Material.CARROT).resultItem("geroestete_karotte").duration(140));

        // Lagerfeuer
        campfire(reg, "campfire_marshmallow", b -> b.inputMaterial(Material.SUGAR).resultItem("marshmallow").duration(120));
        campfire(reg, "campfire_kaiserbroetchen", b -> b.inputItem("teig").resultItem("kaiserbroetchen").duration(160));

        // Lavakessel
        lava(reg, "lava_pommes", b -> b.inputMaterial(Material.POTATO).resultItem("pommes").duration(140));

        // Schneiden
        cutting(reg, "cutting_chicken_nuggets", b -> b.inputMaterial(Material.COOKED_CHICKEN)
                .resultItem("chicken_nuggets").resultAmount(3).duration(60));
    }

    private interface Cfg {
        CookingRecipe.Builder apply(CookingRecipe.Builder b);
    }

    private static void smoker(de.yourshika.smokeandsalt.cooking.CookingRegistry reg, String id, Cfg cfg) {
        if (reg.contains(id)) return;
        reg.register(cfg.apply(CookingRecipe.builder(id, CookingStation.SMOKER)).build());
    }

    private static void campfire(de.yourshika.smokeandsalt.cooking.CookingRegistry reg, String id, Cfg cfg) {
        if (reg.contains(id)) return;
        reg.register(cfg.apply(CookingRecipe.builder(id, CookingStation.CAMPFIRE)).build());
    }

    private static void lava(de.yourshika.smokeandsalt.cooking.CookingRegistry reg, String id, Cfg cfg) {
        if (reg.contains(id)) return;
        reg.register(cfg.apply(CookingRecipe.builder(id, CookingStation.CAULDRON_LAVA)).build());
    }

    private static void cutting(de.yourshika.smokeandsalt.cooking.CookingRegistry reg, String id, Cfg cfg) {
        if (reg.contains(id)) return;
        reg.register(cfg.apply(CookingRecipe.builder(id, CookingStation.CUTTING)).build());
    }

    // --- Kessel (mehrere Zutaten, Wasser implizit) --------------------------

    private static void registerCauldronRecipes(SmokeAndSalt plugin) {
        var c = plugin.cauldron();
        if (!c.contains("cauldron_teig")) c.register(new CauldronRecipe("cauldron_teig",
                List.of(Ingredient.material(Material.WHEAT, "Wheat"),
                        Ingredient.material(Material.WHEAT, "Wheat"),
                        Ingredient.material(Material.WHEAT, "Wheat")),
                ResultSpec.item("teig", 1), 120, 1));
        if (!c.contains("cauldron_nudeln")) c.register(new CauldronRecipe("cauldron_nudeln",
                List.of(Ingredient.item("teig", "Dough")),
                ResultSpec.item("nudeln", 1), 200));
        if (!c.contains("cauldron_kaese")) c.register(new CauldronRecipe("cauldron_kaese",
                List.of(Ingredient.material(Material.MILK_BUCKET, "Milk")),
                ResultSpec.item("kaese", 1), 200));
        if (!c.contains("cauldron_sauce")) c.register(new CauldronRecipe("cauldron_sauce",
                List.of(Ingredient.material(Material.BEETROOT, "Beetroot"),
                        Ingredient.material(Material.CARROT, "Carrot")),
                ResultSpec.item("sauce", 1), 220));
        if (!c.contains("cauldron_misosuppe")) c.register(new CauldronRecipe("cauldron_misosuppe",
                List.of(Ingredient.item("nudeln", "Noodles"),
                        Ingredient.material(Material.CARROT, "Carrot"),
                        Ingredient.material(Material.KELP, "Kelp"),
                        Ingredient.material(Material.COD, "Fish")),
                ResultSpec.item("misosuppe", 1), 260, 0, true));
    }

    // --- Werkbank (shapeless) ----------------------------------------------

    private static void registerCraftingRecipes(SmokeAndSalt plugin) {
        var cm = plugin.crafting();

        if (!cm.contains("burger")) cm.register(new CraftingRecipe("burger",
                List.of(Ingredient.item("kaiserbroetchen", "Kaiser Roll"),
                        Ingredient.material(Material.COOKED_BEEF, "Meat")),
                ResultSpec.item("burger", 1)));

        if (!cm.contains("cheeseburger")) cm.register(new CraftingRecipe("cheeseburger",
                List.of(Ingredient.item("kaiserbroetchen", "Kaiser Roll"),
                        Ingredient.item("kaese", "Cheese"),
                        Ingredient.material(Material.COOKED_BEEF, "Meat")),
                ResultSpec.item("cheeseburger", 1)));

        if (!cm.contains("sourcream")) cm.register(new CraftingRecipe("sourcream",
                List.of(Ingredient.material(Material.MILK_BUCKET, "Milk"),
                        Ingredient.material(Material.SWEET_BERRIES, "Sweet Berries"),
                        Ingredient.material(Material.BOWL, "Bowl")),
                ResultSpec.item("sourcream", 1)));

        if (!cm.contains("schaschlik_karotte")) cm.register(new CraftingRecipe("schaschlik_karotte",
                List.of(Ingredient.material(Material.STICK, "Stick"),
                        Ingredient.material(Material.COOKED_BEEF, "Meat"),
                        Ingredient.material(Material.CARROT, "Carrot")),
                ResultSpec.item("schaschlik", 1)));

        if (!cm.contains("schaschlik_kartoffel")) cm.register(new CraftingRecipe("schaschlik_kartoffel",
                List.of(Ingredient.material(Material.STICK, "Stick"),
                        Ingredient.material(Material.COOKED_BEEF, "Meat"),
                        Ingredient.material(Material.POTATO, "Potato")),
                ResultSpec.item("schaschlik", 1)));

        if (!cm.contains("ofenkartoffel_sourcream")) cm.register(new CraftingRecipe("ofenkartoffel_sourcream",
                List.of(Ingredient.material(Material.BAKED_POTATO, "Baked Potato"),
                        Ingredient.item("sourcream", "Sour Cream")),
                ResultSpec.item("ofenkartoffel_sourcream", 1)));

        if (!cm.contains("spaghetti")) cm.register(new CraftingRecipe("spaghetti",
                List.of(Ingredient.item("nudeln", "Noodles"),
                        Ingredient.item("sauce", "Sauce"),
                        Ingredient.material(Material.BOWL, "Bowl")),
                ResultSpec.item("spaghetti", 1)));

        if (!cm.contains("kandierter_apfel")) cm.register(new CraftingRecipe("kandierter_apfel",
                List.of(Ingredient.material(Material.APPLE, "Apple"),
                        Ingredient.material(Material.SUGAR, "Sugar")),
                ResultSpec.item("kandierter_apfel", 1)));
    }
}
