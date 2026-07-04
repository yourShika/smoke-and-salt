# Smoke & Salt

Smoke & Salt is a vanilla-faithful cooking expansion for Paper/Spigot servers.
It adds deeper food progression without large machines: smokers, campfires,
water cauldrons, lava cauldrons, cutting, hanging chains, custom seeds, recipe
GUIs, and optional Oraxen textures.

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62b47a?style=flat-square&logo=minecraft&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Paper%2FSpigot-f7a41d?style=flat-square)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Version](https://img.shields.io/badge/Version-0.5.0-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

## What's new in 0.5.0

- **Custom crop system**: seeds are planted on farmland with **no wheat block** — each plant grows as its **own textured crop** (a plugin-driven display), so there is no more double texture and no impact on vanilla wheat farming. Growth is plugin-driven (light-gated, bone-mealable); right-click the crop to harvest when ripe (it regrows), break the farmland to remove it. All 19 plants (rice + 18) have their own crop texture. Configurable and toggleable under `seeds.custom-crops` (fall back to plain wheat with `false`).

## What's new in 0.4.0

- **18 new plants** to grow: Tomato, Onion, Lettuce, Corn, Cucumber, Garlic, Chili, Strawberry, Blueberry, Soybean, Cotton, Cabbage, Bell Pepper, Pineapple, Grapes, Coffee Beans, Zucchini, Eggplant — each with its own 16x16 texture, a tinted seed, and food values. Seeds grow like wheat on farmland and drop the produce (+ seeds) when harvested.
- **Lava cauldron fix**: finished results no longer burn in the lava — they drop fire-proof onto the cauldron.
- **Effect lore**: foods that grant an effect now list it in the item lore (e.g. `Strength I (5s)`).
- **No more double crop texture**: the hovering custom crop model is off by default (`seeds.crop-display`), so planted crops no longer show wheat *and* a floating model at once.

## What's new in 0.3.x

### 0.3.2

- All bundled dishes use the balanced hunger/saturation/effect values (e.g. Burger 9/11.5 + Strength I, Miso Soup 7/8.4 + Regeneration I).
- Cauldron results no longer burn in the lava/fire below - they drop fire-proof onto the cauldron.
- Cauldron works as a queue: drop a whole stack and it is processed batch after batch; right-click still cancels and returns everything.
- Cutting takes a random 1-5 durability (configurable) and yields a random amount (Chicken Nuggets 1-4).
- `/sas recipes` gains cross-cutting **Finished Dishes** and **Ingredients** categories on top of the per-station ones.
- New optional **leaf-drops** function (config `leaf-drops.drops`) - drops items/seeds when breaking leaves.

### 0.3.1

- Rice now shows a full 8-stage growth (`reis_stage0..7`) mapped onto wheat's ages, instead of only 4 stages.
- Burger and cheeseburger textures reworked with a rounded, domed silhouette instead of a hard rectangle.

### 0.3.0

- All bundled dishes now use real food data for hunger and saturation.
- Fried Egg is no longer throwable and is now a proper edible custom item.
- Fries moved from the smoker to the lava cauldron.
- Dough is made in a boiling water cauldron from 3 wheat and consumes 1 water level.
- Vanilla bread crafting is disabled so dough and Kaiser Rolls carry the bread progression.
- Stick Bread was replaced with a small Kaiser Roll.
- Chicken Nuggets are now made with the cutting interaction.
- Sour Cream was added as an ingredient and replaces direct milk in the baked potato recipe.
- Miso Soup is cooked in the water cauldron and served with bowls until the cauldron is empty.
- `/sas recipes` is split into categories and now includes seed acquisition/farming info.
- Shashlik appears as one recipe entry with variant navigation.
- Oraxen textures were refreshed with improved 16x16 vanilla-style sprites.
- Lava cauldron ingredients are protected from burning while the recipe starts.
- Cutting now works with the axe and ingredient in either hand and uses recipe duration.
- Custom items are blocked from vanilla side effects and unrelated crafting recipes.
- Default content is split into editable `content/*.yml` files, including `cutting.yml`.
- Rice crops use bundled Oraxen display-stage textures when planted on farmland.

## Stations

| Station | How it works |
| --- | --- |
| Smoker | Vanilla smoking recipes for simple ingredients such as egg, beetroot, and carrot. |
| Campfire | Place ingredients on a lit campfire for sweet or baked campfire food. |
| Water Cauldron | A water cauldron above a heat source boils, accepts ingredients, and can consume water levels. |
| Lava Cauldron | Used for frying/roasting recipes such as fries. |
| Cutting | Hold an axe in either hand and the ingredient in the other hand, then right-click. |
| Chains | Right-click chains to hang or retrieve food items. |
| Custom Seeds | Rice seeds drop from seagrass, can be planted on farmland, and show custom crop stages when Oraxen is active. |

Water cauldrons boil only when a heat source is directly beneath them. Supported
heat sources include campfires, fire, lava, and magma blocks.

## Bundled content

### Smoker

| Input | Result |
| --- | --- |
| Egg | Fried Egg |
| Beetroot | Beetroot Chips |
| Carrot | Roasted Carrot |

### Campfire

| Input | Result |
| --- | --- |
| Sugar | Marshmallow |
| Dough | Kaiser Roll |

### Water cauldron

| Input | Result |
| --- | --- |
| 3 Wheat | Dough, consumes 1 water level |
| Dough | Noodles |
| Milk Bucket | Cheese, returns bucket |
| Beetroot + Carrot | Sauce |
| Noodles + Carrot + Kelp + Cod | Miso Soup, served with bowls |

### Lava cauldron

| Input | Result |
| --- | --- |
| Potato | Fries |

### Cutting

| Input | Result |
| --- | --- |
| Cooked Chicken | Chicken Nuggets, after a short cutting action |

### Crafting

| Input | Result |
| --- | --- |
| Kaiser Roll + Cooked Beef | Burger |
| Kaiser Roll + Cheese + Cooked Beef | Cheeseburger |
| Milk Bucket + Sweet Berries + Bowl | Sour Cream |
| Stick + Cooked Beef + Carrot | Shashlik |
| Stick + Cooked Beef + Potato | Shashlik |
| Baked Potato + Sour Cream | Baked Potato with Sour Cream |
| Noodles + Sauce + Bowl | Spaghetti |
| Apple + Sugar | Candy Apple |

### Seeds

Rice Seeds drop from seagrass, can be planted on farmland, and grow into Rice.
When Oraxen is active, planted Rice gets a small custom crop-stage display using
the bundled `sas/crops/reis_stage*.png` textures. The recipe browser shows this
path under the Seeds category.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/sas` or `/sas help` | Open the overview GUI | `smokeandsalt.command.help` |
| `/sas recipes` | Open the categorized recipe browser | `smokeandsalt.command.recipes` |
| `/sas version` | Show version, server, hooks, and stats | `smokeandsalt.command.version` |
| `/sas give [player] <id> [amount]` | Give a custom item or seed | `smokeandsalt.admin.give` |
| `/sas modules` | Manage optional hooks live | `smokeandsalt.admin.modules` |
| `/sas assets [status|redeploy]` | Check or redeploy Oraxen assets | `smokeandsalt.admin.assets` |
| `/sas update` | Download the latest GitHub release | `smokeandsalt.admin.update` |
| `/sas reload` | Reload config, messages, items, recipes, and modules | `smokeandsalt.admin.reload` |

Aliases: `/smokeandsalt`, `/smokesalt`.

## Permissions

- `smokeandsalt.*` - all permissions, default OP.
- `smokeandsalt.use` - use gameplay systems, default true.
- `smokeandsalt.seed.plant` - plant custom seeds, default true.
- `smokeandsalt.command.*` - public commands, default true.
- `smokeandsalt.admin.*` - admin commands, default OP.

## Optional hooks

| Hook | Purpose |
| --- | --- |
| Oraxen | Custom models and textures for bundled items and seeds. |
| PlaceholderAPI | `%sas_version%`, `%sas_items%`, `%sas_recipes%`, `%sas_active_cooks%`. |

Oraxen assets are bundled in `src/main/resources/oraxen`. When the Oraxen hook is
active, Smoke & Salt deploys managed item YAML and textures with backups and a
versioned manifest. Run `/sas assets status` or `/sas assets redeploy` in-game.

## Configuration

The bundled starter content is built in. Server owners can add or override
content through `config.yml`:

- `items` for custom items, display names, lore, provider IDs, glow, and food data.
- `seeds.definitions` for custom seeds and crop drops.
- `recipes` for simple station recipes.
- `cooking.stations` for enabling/disabling stations.
- `worlds.whitelist` and `worlds.blacklist` for world restrictions.
- `hooks.modules` for optional integrations.

Custom item food values use `nutrition` for half-hunger icons and `saturation`
for the actual saturation value.

For easier editing, Smoke & Salt also creates these files on first run:
`content/items.yml`, `content/seeds.yml`, `content/smoker.yml`,
`content/campfire.yml`, `content/water_cauldron.yml`,
`content/lava_cauldron.yml`, `content/cutting.yml`, and
`content/crafting.yml`. Station files can define local `items` plus their
recipes, so a new cut food can live entirely in `content/cutting.yml`.

## Requirements

- Paper or Spigot compatible with Minecraft `26.1.2`
- Java `25+`
- Optional: Oraxen, PlaceholderAPI

## Building

```bash
mvn clean package
```

The plugin jar is written to `target/Smoke-and-Salt-<version>.jar`.

## Release

Pushing a tag named `v*` triggers the GitHub Actions release workflow. The action
builds the plugin and publishes the jar as a GitHub Release asset.

```bash
git tag v0.5.0
git push origin v0.5.0
```

## License

MIT, see `LICENSE`.
