package dev.silent.silentui.command;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.hud.HudInstance;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ArgDef;
import dev.silent.silentui.model.UiDefinition.CommandDef;
import dev.silent.silentui.model.UiDefinition.NodeDef;
import dev.silent.silentui.render.ScreenSession;
import dev.silent.silentui.render.WorldUi;
import dev.silent.silentui.script.UiInstance;
import dev.silent.silentui.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registers commands declared by UIs:
 * <ul>
 *   <li>settings.command: opens the UI</li>
 *   <li>"commands" list: custom commands with typed arguments, tab completion and usage messages</li>
 *   <li>"When command is used" script nodes</li>
 * </ul>
 * Arguments reach scripts as {arg:name}, {args} and {arg1}..{arg9}.
 */
public final class DynamicCommands {

    private final SilentUI plugin;
    private final List<Command> registered = new ArrayList<>();

    public DynamicCommands(SilentUI plugin) {
        this.plugin = plugin;
    }

    private record Binding(UiDefinition def, NodeDef node, CommandDef cmd) {}

    public void registerAll() {
        unregisterAll();
        Map<String, List<Binding>> bindings = new LinkedHashMap<>();
        Map<String, CommandDef> specs = new HashMap<>();
        Map<String, List<String>> aliases = new HashMap<>();
        for (UiDefinition def : plugin.registry().all()) {
            String cmd = clean(def.settings.command);
            if (!cmd.isEmpty()) bindings.computeIfAbsent(cmd, k -> new ArrayList<>()).add(new Binding(def, null, null));
            for (CommandDef c : def.commands) {
                String name = clean(c.name);
                if (name.isEmpty()) continue;
                bindings.computeIfAbsent(name, k -> new ArrayList<>()).add(new Binding(def, null, c));
                specs.putIfAbsent(name, c);
                List<String> al = aliases.computeIfAbsent(name, k -> new ArrayList<>());
                for (String a : c.aliases) if (!clean(a).isEmpty()) al.add(clean(a));
            }
            for (NodeDef n : def.script.nodes) {
                if (!"on_command".equals(n.type)) continue;
                String c = clean(n.prop("command", ""));
                if (!c.isEmpty()) bindings.computeIfAbsent(c, k -> new ArrayList<>()).add(new Binding(def, n, null));
            }
        }
        CommandMap map = Bukkit.getCommandMap();
        for (Map.Entry<String, List<Binding>> e : bindings.entrySet()) {
            String name = e.getKey();
            if (name.equals("silentui") || name.equals("sui")) continue;
            Command existing = map.getCommand(name);
            if (existing != null && !(existing instanceof UiCommand)) {
                plugin.getLogger().warning("[SilentUI] /" + name + " already belongs to another plugin; Silent UI's version is /silentui:" + name + ".");
            }
            UiCommand command = new UiCommand(name, e.getValue(), specs.get(name));
            command.setAliases(aliases.getOrDefault(name, List.of()));
            map.register("silentui", command);
            registered.add(command);
        }
        for (Player p : Bukkit.getOnlinePlayers()) p.updateCommands();
    }

    public void unregisterAll() {
        CommandMap map = Bukkit.getCommandMap();
        Map<String, Command> known = map.getKnownCommands();
        for (Command c : registered) c.unregister(map);
        known.values().removeIf(c -> c instanceof UiCommand);
        registered.clear();
    }

    static String clean(String s) {
        if (s == null) return "";
        String c = s.trim().toLowerCase(Locale.ROOT);
        while (c.startsWith("/")) c = c.substring(1);
        int sp = c.indexOf(' ');
        if (sp >= 0) c = c.substring(0, sp);
        return c.replaceAll("[^a-z0-9_\\-]", "");
    }

    /** "/shop <category> [amount]" */
    static String usage(String label, CommandDef spec) {
        StringBuilder sb = new StringBuilder("/").append(label);
        if (spec == null) return sb.toString();
        for (ArgDef a : spec.args) {
            String inner = a.name + ("choice".equals(a.type) && !a.choices.isEmpty() ? ":" + String.join("|", a.choices) : "");
            sb.append(' ').append(a.optional ? "[" + inner + "]" : "<" + inner + ">");
        }
        return sb.toString();
    }

