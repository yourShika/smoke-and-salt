package de.yourshika.smokeandsalt.item;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.util.Text;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry aller Custom-Items. Standardmaessig leer - Zutaten und Gerichte werden
 * spaeter ueber die config.yml ({@code items:}) oder {@link #register(ItemDefinition)}
 * hinzugefuegt. Die Registry baut fertige {@link ItemStack}s inklusive
 * PDC-Markierung und (falls das Oraxen-Modul aktiv ist) Custom-Textur.
 */
public final class ItemRegistry {

    private final SmokeAndSalt plugin;
    private final ItemKeys keys;
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();
    private static final Key EAT_SOUND = Key.key("minecraft", "entity.generic.eat");

    public ItemRegistry(SmokeAndSalt plugin, ItemKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    /** Liest die Item-Definitionen aus der config.yml neu ein. */
    public void loadFromConfig() {
        definitions.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("items");
        int loaded = loadFromSection(root, false, "config.yml");
        if (loaded > 0) {
            plugin.getLogger().info("Custom-Items geladen: " + definitions.size());
        }
    }

    /** Liest Item-Definitionen aus einer beliebigen YAML-Section. */
    public int loadFromSection(ConfigurationSection root, boolean skipExisting, String source) {
        if (root == null) return 0;
        int loaded = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            if (skipExisting && contains(id)) continue;
            try {
                Material material = Material.matchMaterial(
                        sec.getString("material", "PAPER").toUpperCase(Locale.ROOT));
                if (material == null) {
                    plugin.getLogger().warning("Item '" + id + "': unbekanntes Material - uebersprungen.");
                    continue;
                }
                ItemDefinition def = new ItemDefinition(
                        id.toLowerCase(Locale.ROOT),
                        material,
                        sec.getString("display-name", id),
                        sec.getStringList("lore"),
                        sec.getString("provider-id", null),
                        sec.getBoolean("glow", false),
                        parseFood(sec));
                register(def);
                loaded++;
            } catch (Exception ex) {
                plugin.getLogger().warning(source + " Item '" + id
                        + "' konnte nicht geladen werden: " + ex.getMessage());
            }
        }
        return loaded;
    }

    private FoodProfile parseFood(ConfigurationSection itemSection) {
        ConfigurationSection food = itemSection.getConfigurationSection("food");
        if (food == null) return null;
        int nutrition = food.getInt("nutrition", 0);
        float saturation = (float) food.getDouble("saturation", 0.0);
        boolean always = food.getBoolean("can-always-eat", false);
        return nutrition > 0 ? new FoodProfile(nutrition, saturation, always, parseEffects(food)) : null;
    }

    private List<PotionEffect> parseEffects(ConfigurationSection food) {
        List<PotionEffect> effects = new ArrayList<>();
        for (Map<?, ?> raw : food.getMapList("effects")) {
            PotionEffect effect = parseEffect(null, raw);
            if (effect != null) effects.add(effect);
        }
        ConfigurationSection section = food.getConfigurationSection("effects");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection effectSection = section.getConfigurationSection(key);
                if (effectSection == null) continue;
                PotionEffect effect = parseEffect(key, effectSection);
                if (effect != null) effects.add(effect);
            }
        }
        return effects;
    }

    private PotionEffect parseEffect(String fallbackType, ConfigurationSection section) {
        String typeName = section.getString("type", fallbackType);
        PotionEffectType type = potionType(typeName);
        if (type == null) return null;
        int duration = section.getInt("duration-ticks", section.getInt("duration", 100));
        int amplifier = section.getInt("amplifier", 0);
        boolean ambient = section.getBoolean("ambient", true);
        boolean particles = section.getBoolean("particles", true);
        boolean icon = section.getBoolean("icon", true);
        return new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier),
                ambient, particles, icon);
    }

    private PotionEffect parseEffect(String fallbackType, Map<?, ?> raw) {
        String typeName = string(raw, "type", fallbackType);
        PotionEffectType type = potionType(typeName);
        if (type == null) return null;
        int duration = integer(raw, "duration-ticks", integer(raw, "duration", 100));
        int amplifier = integer(raw, "amplifier", 0);
        boolean ambient = bool(raw, "ambient", true);
        boolean particles = bool(raw, "particles", true);
        boolean icon = bool(raw, "icon", true);
        return new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier),
                ambient, particles, icon);
    }

    /**
     * Alte Bukkit-Effektnamen, die nicht dem Registry-Key entsprechen. Frueher
     * fing {@code PotionEffectType.getByName(..)} diese ab; die Methode ist
     * inzwischen deprecated, daher wird hier explizit uebersetzt, damit
     * bestehende Configs mit Legacy-Namen weiter funktionieren.
     */
    private static final Map<String, String> LEGACY_EFFECT_KEYS = Map.of(
            "slow", "slowness",
            "fast_digging", "haste",
            "slow_digging", "mining_fatigue",
            "increase_damage", "strength",
            "heal", "instant_health",
            "harm", "instant_damage",
            "jump", "jump_boost",
            "confusion", "nausea",
            "damage_resistance", "resistance");

    private PotionEffectType potionType(String name) {
        if (name == null || name.isBlank()) return null;
        // Ueber die Registry aufloesen (akzeptiert Keys wie "haste", "absorption",
        // "dolphins_grace"); Legacy-Namen werden vorher uebersetzt.
        String key = name.toLowerCase(Locale.ROOT).replace("minecraft:", "").trim();
        key = LEGACY_EFFECT_KEYS.getOrDefault(key, key);
        PotionEffectType type = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(key));
        if (type == null) {
            plugin.getLogger().warning("Unbekannter Food-Effekt: " + name);
        }
        return type;
    }

    private String string(Map<?, ?> raw, String key, String fallback) {
        Object value = raw.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private int integer(Map<?, ?> raw, String key, int fallback) {
        Object value = raw.get(key);
        if (value instanceof Number number) return number.intValue();
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private boolean bool(Map<?, ?> raw, String key, boolean fallback) {
        Object value = raw.get(key);
        if (value instanceof Boolean b) return b;
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    /** Registriert oder ueberschreibt eine Item-Definition. */
    public void register(ItemDefinition def) {
        definitions.put(def.id().toLowerCase(Locale.ROOT), def);
    }

    public ItemDefinition definition(String id) {
        return id == null ? null : definitions.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String id) {
        return id != null && definitions.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<ItemDefinition> all() {
        return definitions.values();
    }

    public List<String> ids() {
        return new ArrayList<>(definitions.keySet());
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    /** Baut einen fertigen ItemStack fuer die gegebene ID (oder {@code null}). */
    public ItemStack create(String id, int amount) {
        ItemDefinition def = definition(id);
        if (def == null) return null;
        ItemStack item = new ItemStack(def.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.line(def.displayName()));
            List<Component> lore = new ArrayList<>();
            for (String s : def.lore()) lore.add(Text.line(s));
            appendEffectLore(lore, def.food());
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.getPersistentDataContainer().set(keys.itemId, PersistentDataType.STRING, def.id());
            if (def.glow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        // Optionale Custom-Textur via aktivem Item-Modul (Oraxen).
        plugin.moduleManager().applyExternalModel(item, def.providerId());
        applyFood(item, def.food());
        return item;
    }

    /** Ergaenzt die Lore um eine lesbare Auflistung der Ess-Effekte. */
    private void appendEffectLore(List<Component> lore, FoodProfile food) {
        if (food == null || food.effects().isEmpty()) return;
        lore.add(Text.line(" "));
        lore.add(Text.line("<gray>When eaten:"));
        for (PotionEffect effect : food.effects()) {
            String name = prettyEffect(effect.getType());
            String level = roman(effect.getAmplifier() + 1);
            String seconds = String.valueOf(Math.round(effect.getDuration() / 20.0));
            lore.add(Text.line("<blue>• " + name + " " + level + " <dark_gray>(" + seconds + "s)"));
        }
    }

    private String prettyEffect(PotionEffectType type) {
        String key = type.getKey().getKey().replace('_', ' ');
        String[] parts = key.split(" ");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return out.toString().trim();
    }

    private String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    private void applyFood(ItemStack item, FoodProfile food) {
        if (food == null || food.nutrition() <= 0) return;

        item.setData(DataComponentTypes.FOOD, FoodProperties.food()
                .nutrition(food.nutrition())
                .saturation(food.saturation())
                .canAlwaysEat(food.canAlwaysEat()));

        var consumable = Consumable.consumable()
                .consumeSeconds(1.6f)
                .animation(ItemUseAnimation.EAT)
                .sound(EAT_SOUND)
                .hasConsumeParticles(true);
        if (!food.effects().isEmpty()) {
            consumable.effects(List.of(ConsumeEffect.applyStatusEffects(food.effects(), 1.0f)));
        }
        item.setData(DataComponentTypes.CONSUMABLE, consumable);
    }

    /** Liest die Custom-Item-ID aus einem Stack (oder {@code null}). */
    public String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(keys.itemId, PersistentDataType.STRING);
    }
}
