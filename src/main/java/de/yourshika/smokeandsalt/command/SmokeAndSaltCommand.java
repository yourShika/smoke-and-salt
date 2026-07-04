package de.yourshika.smokeandsalt.command;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import de.yourshika.smokeandsalt.config.MessageManager;
import de.yourshika.smokeandsalt.gui.AssetsMenu;
import de.yourshika.smokeandsalt.gui.GiveMenu;
import de.yourshika.smokeandsalt.gui.HelpMenu;
import de.yourshika.smokeandsalt.gui.ModulesMenu;
import de.yourshika.smokeandsalt.gui.RecipesMenu;
import de.yourshika.smokeandsalt.module.Module;
import de.yourshika.smokeandsalt.module.OraxenAssetDeployer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static de.yourshika.smokeandsalt.config.MessageManager.ph;

/**
 * Verarbeitet {@code /sas} (Aliase: {@code /smokeandsalt}, {@code /smokesalt})
 * inklusive Tab-Completion. Setzt fuer jeden Unterbefehl die passende Permission
 * durch.
 */
public final class SmokeAndSaltCommand implements CommandExecutor, TabCompleter {

    private final SmokeAndSalt plugin;
    private final MessageManager msg;

    public SmokeAndSaltCommand(SmokeAndSalt plugin) {
        this.plugin = plugin;
        this.msg = plugin.messages();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> help(sender);
            case "recipes" -> recipes(sender);
            case "version" -> version(sender);
            case "give" -> give(sender, args);
            case "reload" -> reload(sender);
            case "modules" -> modules(sender);
            case "update" -> update(sender);
            case "assets" -> assets(sender, args);
            default -> msg.send(sender, "command.unknown", ph("sub", sub));
        }
        return true;
    }

    // --- help ---------------------------------------------------------------

    private void help(CommandSender sender) {
        if (!sender.hasPermission("smokeandsalt.command.help")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        if (sender instanceof Player player) {
            HelpMenu.open(plugin, player);
            return;
        }
        msg.sendRaw(sender, "help.header");
        msg.sendRaw(sender, "help.line-help");
        msg.sendRaw(sender, "help.line-recipes");
        msg.sendRaw(sender, "help.line-version");
        msg.sendRaw(sender, "help.line-give");
        msg.sendRaw(sender, "help.line-reload");
        msg.sendRaw(sender, "help.line-modules");
        msg.sendRaw(sender, "help.line-update");
        msg.sendRaw(sender, "help.line-assets");
    }

    // --- recipes ------------------------------------------------------------

    private void recipes(CommandSender sender) {
        if (!sender.hasPermission("smokeandsalt.command.recipes")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        if (sender instanceof Player player) {
            RecipesMenu.open(plugin, player);
            return;
        }
        int total = plugin.cooking().registry().size() + plugin.cauldron().size() + plugin.crafting().size();
        msg.send(sender, "recipes.count",
                ph("count", String.valueOf(total)),
                ph("station", String.valueOf(plugin.cooking().registry().size())),
                ph("cauldron", String.valueOf(plugin.cauldron().size())),
                ph("crafting", String.valueOf(plugin.crafting().size())));
    }

    // --- version ------------------------------------------------------------

    private void version(CommandSender sender) {
        if (!sender.hasPermission("smokeandsalt.command.version")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        msg.sendRaw(sender, "version.header");
        msg.sendRaw(sender, "version.plugin", ph("version", plugin.getPluginMeta().getVersion()));
        msg.sendRaw(sender, "version.server", ph("server", Bukkit.getVersion()));
        String hooks = plugin.moduleManager().modules().stream()
                .filter(Module::isActive).map(Module::displayName).reduce((a, b) -> a + ", " + b).orElse("-");
        msg.sendRaw(sender, "version.hooks", ph("hooks", hooks));
        int recipes = plugin.cooking().registry().size() + plugin.cauldron().size() + plugin.crafting().size();
        msg.sendRaw(sender, "version.stats",
                ph("items", String.valueOf(plugin.items().all().size())),
                ph("recipes", String.valueOf(recipes)),
                ph("seeds", String.valueOf(plugin.seeds().all().size())));
    }

    // --- give ---------------------------------------------------------------

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smokeandsalt.admin.give")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        // Ohne ID + Spieler-Kontext: GUI oeffnen.
        if (args.length < 2 && sender instanceof Player player) {
            GiveMenu.open(plugin, player);
            return;
        }
        if (args.length < 2) {
            msg.send(sender, "give.usage");
            return;
        }

        // Optionaler Ziel-Spieler an Position 1.
        Player target = Bukkit.getPlayerExact(args[1]);
        int idIndex = target != null ? 2 : 1;
        if (target == null && sender instanceof Player self) {
            target = self;
        }
        if (target == null) {
            msg.send(sender, "give.no-target");
            return;
        }
        if (args.length <= idIndex) {
            msg.send(sender, "give.usage");
            return;
        }
        String id = args[idIndex].toLowerCase(Locale.ROOT);
        int amount = 1;
        if (args.length > idIndex + 1) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(args[idIndex + 1])));
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack stack = plugin.items().create(id, amount);
        if (stack == null) stack = plugin.seeds().create(id, amount);
        if (stack == null) {
            msg.send(sender, "give.unknown", ph("item", id));
            return;
        }
        final Player recipient = target;
        var leftover = recipient.getInventory().addItem(stack);
        leftover.values().forEach(s -> recipient.getWorld().dropItemNaturally(recipient.getLocation(), s));
        msg.send(sender, "give.other",
                ph("amount", String.valueOf(amount)), ph("item", id), ph("player", target.getName()));
        if (!target.equals(sender)) {
            msg.send(target, "give.received", ph("amount", String.valueOf(amount)), ph("item", id));
        }
    }

    // --- reload -------------------------------------------------------------

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("smokeandsalt.admin.reload")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        plugin.reloadAll();
        msg.send(sender, "reload.done");
    }

    // --- modules ------------------------------------------------------------

    private void modules(CommandSender sender) {
        if (!sender.hasPermission("smokeandsalt.admin.modules")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        if (sender instanceof Player player) {
            ModulesMenu.open(plugin, player);
            return;
        }
        msg.sendRaw(sender, "modules.header");
        for (Module module : plugin.moduleManager().modules()) {
            msg.sendRaw(sender, "modules.line",
                    ph("name", module.displayName()),
                    ph("status", module.isActive() ? "AKTIV" : "inaktiv"),
                    ph("plugin", module.requiredPlugin()));
        }
    }

    // --- update -------------------------------------------------------------

    private void update(CommandSender sender) {
        if (!sender.hasPermission("smokeandsalt.admin.update")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        plugin.updater().checkAndUpdate(sender);
    }

    // --- assets -------------------------------------------------------------

    private void assets(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smokeandsalt.admin.assets")) {
            msg.send(sender, "command.no-permission");
            return;
        }
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "status";
        if (action.equals("redeploy")) {
            if (!plugin.moduleManager().isActive("oraxen")) {
                msg.send(sender, "assets.no-oraxen");
                return;
            }
            new OraxenAssetDeployer(plugin).deploy();
            plugin.resyncExternalAssets();
            msg.send(sender, "assets.redeployed");
            return;
        }
        if (sender instanceof Player player && action.equals("status") && args.length <= 1) {
            AssetsMenu.open(plugin, player);
            return;
        }
        OraxenAssetDeployer.AssetStatus status = new OraxenAssetDeployer(plugin).status();
        msg.sendRaw(sender, "assets.status-header");
        msg.sendRaw(sender, "assets.status-oraxen", ph("present", status.oraxenPresent() ? "ja" : "nein"));
        msg.sendRaw(sender, "assets.status-version",
                ph("bundled", status.bundledVersion()), ph("deployed", status.deployedVersion()));
        msg.sendRaw(sender, "assets.status-files",
                ph("managed", String.valueOf(status.managed())),
                ph("total", String.valueOf(status.total())),
                ph("missing", String.valueOf(status.missing())));
    }

    // --- tab completion -----------------------------------------------------

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            addIfPerm(sender, out, "help", "smokeandsalt.command.help");
            addIfPerm(sender, out, "recipes", "smokeandsalt.command.recipes");
            addIfPerm(sender, out, "version", "smokeandsalt.command.version");
            addIfPerm(sender, out, "give", "smokeandsalt.admin.give");
            addIfPerm(sender, out, "reload", "smokeandsalt.admin.reload");
            addIfPerm(sender, out, "modules", "smokeandsalt.admin.modules");
            addIfPerm(sender, out, "update", "smokeandsalt.admin.update");
            addIfPerm(sender, out, "assets", "smokeandsalt.admin.assets");
            return filter(out, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give") && sender.hasPermission("smokeandsalt.admin.give")) {
            if (args.length == 2) {
                Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                out.addAll(plugin.items().ids());
                out.addAll(plugin.seeds().ids());
            } else if (args.length == 3) {
                out.addAll(plugin.items().ids());
                out.addAll(plugin.seeds().ids());
                out.add("1");
                out.add("16");
            } else if (args.length == 4) {
                out.add("1");
                out.add("16");
                out.add("64");
            }
            return filter(out, args[args.length - 1]);
        }
        if (sub.equals("assets") && sender.hasPermission("smokeandsalt.admin.assets") && args.length == 2) {
            out.add("status");
            out.add("redeploy");
            return filter(out, args[1]);
        }
        return out;
    }

    private void addIfPerm(CommandSender sender, List<String> out, String value, String perm) {
        if (sender.hasPermission(perm)) out.add(value);
    }

    private List<String> filter(List<String> in, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        }
        return out;
    }
}
