package dev.silent.silentui.command;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.hud.HudInstance;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.NodeDef;
import dev.silent.silentui.render.ScreenSession;
import dev.silent.silentui.render.WorldUi;
import dev.silent.silentui.script.UiInstance;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registers commands declared by UIs: settings.command (opens the UI) and "When command is used" script nodes.
 */
public final class DynamicCommands {

    private final SilentUI plugin;
    private final List<Command> registered = new ArrayList<>();

    public DynamicCommands(SilentUI plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        unregisterAll();
        Map<String, List<Binding>> bindings = new HashMap<>();
        for (UiDefinition def : plugin.registry().all()) {
            String cmd = clean(def.settings.command);
            if (!cmd.isEmpty()) bindings.computeIfAbsent(cmd, k -> new ArrayList<>()).add(new Binding(def, null));
            for (NodeDef n : def.script.nodes) {
                if (!"on_command".equals(n.type)) continue;
                String c = clean(n.prop("command", ""));
                if (!c.isEmpty()) bindings.computeIfAbsent(c, k -> new ArrayList<>()).add(new Binding(def, n));
            }
        }
        CommandMap map = Bukkit.getCommandMap();
        for (Map.Entry<String, List<Binding>> e : bindings.entrySet()) {
            String name = e.getKey();
            if (name.equals("silentui") || name.equals("sui")) continue;
            Command existing = map.getCommand(name);
            if (existing != null && !(existing instanceof UiCommand)) {
                plugin.getLogger().warning("[SilentUI] Command /" + name + " already belongs to another plugin, registered as /silentui:" + name + " only.");
            }
            UiCommand command = new UiCommand(name, e.getValue());
            map.register("silentui", command);
            registered.add(command);
        }
        for (Player p : Bukkit.getOnlinePlayers()) p.updateCommands();
    }

    public void unregisterAll() {
        CommandMap map = Bukkit.getCommandMap();
        Map<String, Command> known = map.getKnownCommands();
        for (Command c : registered) {
            known.remove(c.getName());
            known.remove("silentui:" + c.getName());
            c.unregister(map);
        }
        // also drop stale entries that point at our command objects
        known.values().removeIf(c -> c instanceof UiCommand);
        registered.clear();
    }

    private static String clean(String s) {
        if (s == null) return "";
        String c = s.trim().toLowerCase(Locale.ROOT);
        while (c.startsWith("/")) c = c.substring(1);
        int sp = c.indexOf(' ');
        if (sp >= 0) c = c.substring(0, sp);
        return c.replaceAll("[^a-z0-9_\\-]", "");
    }

    private record Binding(UiDefinition def, NodeDef node) {}

    private final class UiCommand extends Command {
        private final List<Binding> bindings;

        UiCommand(String name, List<Binding> bindings) {
            super(name);
            this.bindings = bindings;
            setDescription("Silent UI command");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Players only.");
                return true;
            }
            boolean any = false;
            for (Binding b : bindings) {
                UiDefinition def = plugin.registry().get(b.def().id);
                if (def == null) continue;
                String perm = b.node() != null ? b.node().prop("permission", "") : def.settings.permission;
                if (perm != null && !perm.isEmpty() && !p.hasPermission(perm)) continue;
                any = true;
                UiInstance ui = ensureOpen(p, def);
                if (ui == null || b.node() == null) continue;
                Map<String, String> extra = new HashMap<>();
                extra.put("args", String.join(" ", args));
                for (int i = 0; i < args.length && i < 9; i++) extra.put("arg" + (i + 1), args[i]);
                NodeDef node = b.node();
                plugin.scripts().fire(ui, p, "on_command", n -> n == node, extra);
            }
            if (!any) p.sendMessage(plugin.message("no-permission"));
            return true;
        }
    }

    /** Make sure the UI is running for the player so command actions have something to act on. */
    private UiInstance ensureOpen(Player p, UiDefinition def) {
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