    /** Parses and validates arguments; returns an error message or null and fills {@code out}. */
    static String parse(CommandDef spec, String[] args, Map<String, String> out) {
        if (spec == null) return null;
        int i = 0;
        for (int ai = 0; ai < spec.args.size(); ai++) {
            ArgDef a = spec.args.get(ai);
            String value;
            if ("text".equals(a.type)) {
                value = i < args.length ? String.join(" ", java.util.Arrays.copyOfRange(args, i, args.length)) : "";
                i = args.length;
            } else {
                value = i < args.length ? args[i++] : "";
            }
            if (value.isEmpty()) {
                if (!a.optional) return "Missing <" + a.name + ">.";
                value = a.def == null ? "" : a.def;
                out.put("arg:" + a.name, value);
                continue;
            }
            switch (a.type) {
                case "number" -> {
                    if (Double.isNaN(Text.num(value, Double.NaN))) return "<" + a.name + "> must be a number.";
                }
                case "integer" -> {
                    try {
                        Long.parseLong(value);
                    } catch (NumberFormatException ex) {
                        return "<" + a.name + "> must be a whole number.";
                    }
                }
                case "player" -> {
                    Player p = Bukkit.getPlayerExact(value);
                    if (p == null) return "Player " + value + " is not online.";
                    value = p.getName();
                }
                case "boolean" -> {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) return "<" + a.name + "> must be true or false.";
                    value = value.toLowerCase(Locale.ROOT);
                }
                case "choice" -> {
                    String match = null;
                    for (String c : a.choices) if (c.equalsIgnoreCase(value)) match = c;
                    if (match == null) return "<" + a.name + "> must be one of: " + String.join(", ", a.choices);
                    value = match;
                }
                default -> { }
            }
            out.put("arg:" + a.name, value);
        }
        if (i < args.length && !spec.args.isEmpty() && !"text".equals(spec.args.get(spec.args.size() - 1).type)) {
            return "Too many arguments.";
        }
        return null;
    }

    private final class UiCommand extends Command {
        private final List<Binding> bindings;
        private final CommandDef spec;

        UiCommand(String name, List<Binding> bindings, CommandDef spec) {
            super(name);
            this.bindings = bindings;
            this.spec = spec;
            setDescription(spec != null && !spec.description.isEmpty() ? spec.description : "Silent UI command");
            setUsage(usage(name, spec));
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (spec != null && !spec.permission.isEmpty() && !p.hasPermission(spec.permission)) {
                p.sendMessage(plugin.message("no-permission"));
                return true;
            }
            Map<String, String> extra = new HashMap<>();
            String err = parse(spec, args, extra);
            if (err != null) {
                p.sendMessage(Text.mm("<red>" + err.replace("<", "\\<") + "</red> <gray>Usage: " + usage(label, spec).replace("<", "\\<")));
                return true;
            }
            extra.put("args", String.join(" ", args));
            for (int i = 0; i < args.length && i < 9; i++) extra.put("arg" + (i + 1), args[i]);
            boolean any = false;
            for (Binding b : bindings) {
                UiDefinition def = plugin.registry().get(b.def().id);
                if (def == null) continue;
                String perm = b.node() != null ? b.node().prop("permission", "")
                        : b.cmd() != null ? b.cmd().permission : def.settings.permission;
                if (perm != null && !perm.isEmpty() && !p.hasPermission(perm)) continue;
                any = true;
                boolean open = b.cmd() == null || b.cmd().open;
                UiInstance ui = open ? ensureOpen(p, def) : plugin.manager().instanceFor(p, def.id);
                if (ui == null) continue;
                if (b.node() != null) {
                    NodeDef node = b.node();
                    plugin.scripts().fire(ui, p, "on_command", n -> n == node, extra);
                } else if (b.cmd() != null && !b.cmd().function.isEmpty()) {
                    plugin.scripts().callFunction(ui, p, b.cmd().function, extra);
                }
            }
            if (!any) p.sendMessage(plugin.message("no-permission"));
            return true;
        }

        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
            if (spec == null || args.length == 0) return List.of();
            int idx = args.length - 1;
            if (idx >= spec.args.size()) {
                ArgDef last = spec.args.isEmpty() ? null : spec.args.get(spec.args.size() - 1);
                if (last == null || !"text".equals(last.type)) return List.of();
                idx = spec.args.size() - 1;
            }
            ArgDef a = spec.args.get(idx);
            List<String> out = new ArrayList<>();
            switch (a.type) {
                case "player" -> Bukkit.getOnlinePlayers().forEach(pl -> out.add(pl.getName()));
                case "choice" -> out.addAll(a.choices);
                case "boolean" -> out.addAll(List.of("true", "false"));
                default -> out.add("<" + a.name + ">");
            }
            String cur = args[args.length - 1].toLowerCase(Locale.ROOT);
            out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(cur) && !s.startsWith("<"));
            return out;
        }
    }

    /** Make sure the UI is running for the player so command actions have something to act on. */
    private UiInstance ensureOpen(Player p, UiDefinition def) {
        if (def.isLogic()) return plugin.manager().instanceFor(p, def.id);
        if (def.isHud()) {
            HudInstance h = plugin.hud().instance(p, def.id);
            if (h == null) {
                plugin.hud().show(p, def.id);
                h = plugin.hud().instance(p, def.id);
            }
            return h;
        }
        if (def.isWorld()) {
            for (WorldUi w : plugin.manager().worlds().values()) if (w.def().id.equals(def.id) && w.isActive()) return w;
            return null;
        }
        ScreenSession s = plugin.manager().screen(p);
        if (s == null || !s.def().id.equals(def.id)) s = plugin.manager().openScreen(p, def);
        return s;
    }
}
