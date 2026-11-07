package dev.silent.silentui.command;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.hud.HudInstance;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiRegistry;
import dev.silent.silentui.render.ScreenSession;
import dev.silent.silentui.render.WorldUi;
import dev.silent.silentui.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** /silentui (alias /sui) */
public final class SuiCommand implements TabExecutor {

    private final SilentUI plugin;

    public SuiCommand(SilentUI plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String mm) {
        s.sendMessage(Text.mm("<#ff8fd1><bold>Silent UI</bold></#ff8fd1> <dark_gray>»</dark_gray> " + mm));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "open" -> open(sender, args);
            case "close" -> close(sender, args);
            case "hud" -> hud(sender, args);
            case "world" -> world(sender, args);
            case "anim" -> anim(sender, args);
            case "edit" -> edit(sender, args);
            case "bind" -> bind(sender, args, false);
            case "unbind" -> bind(sender, args, true);
            case "import" -> importCmd(sender, args);
            case "ed" -> ed(sender, args);
            case "var" -> var(sender, args);
            case "reload" -> {
                if (!admin(sender)) return true;
                String err = plugin.reloadAll();
                if (err == null) msg(sender, "<green>Reloaded " + plugin.registry().all().size() + " UI(s) and rebuilt the resource pack.");
                else msg(sender, "<red>Reload failed: " + err);
            }
            case "list" -> {
                if (!admin(sender)) return true;
                msg(sender, "<gray>Loaded UIs:");
                for (UiDefinition d : plugin.registry().all()) {
                    sender.sendMessage(Text.mm("  <#ff8fd1>" + d.id + "</#ff8fd1> <dark_gray>(" + d.type + ")</dark_gray> <gray>"
                            + d.elements.size() + " elements, " + d.sprites.size() + " sprites"
                            + (d.settings.command.isEmpty() ? "" : ", /" + d.settings.command)));
                }
            }
            case "pack" -> {
                if (!admin(sender)) return true;
                List<Player> targets = args.length > 1 ? playerArg(sender, args[1]) : new ArrayList<>(Bukkit.getOnlinePlayers());
                for (Player p : targets) plugin.sendPack(p);
                msg(sender, "<green>Sent the pack to " + targets.size() + " player(s). <gray>Zip: plugins/SilentUI/output/SilentUI-pack.zip");
            }
            case "editor" -> {
                if (!admin(sender)) return true;
                String url = plugin.editorUrl();
                if (url == null) {
                    msg(sender, "<red>The web server is disabled (web.enabled in config.yml).");
                } else {
                    msg(sender, "<gray>Editor: <click:open_url:'" + url + "'><u><#8fd1ff>" + url + "</#8fd1ff></u></click>");
                    msg(sender, "<gray>Use <white>Push to server</white> with the token in <white>config.yml → web.upload-token</white>.");
                }
            }
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender s) {
        msg(s, "<gray>Commands:");
        String[] lines = {
                "/sui open <ui> [player] <dark_gray>- open a screen UI or show a HUD",
                "/sui close [player] <dark_gray>- close the open screen UI",
                "/sui hud <show|hide|toggle> <ui> [player]",
                "/sui world place <ui> [name] <dark_gray>- place a world UI in front of you",
                "/sui world move <name> | remove <name> | list",
                "/sui bind <ui> | unbind <dark_gray>- right clicking the block you look at opens the UI",
                "/sui import decentholograms <dark_gray>- turn DecentHolograms into Silent UI holograms",
                "/sui edit <ui> <dark_gray>- edit a UI in game",
                "/sui anim <animation> [player] <dark_gray>- play an animation on the open UI",
                "/sui var <player> <name> <value> <dark_gray>- set a script variable",
                "/sui reload | list | pack [player] | editor"
        };
        for (String l : lines) s.sendMessage(Text.mm("  <#ff8fd1>" + l));
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("silentui.admin")) return true;
        s.sendMessage(plugin.message("no-permission"));
        return false;
    }

    private List<Player> playerArg(CommandSender sender, String arg) {
        if (arg.equals("@a")) return new ArrayList<>(Bukkit.getOnlinePlayers());
        Player p = Bukkit.getPlayerExact(arg);
        if (p == null) {
            msg(sender, "<red>Player not found: " + arg);
            return List.of();
        }
        return List.of(p);
    }

    private List<Player> targets(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            if (!sender.hasPermission("silentui.open.others") && !sender.hasPermission("silentui.admin")) {
                sender.sendMessage(plugin.message("no-permission"));
                return List.of();
            }
            return playerArg(sender, args[index]);
        }
        if (sender instanceof Player p) return List.of(p);
        msg(sender, "<red>Specify a player.");
        return List.of();
    }

    private void open(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "<red>Usage: /sui open <ui> [player]");
            return;
        }
        UiDefinition def = plugin.registry().get(args[1]);
        if (def == null) {
            msg(sender, "<red>Unknown UI: " + args[1]);
            return;
        }
        if (def.isWorld()) {
            msg(sender, "<red>" + def.id + " is a world UI. Use /sui world place " + def.id);
            return;
        }
        boolean allowed = sender.hasPermission("silentui.admin") || sender.hasPermission("silentui.open.*")
                || sender.hasPermission("silentui.open." + def.id)
                || (!def.settings.permission.isEmpty() && sender.hasPermission(def.settings.permission));
        if (!allowed) {
            sender.sendMessage(plugin.message("no-permission"));
            return;
        }
        for (Player p : targets(sender, args, 2)) plugin.manager().openAny(p, def.id);
    }

    private void close(CommandSender sender, String[] args) {
        for (Player p : targets(sender, args, 1)) plugin.manager().closeScreen(p);
    }

    private void hud(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 3) {
            msg(sender, "<red>Usage: /sui hud <show|hide|toggle> <ui> [player]");
            return;
        }
        UiDefinition def = plugin.registry().get(args[2]);
        if (def == null || !def.isHud()) {
            msg(sender, "<red>Unknown HUD: " + args[2]);
            return;
        }
        for (Player p : targets(sender, args, 3)) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "show" -> plugin.hud().show(p, def.id);
                case "hide" -> plugin.hud().hide(p, def.id);
                default -> plugin.hud().toggle(p, def.id);
            }
        }
    }

    private void world(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (action) {
            case "place" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "<red>Players only.");
                    return;
                }
                if (args.length < 3) {
                    msg(sender, "<red>Usage: /sui world place <ui> [name]");
                    return;
                }
                UiDefinition def = plugin.registry().get(args[2]);
                if (def == null || !def.isWorld()) {
                    msg(sender, "<red>Unknown world UI: " + args[2] + " <gray>(set the UI type to World in the editor)");
                    return;
                }
                String name = args.length > 3 ? UiRegistry.cleanId(args[3]) : def.id;
                Location origin = placementFor(p, def);
                plugin.manager().placeWorld(name, def, origin, p.getEyeLocation().getYaw() + 180f);
                msg(sender, "<green>Placed <white>" + name + "</white>. <gray>Move it with /sui world move " + name);
            }
            case "move" -> {
                if (!(sender instanceof Player p) || args.length < 3) {
                    msg(sender, "<red>Usage: /sui world move <name>");
                    return;
                }
                WorldUi w = plugin.manager().worlds().get(args[2]);
                if (w == null) {
                    msg(sender, "<red>No world UI named " + args[2]);
                    return;
                }
                plugin.manager().placeWorld(args[2], w.def(), placementFor(p, w.def()), p.getEyeLocation().getYaw() + 180f);
                msg(sender, "<green>Moved " + args[2] + ".");
            }
            case "remove" -> {
                if (args.length < 3) {
                    msg(sender, "<red>Usage: /sui world remove <name>");
                    return;
                }
                boolean ok = plugin.manager().removeWorld(args[2], true);
                msg(sender, ok ? "<green>Removed " + args[2] : "<red>No world UI named " + args[2]);
            }
            default -> {
                msg(sender, "<gray>World UIs:");
                for (WorldUi w : plugin.manager().worlds().values()) {
                    Location o = w.origin();
                    sender.sendMessage(Text.mm("  <#ff8fd1>" + w.placementId() + "</#ff8fd1> <gray>" + w.def().id + " @ "
                            + o.getWorld().getName() + " " + o.getBlockX() + " " + o.getBlockY() + " " + o.getBlockZ()));
                }
            }
        }
    }

    /**
     * Where a world UI goes when placed: holograms stand where you stand, floor UIs lie at your feet,
     * flat boards float in front of your eyes.
     */
    static Location placementFor(Player p, UiDefinition def) {
        double height = def.canvas.height * def.settings.worldScale / 100.0;
        Location origin;
        if (Math.abs(def.settings.tilt) >= 45) {
            origin = p.getLocation().clone().add(0, def.settings.tilt > 0 ? 0.02 : 2.9, 0);
        } else if (!"fixed".equalsIgnoreCase(def.settings.billboard)) {
            origin = p.getLocation().clone().add(0, height / 2 + 0.3, 0);
        } else {
            Location eye = p.getEyeLocation();
            double yaw = Math.toRadians(eye.getYaw());
            Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
            origin = eye.clone().add(forward.multiply(Math.max(1.5, height * 0.9 + 1)));
        }
        origin.setYaw(0);
        origin.setPitch(0);
        return origin;
    }

    private void bind(CommandSender sender, String[] args, boolean unbind) {
        if (!admin(sender)) return;
        if (!(sender instanceof Player p)) {
            msg(sender, "<red>Players only.");
            return;
        }
        org.bukkit.block.Block b = p.getTargetBlockExact(6);
        if (b == null || b.getType().isAir()) {
            msg(sender, "<red>Look at a block (chest, barrel, sign, anything) within 6 blocks.");
            return;
        }
        if (unbind) {
            String was = plugin.manager().boundUi(b);
            plugin.manager().bind(b, null);
            msg(sender, was == null ? "<gray>That block had no UI." : "<green>Unbound " + was + " from that block.");
            return;
        }
        if (args.length < 2) {
            msg(sender, "<red>Usage: /sui bind <ui>  (while looking at a block)");
            return;
        }
        UiDefinition def = plugin.registry().get(args[1]);
        if (def == null || def.isWorld() || def.isLogic()) {
            msg(sender, "<red>Pick a chest GUI, screen UI or HUD: " + args[1]);
            return;
        }
        plugin.manager().bind(b, def.id);
        msg(sender, "<green>Right clicking that " + b.getType().getKey().getKey().replace('_', ' ') + " now opens <white>" + def.id
                + "</white>. <gray>Sneak + right click (admins) still opens the real block.");
    }

    /** /sui import decentholograms : converts every DecentHolograms hologram into a Silent UI hologram. */
    private void importCmd(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        String from = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (!from.equals("decentholograms") && !from.equals("dh")) {
            msg(sender, "<red>Usage: /sui import decentholograms [replace]");
            return;
        }
        boolean replace = args.length > 2 && args[2].equalsIgnoreCase("replace");
        java.nio.file.Path folder = plugin.getDataFolder().toPath().getParent().resolve("DecentHolograms").resolve("holograms");
        java.util.List<String> problems = new java.util.ArrayList<>();
        java.util.List<dev.silent.silentui.util.DecentHologramsImporter.Imported> list;
        try {
            list = dev.silent.silentui.util.DecentHologramsImporter.load(folder, problems);
        } catch (java.io.IOException e) {
            msg(sender, "<red>Could not read " + folder + ": " + e.getMessage());
            return;
        }
        if (list.isEmpty() && problems.isEmpty()) {
            msg(sender, "<red>No holograms found in plugins/DecentHolograms/holograms");
            return;
        }
        int made = 0, skipped = 0;
        for (var i : list) {
            if (plugin.registry().get(i.def().id) != null && !replace) {
                skipped++;
                continue;
            }
            try {
                plugin.registry().save(i.def());
            } catch (Exception e) {
                problems.add(i.def().id + ": " + e.getMessage());
                continue;
            }
            plugin.manager().placeWorld(i.def().id, i.def(), i.origin(), 0f);
            made++;
        }
        if (made > 0) {
            String err = plugin.reloadAll(); // loads the new files and re-places everything from disk
            if (err != null) problems.add("reload: " + err);
        }
        msg(sender, "<green>Imported " + made + " hologram" + (made == 1 ? "" : "s") + "."
                + (skipped > 0 ? " <gray>" + skipped + " already existed (add <white>replace</white> to overwrite)." : "")
                + " <gray>Open them in /sui editor to animate them.");
        for (String pr : problems) msg(sender, "<yellow>  " + pr);
        if (made > 0) msg(sender, "<gray>Tip: disable DecentHolograms (or delete its holograms) so they don't show twice.");
    }

    private void anim(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 2) {
            msg(sender, "<red>Usage: /sui anim <animation> [player]");
            return;
        }
        for (Player p : targets(sender, args, 2)) {
            ScreenSession s = plugin.manager().screen(p);
            if (s != null) s.playAnimation(args[1]);
            for (String id : plugin.hud().shown(p)) {
                HudInstance h = plugin.hud().instance(p, id);
                if (h != null) h.playAnimation(args[1]);
            }
        }
    }

    private void edit(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (!(sender instanceof Player p)) {
            msg(sender, "<red>Players only.");
            return;
        }
        if (args.length < 2) {
            msg(sender, "<red>Usage: /sui edit <ui>");
            return;
        }
        UiDefinition def = plugin.registry().get(args[1]);
        if (def == null) {
            msg(sender, "<red>Unknown UI: " + args[1]);
            return;
        }
        if (def.isLogic()) {
            msg(sender, "<red>" + def.id + " is a logic file (commands and functions only). Edit it in the web editor.");
            return;
        }
        plugin.manager().startEditing(p, def);
    }

    /** /sui ed set <prop> <value...> (used by the editor's chat fallback) */
    private void ed(CommandSender sender, String[] args) {
        if (!admin(sender) || !(sender instanceof Player p)) return;
        dev.silent.silentui.render.EditController ctrl = plugin.manager().editor(p);
        if (ctrl == null) {
            msg(sender, "<red>You are not editing a UI. Use /sui edit <ui>.");
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("set")) {
            ctrl.setFromCommand(args[2], args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "");
        } else {
            msg(sender, "<red>Usage: /sui ed set <property> <value>");
        }
    }

    private void var(CommandSender sender, String[] args) {
        if (!admin(sender)) return;
        if (args.length < 4) {
            msg(sender, "<red>Usage: /sui var <player> <name> <value>");
            return;
        }
        for (Player p : playerArg(sender, args[1])) {
            plugin.vars(p).put(args[2], String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("open", "close", "edit", "bind", "unbind", "import", "hud", "world", "anim", "var", "reload", "list", "pack", "editor", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "open" -> plugin.registry().all().stream().filter(d -> !d.isWorld() && !d.isLogic()).forEach(d -> out.add(d.id));
                case "edit" -> plugin.registry().all().stream().filter(d -> !d.isLogic()).forEach(d -> out.add(d.id));
                case "hud" -> out.addAll(List.of("show", "hide", "toggle"));
                case "world" -> out.addAll(List.of("place", "move", "remove", "list"));
                case "bind" -> plugin.registry().all().stream().filter(d -> !d.isWorld() && !d.isLogic()).forEach(d -> out.add(d.id));
                case "import" -> out.add("decentholograms");
                case "close", "pack", "var" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                case "anim" -> {
                    if (sender instanceof Player p && plugin.manager().screen(p) != null) {
                        plugin.manager().screen(p).def().animations.forEach(a -> out.add(a.name));
                    }
                }
                default -> { }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "hud" -> plugin.registry().all().stream().filter(UiDefinition::isHud).forEach(d -> out.add(d.id));
                case "world" -> {
                    if (args[1].equalsIgnoreCase("place")) plugin.registry().all().stream().filter(UiDefinition::isWorld).forEach(d -> out.add(d.id));
                    else out.addAll(plugin.manager().worlds().keySet());
                }
                case "open", "anim" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> { }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("hud")) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }
}
