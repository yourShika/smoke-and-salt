# 🍲 Smoke & Salt

> *A vanilla-faithful cooking expansion for Minecraft — crafted with fire, patience, and a pinch of magic.*

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62b47a?style=flat-square&logo=minecraft&logoColor=white)
![Paper](https://img.shields.io/badge/Platform-Paper%2FSpigot-f7a41d?style=flat-square)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)
![Status](https://img.shields.io/badge/Status-In%20Development-orange?style=flat-square)

</div>

---

## ✨ What is Smoke & Salt?

**Smoke & Salt** brings a deep, immersive cooking system to your server — without
breaking the vanilla feel. No bloated machines. Just a smoker, a campfire, a
cauldron and the right ingredients.

It ships with a **complete starter set of ingredients, dishes and recipes** — each
custom item with its own hand-drawn 16×16 vanilla-style texture (via Oraxen). More
content can be added any time through `config.yml`, no code required.

---

## 🔥 Stations

| Station | How it works |
|---------|--------------|
| 🪵 **Smoker** | Put an ingredient into a **fuelled smoker** — it smokes exactly like vanilla and pops out the result. |
| 🔥 **Campfire** | **Place** an ingredient on a **lit campfire** and wait until it's done — just like cooking meat. |
| 🫧 **Water Cauldron** | A cauldron over a **heat source** boils. Throw items in — they sink slightly into the water and cook. Multiple ingredients accumulate until a recipe matches. **Right-click the cauldron to cancel** and get your items back. |
| 🌋 **Lava Cauldron** | A careful frying/roasting station — only specific items work. |
| 🪓 **Cutting** | Axe in the main hand, ingredient in the off hand, right-click to cut (particles + sound). |
| ⛓️ **Chains** | A hanger for cauldron ware or smoked goods — right-click to hang/retrieve. |
| 🌱 **Custom Seeds** | Plantable on farmland; drops from grass, seagrass and composters. |

Smoker & campfire recipes are registered as **real vanilla recipes**, so they feel
completely native (and show up in the recipe book). A water cauldron only boils
when a **heat source** (campfire, fire, lava or magma block) sits directly beneath
it — then it visibly bubbles.

---

## 🍳 Bundled recipes

| Station | Ingredient(s) | Result |
|---------|---------------|--------|
| Smoker | Egg | Fried Egg |
| Smoker | Beetroot | Beetroot Chips |
| Smoker | Carrot | Roasted Carrot |
| Smoker | Potato | Fries |
| Smoker | Dough | Bread |
| Campfire | Sugar | Marshmallow |
| Campfire | Dough | Stick Bread |
| Cauldron | Dough *(+ water)* | Noodles |
| Cauldron | Milk | Cheese |
| Cauldron | Beetroot + Carrot *(+ water)* | Sauce |
| Crafting | Wheat + Water | Dough |
| Crafting | Bread + Meat | Burger |
| Crafting | Bread + Cheese + Meat | Cheeseburger |
| Crafting | Cooked Chicken + Sword | Chicken Nuggets |
| Crafting | Stick + Meat + Carrot/Potato | Shashlik |
| Crafting | Baked Potato + Milk | Baked Potato with Sour Cream |
| Crafting | Noodles + Sauce + Bowl | Spaghetti |
| Crafting | Noodles + Carrot + Kelp + Fish + Bowl | Miso Soup |
| Crafting | Apple + Sugar | Candy Apple |
| Seed | Break seagrass | Rice Seeds → plant on farmland → **Rice** |

Browse everything in-game with **`/sas recipes`** — click any result to see the
full process laid out in a GUI. Tool/bucket recipes keep the sword and return an
empty bucket.

---

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/sas` · `/sas help` | Overview GUI | `smokeandsalt.command.help` |
| `/sas recipes` | Recipe browser (GUI) | `smokeandsalt.command.recipes` |
| `/sas version` | Version, server, active hooks | `smokeandsalt.command.version` |
| `/sas give [player] <id> [amount]` | Give a custom item/seed (GUI without args) | `smokeandsalt.admin.give` |
| `/sas modules` | Manage external modules live (GUI) | `smokeandsalt.admin.modules` |
| `/sas assets [status\|redeploy]` | Oraxen assets: check / redeploy | `smokeandsalt.admin.assets` |
| `/sas update` | Download the latest GitHub release | `smokeandsalt.admin.update` |
| `/sas reload` | Reload config, messages, items, recipes, modules | `smokeandsalt.admin.reload` |

Aliases: `/smokeandsalt`, `/smokesalt`. Almost everything is reachable via **GUI**.

### Permissions

- `smokeandsalt.*` – everything (default: OP)
- `smokeandsalt.use` – use the cooking functions (default: on)
- `smokeandsalt.seed.plant` – plant custom seeds (default: on)
- `smokeandsalt.command.*` – public subcommands (default: on)
- `smokeandsalt.admin.*` – admin functions (default: OP)

---

## 🧩 Modules (hooks)

Toggle external hooks **live** via `/sas modules` (or `config.yml` under
`hooks.modules`). They enable automatically once the required plugin is installed —
if it's missing, the module stays silently inactive and the plugin runs fully
standalone.

| Module | Purpose | Required |
|--------|---------|----------|
| **Oraxen** | Custom models/textures for every item, seed and result | optional (needed for custom textures) |
| **PlaceholderAPI** | Placeholders `%sas_version%`, `%sas_items%`, `%sas_recipes%`, `%sas_active_cooks%` | optional |

### Oraxen assets

When the Oraxen module is active, the asset deployer places the bundled item YAMLs
and 16×16 textures into Oraxen — **versioned and with backups**. Status and redeploy
run via `/sas assets`. Details in
[`src/main/resources/oraxen/README.md`](src/main/resources/oraxen/README.md).

---

## ⚙️ Configuration

All content is defined via `config.yml`. The bundled starter content is built in;
config entries with the same id override it. You can add your own:

- `items:` – custom items (ingredients, tools, results)
- `seeds.definitions:` – custom seeds incl. growth & drop chances
- `recipes:` – cooking recipes per station

Global toggles per station, particles/sounds, world whitelist/blacklist and the
language (`en` / `de`) are configurable too. Default language is **English**.

---

## 📋 Requirements

- Paper or Spigot `26.1.2`
- Java `25+`
- *(optional)* Oraxen · PlaceholderAPI

---

## 🛠️ Building

```bash
mvn clean package
```

The finished plugin lands in `target/Smoke-and-Salt-<version>.jar`. GitHub Actions
build every push (`build.yml`) and publish a release on a `v*` tag
(`release.yml`) — which `/sas update` pulls from. Textures, Oraxen items and the
manifest are (re)generated by `scripts/generate_assets.py`.

---

## 🗺️ Roadmap

- [x] Cooking engine (stations, particles, timing, sequential)
- [x] Vanilla smoker & campfire recipes
- [x] Multi-ingredient cauldron with cancel + crafting system
- [x] Starter set of items, dishes & recipes with 16×16 textures
- [x] Custom seeds (rice from seagrass)
- [x] GUIs, commands, permissions, self-updater
- [ ] Food values & effects per dish
- [ ] Custom crop-block model for rice (needs Oraxen custom blocks)

---

## 📄 License

MIT — see [`LICENSE`](LICENSE).

---

<div align="center">
  <sub>Made with 🔥 and a wooden spoon · <i>Smoke & Salt</i></sub>
</div>
