# Smoke & Salt

Smoke & Salt is a vanilla-faithful cooking expansion for Paper/Spigot servers.
It adds deeper food progression without large machines: smokers, campfires,
water cauldrons, lava cauldrons, cutting, hanging chains, custom seeds, recipe
GUIs, and optional Oraxen textures.

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62b47a?style=flat-square&logo=minecraft&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Paper%2FSpigot-f7a41d?style=flat-square)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Version](https://img.shields.io/badge/Version-1.0.1-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

## What's new in 1.0.1

- **Sodas require a real water bottle** — the three fruit sodas now only accept an actual (water-filled) bottle, so you can't waste a brewed potion on them.
- **Mushroom recipes accept both mushrooms** — Pizza takes red *or* brown mushrooms (matching Grilled Mushroom). New `materials: [...]` ingredient syntax for content files.
- **Recipe reference check** — on load, the plugin now logs a warning if a recipe points at a custom item that doesn't exist, so typos in content files surface immediately instead of failing silently.

## 1.0.0 — first stable release

A hardening pass focused on **anti-dupe and robustness** rounds out the feature set for the first stable release. Fixed in this release:

- **Cauldron GUI, multi-player**: two players opening the same cauldron now share **one** inventory instead of two divergent copies — closes a stale-view dupe/loss window.
- **Cauldron deposit vs. open GUI**: throwing an item onto a cauldron while its GUI is open no longer overwrites the player's un-synced edits (no item loss).
- **Floating cauldron ingredients** are now `ItemDisplay` entities — they can't be picked up or hoppered by anything, removing a whole class of "decoration is a real item" risk (they still show the ingredients, still non-persistent, no crash-orphans).
- **Break/unload ordering**: a broken or unloaded cauldron is removed from tracking *before* its GUI is closed, so its contents can't be both dropped and re-persisted (no restart dupe).
- **Wine & consumables**: effects apply at `MONITOR` priority, so nothing is granted if another plugin cancels the drink (no effect-farming).
- **Chains**: breaking a chain acts at `MONITOR` priority (no retrieve-on-cancelled-break); and a malformed `chains.yml` key can no longer kill the chain tick task.
- **Cutting**: the consumed ingredient is cleared cleanly at a stack of one (no lingering amount-0 slot).
- **`/sas give`**: the give action re-checks the admin permission on each click.

## What's new in 0.9.1

**Balance buffs** — these foods now give a bit more hunger/saturation and/or an effect: Garlic Bread (Haste), Grilled Cheese (Absorption), Garlic Mushrooms (Regeneration), the three Jam Breads (Speed), Chocolate (Speed), Onigiri (Regeneration), Popcorn, Candy Apple, Burger, Cheeseburger, Miso Soup, and Glittering Apple Slices (now Regeneration + Absorption, closer to a golden apple).

**Changes**
- **Wine** now gives **Nausea** and **stacking Luck** — each glass extends the Luck timer (up to a 10-minute cap), so the more you drink the luckier (and dizzier) you get.
- **Cheesecake with Berry Sauce** recipe simplified to **Dough + Sauce**.
- **Grilled Mushroom** now works with **every mushroom** (red *and* brown) on the campfire.
- Effect parsing made more robust (modern effect keys like `haste`, `absorption`, `nausea` resolve reliably).

## What's new in 0.9.0

**30+ new dishes across every station** (all with hand-made textures)

- **Cutting**: Apple Slices, Pineapple Slice, Steak Strips.
- **Smoker**: Baked Zucchini, Baked Eggplant, Popcorn, Melted Cheese — and **Dough now smokes into plain Bread**.
- **Campfire**: Grilled Corn, Grilled Pineapple, Grilled Mushroom.
- **Water cauldron**: Blueberry / Strawberry / Pineapple Jelly, Wine, Chocolate.
- **Lava cauldron**: Liquid Chocolate.
- **Crafting**: three Jam Breads, three fruit Sodas, Garlic Mushrooms, Glittering Apple Slices, Fruit Salad, S'mores, Chocolate-Covered Fruit, Grilled Cheese, Mac & Cheese, Garlic Bread, and a six-ingredient **Pizza**.

**Fixes & QoL**

- **Chain-hung items are now static** — they hang under the chain as a fixed display and no longer spin.
- **Cotton → textiles**: 1 Cotton → 2 String, 3 Cotton → 1 Wool. These and *Dough → Bread* appear under a new **Vanilla Outputs** category in `/sas recipes`.
- **`/sas give` is now paginated** (arrows at the bottom) so every item and seed is reachable.
- **Seeds drop from grass more reliably** — grass/leaf drop chances were raised.
- **Balance pass**: the strongest foods were toned down (Burger/Cheeseburger/Shashlik 8 hunger, shorter Strength; Baked Potato, Miso Soup and Candy Apple slightly reduced) so nothing is too strong or exploitable, and every new dish uses conservative hunger/saturation/effects.

## What's new in 0.8.1

- **Ingredients are shown floating in the cauldron** again — whatever sits in a water/lava cauldron's container now appears as items hovering in the liquid (non-persistent, no crash-orphans), on top of the GUI.
- **Empty cauldrons keep their contents and GUI**: when a water cauldron runs out of water it no longer closes the menu or loses items. The container persists on the empty cauldron, the GUI stays usable, and cooking simply resumes once you add water + heat. Contents only ever leave when you break the block (dropped safely above it, never burned).
- **Custom ingredients can no longer be eaten if they aren't food** — e.g. **Oil** is now a proper non-edible ingredient (base item changed to a non-consumable, plus a consume guard for any food-less custom item).

## What's new in 0.8.0

**Cauldron stations now have a GUI (furnace-style)**
- **Sneak + right-click** a water or lava cauldron to open a small station GUI. Drop items into the input slots on the left, watch the progress in the middle, and take finished dishes from the output slots on the right.
- The container is **persistent** (`water_cauldron_stations.yml` / `lava_cauldron_stations.yml`): whatever you load stays in the cauldron across chunk reloads and restarts.
- **Both input methods coexist**: throwing a matching ingredient onto the cauldron still works — it now lands in the same persistent container instead of floating on top.
- **Batch processing**: pack in whole stacks and the cauldron works through them recipe by recipe. The GUI header shows the **water level** (water cauldron) or frying heat (lava cauldron); the water cauldron still needs a heat source below.
- **Lava cauldron reworked to multi-ingredient**: it now uses the same container/recipe system as the water cauldron, enabling the new fried dishes below (multi-ingredient recipes are matched before single-ingredient ones, so *Oil + Potato* → Chips while a lone Potato → Fries).
- Breaking a cauldron **returns its whole container** instead of losing it.
- *Note:* the old "serve Miso Soup with bowls" mechanic is replaced by the GUI output; Miso Soup now finishes into the output slot and consumes 1 water level.

**12 new textured dishes**

| Item | Station | Recipe | Hunger | Saturation | Effect |
| --- | --- | --- | ---: | ---: | --- |
| Berry Cookies | Crafting | Wheat + Sweet Berries + Sugar | 4 | 3.2 | Speed I, 4s |
| Cheesecake with Berry Sauce | Crafting | Dough + Sweet Berries + Sugar + Milk | 8 | 9.6 | Regeneration I, 4s |
| Sushi | Crafting | Rice + Fish | 6 | 7.2 | Dolphin's Grace I, 5s |
| Sakura Sushi | Crafting | Rice + Cherry Blossom (Pink Petals) | 5 | 6.0 | Luck I, 8s |
| Onigiri | Crafting | Rice + Meat + Dried Kelp | 7 | 8.4 | – |
| Apple Juice | Water Cauldron | Apple (+ water) | 3 | 2.4 | Regeneration I, 3s |
| Cherry Lemonade | Water Cauldron | Cherry Blossom + Sugar (+ water) | 3 | 2.0 | Speed I, 5s |
| Oil | Water Cauldron | Sunflower (+ water) | 0 | 0.0 | not edible – ingredient only |
| Calamari Rings | Lava Cauldron | Oil + Ink Sac | 6 | 7.2 | Water Breathing I, 5s |
| Chips | Lava Cauldron | Oil + Potato | 5 | 5.2 | – |
| Creeper Cookie | Lava Cauldron | Oil + Dough + Gunpowder | 5 | 3.0 | Speed I, 4s + harmless explosion puff on eat |
| Lard Pastry | Lava Cauldron | Oil + Dough + Sugar | 6 | 6.4 | Speed I, 4s |

All twelve ship with their own 16×16 Oraxen textures. **Oil** is a new intermediate made in the water cauldron and used by every lava-cauldron fry recipe.

**Texture overhaul**
- All existing dishes, produce and seeds now use hand-made 16×16 sprites (source art in `textures/`, wired into `scripts/generate_assets.py` as overrides). The generator still falls back to its procedural art for anything without an override (currently the 12 new dishes and the crop overlays).

## What's new in 0.7.0

**Robustness / anti-dupe**
- Cauldron float items and chain-hung items are no longer persistent → **no more crash-orphans**; a startup/chunk-load sweep also removes legacy leftovers.
- **Chains now persist** (`chains.yml`) and re-spawn per chunk; **breaking a chain returns the hung item** instead of losing it.
- Cauldron tick **skips unloaded chunks** (no force-load, no item loss).
- Crafting tool/bucket return is guarded against late craft cancellation (no free sword/bucket).
- Seed and leaf drops no longer trigger in **Creative** or with **Silk Touch**.
- **Content files auto-update** on version bumps (old files backed up) — fixes like the name bug and seed drops now reach existing servers automatically.

**Cutting rework**
- You must **keep attacking** (arm swing) for the whole duration; stopping aborts. Same length, with a live **progress bar** in the action bar. Randomized yield + tool wear stay, plus a stricter anti-dupe re-check.

**QoL**
- Admins get an **update notice on join** (`notify-updates`).
- Right-click a **composter with a custom produce** for a chance at its seed (`seeds.compost-seed-chance`).

## What's new in 0.6.0

- **Seed drops**: every seed can now drop from configured blocks in specific biomes with a chance (`drops-from` / `biomes` / `chance`), e.g. Rice from seagrass in rivers/swamps, Tomato from grass in plains/forest, Grapes from jungle/oak/birch leaves. Shown in `/sas recipes` under *Seeds*.
- **Name bug fixed**: new seeds/produce showed the raw hex code instead of a colored name — now proper MiniMessage colors.
- **Crops default to vanilla wheat again** (`seeds.custom-crops: false`): performant and **breakable by hand** (drops the produce + seeds). Right-click a crop to see which plant it is (`seeds.crop-identify`). The plugin-driven per-plant display is still available with `seeds.custom-crops: true`.

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
git tag v0.7.0
git push origin v0.7.0
```

## License

MIT, see `LICENSE`.
