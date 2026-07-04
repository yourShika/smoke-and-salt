package de.yourshika.smokeandsalt.content;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.cooking.CauldronRecipe;
import de.yourshika.smokeandsalt.cooking.CookingRecipe;
import de.yourshika.smokeandsalt.cooking.CookingStation;
import de.yourshika.smokeandsalt.crafting.CraftingRecipe;
import de.yourshika.smokeandsalt.item.ItemDefinition;
import de.yourshika.smokeandsalt.seed.SeedDefinition;
import org.bukkit.Material;
import org.bukkit.Tag;

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
        // Zwischenprodukte / Basis
        item(plugin, "teig", Material.PAPER, "<#e8d8a8>Teig", "Zutat");
        // Smoker-Ergebnisse
        item(plugin, "spiegelei", Material.EGG, "<#fff3c0>Spiegelei", "Gericht");
        item(plugin, "rotebeete_chips", Material.BEETROOT, "<#c0392b>Rote-Bete-Chips", "Snack");
        item(plugin, "geroestete_karotte", Material.CARROT, "<#e67e22>Geröstete Karotte", "Gericht");
        item(plugin, "pommes", Material.POTATO, "<#f1c40f>Pommes", "Snack");
        // Lagerfeuer-Ergebnisse
        item(plugin, "marshmallow", Material.PAPER, "<#ffeef2>Marshmallow", "Snack");
        item(plugin, "stockbrot", Material.BREAD, "<#d9a441>Stockbrot", "Gericht");
        // Kessel-Ergebnisse
        item(plugin, "nudeln", Material.PAPER, "<#f0e2b0>Nudeln", "Zutat");
        item(plugin, "kaese", Material.HONEYCOMB, "<#f2c94c>Käse", "Zutat");
        item(plugin, "sauce", Material.BRICK, "<#c0392b>Sauce", "Zutat");
        // Crafting-Ergebnisse
        item(plugin, "burger", Material.BREAD, "<#e2a76f>Burger", "Gericht");
        item(plugin, "cheeseburger", Material.BREAD, "<#f2c94c>Cheeseburger", "Gericht");
        item(plugin, "chicken_nuggets", Material.COOKED_CHICKEN, "<#e6b566>Chicken Nuggets", "Snack");
        item(plugin, "schaschlik", Material.COOKED_BEEF, "<#b5651d>Schaschlik", "Gericht");
        item(plugin, "ofenkartoffel_sourcream", Material.BAKED_POTATO, "<#e9d8a6>Ofenkartoffel mit Sauerrahm", "Gericht");
        item(plugin, "spaghetti", Material.PAPER, "<#f0e2b0>Spaghetti", "Gericht");
        item(plugin, "misosuppe", Material.MUSHROOM_STEW, "<#c98a3a>Misosuppe", "Suppe");
        item(plugin, "kandierter_apfel", Material.APPLE, "<#e74c3c>Kandierter Apfel", "Snack");
        // Custom-Seed-Ernte
        item(plugin, "reis", Material.PAPER, "<#f7f3e3>Reis", "Zutat");
    }

    private static void item(SmokeAndSalt plugin, String id, Material base, String name, String kind) {
        if (plugin.items().contains(id)) return; // config hat Vorrang
        plugin.items().register(new ItemDefinition(
                id, base, name,
                List.of("<dark_gray>Smoke & Salt · " + kind),
                "sas_" + id, false));
    }

    // --- Custom-Seeds -------------------------------------------------------

    private static void registerSeeds(SmokeAndSalt plugin) {
        if (plugin.seeds().definition("reis_samen") == null) {
            plugin.seeds().register(new SeedDefinition(
                    "reis_samen", Material.WHEAT_SEEDS, "<#e6dfbf>Reis-Samen", "sas_reis_samen",
                    Material.WHEAT,          // waechst wie Weizen
                    "reis", null, 1,          // Ernte: 1 Reis
                    1, 2,                     // + 1..2 Samen zurueck
                    0.0, 0.35, 0.0));         // Drop nur aus Seegras (35%)
        }
    }

    // --- Smoker & Lagerfeuer (Einzel-Zutat) --------------------------------

    private static void registerStationRecipes(SmokeAndSalt plugin) {
        var reg = plugin.cooking().registry();

        // Smoker
        smoker(reg, "smoker_spiegelei", b -> b.inputMaterial(Material.EGG).resultItem("spiegelei").duration(140));
        smoker(reg, "smoker_rotebeete_chips", b -> b.inputMaterial(Material.BEETROOT).resultItem("rotebeete_chips").duration(140));
        smoker(reg, "smoker_geroestete_karotte", b -> b.inputMaterial(Material.CARROT).resultItem("geroestete_karotte").duration(140));
        smoker(reg, "smoker_pommes", b -> b.inputMaterial(Material.POTATO).resultItem("pommes").duration(140));
        smoker(reg, "smoker_brot", b -> b.inputItem("teig").resultMaterial(Material.BREAD).duration(160));

        // Lagerfeuer
        campfire(reg, "campfire_marshmallow", b -> b.inputMaterial(Material.SUGAR).resultItem("marshmallow").duration(120));
        campfire(reg, "campfire_stockbrot", b -> b.inputItem("teig").resultItem("stockbrot").duration(160));
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

    // --- Kessel (mehrere Zutaten, Wasser implizit) --------------------------

    private static void registerCauldronRecipes(SmokeAndSalt plugin) {
        var c = plugin.cauldron();
        c.register(new CauldronRecipe("cauldron_nudeln",
                List.of(Ingredient.item("teig", "Teig")),
                ResultSpec.item("nudeln", 1), 200));
        c.register(new CauldronRecipe("cauldron_kaese",
                List.of(Ingredient.material(Material.MILK_BUCKET, "Milch")),
                ResultSpec.item("kaese", 1), 200));
        c.register(new CauldronRecipe("cauldron_sauce",
                List.of(Ingredient.material(Material.BEETROOT, "Rote Bete"),
                        Ingredient.material(Material.CARROT, "Karotte")),
                ResultSpec.item("sauce", 1), 220));
    }

    // --- Werkbank (shapeless) ----------------------------------------------

    private static void registerCraftingRecipes(SmokeAndSalt plugin) {
        var cm = plugin.crafting();

        cm.register(new CraftingRecipe("burger",
                List.of(Ingredient.material(Material.BREAD, "Brot"),
                        Ingredient.material(Material.COOKED_BEEF, "Fleisch")),
                ResultSpec.item("burger", 1)));

        cm.register(new CraftingRecipe("cheeseburger",
                List.of(Ingredient.material(Material.BREAD, "Brot"),
                        Ingredient.item("kaese", "Käse"),
                        Ingredient.material(Material.COOKED_BEEF, "Fleisch")),
                ResultSpec.item("cheeseburger", 1)));

        cm.register(new CraftingRecipe("chicken_nuggets",
                List.of(Ingredient.material(Material.COOKED_CHICKEN, "Gebratenes Hähnchen"),
                        Ingredient.tag(Tag.ITEMS_SWORDS, Material.IRON_SWORD, "Schwert")),
                ResultSpec.item("chicken_nuggets", 4)));

        cm.register(new CraftingRecipe("teig",
                List.of(Ingredient.material(Material.WHEAT, "Weizen"),
                        Ingredient.material(Material.WATER_BUCKET, "Wasser")),
                ResultSpec.item("teig", 1)));

        cm.register(new CraftingRecipe("schaschlik_karotte",
                List.of(Ingredient.material(Material.STICK, "Stock"),
                        Ingredient.material(Material.COOKED_BEEF, "Fleisch"),
                        Ingredient.material(Material.CARROT, "Karotte")),
                ResultSpec.item("schaschlik", 1)));

        cm.register(new CraftingRecipe("schaschlik_kartoffel",
                List.of(Ingredient.material(Material.STICK, "Stock"),
                        Ingredient.material(Material.COOKED_BEEF, "Fleisch"),
                        Ingredient.material(Material.POTATO, "Kartoffel")),
                ResultSpec.item("schaschlik", 1)));

        cm.register(new CraftingRecipe("ofenkartoffel_sourcream",
                List.of(Ingredient.material(Material.BAKED_POTATO, "Ofenkartoffel"),
                        Ingredient.material(Material.MILK_BUCKET, "Milch")),
                ResultSpec.item("ofenkartoffel_sourcream", 1)));

        cm.register(new CraftingRecipe("spaghetti",
                List.of(Ingredient.item("nudeln", "Nudeln"),
                        Ingredient.item("sauce", "Sauce"),
                        Ingredient.material(Material.BOWL, "Schüssel")),
                ResultSpec.item("spaghetti", 1)));

        cm.register(new CraftingRecipe("misosuppe",
                List.of(Ingredient.item("nudeln", "Nudeln"),
                        Ingredient.material(Material.CARROT, "Karotte"),
                        Ingredient.material(Material.KELP, "Seetang"),
                        Ingredient.material(Material.COD, "Fisch"),
                        Ingredient.material(Material.BOWL, "Schüssel")),
                ResultSpec.item("misosuppe", 1)));

        cm.register(new CraftingRecipe("kandierter_apfel",
                List.of(Ingredient.material(Material.APPLE, "Apfel"),
                        Ingredient.material(Material.SUGAR, "Zucker")),
                ResultSpec.item("kandierter_apfel", 1)));
    }
}
