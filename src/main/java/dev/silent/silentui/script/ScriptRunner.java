package dev.silent.silentui.script;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.NodeDef;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.render.ElementState;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Runs the node graphs made in the editor's script view.
 * Event nodes start a chain; action nodes run in order following their "then" (or "true"/"false") outputs.
 */
public final class ScriptRunner {

    private static final int MAX_STEPS = 512;

    private final SilentUI plugin;

    public ScriptRunner(SilentUI plugin) {
        this.plugin = plugin;
    }

    /** Fire every event node of {@code type} that passes {@code filter}. */
    public void fire(UiInstance ui, Player player, String type, Predicate<NodeDef> filter, Map<String, String> extra) {
        if (ui == null || !ui.isActive()) return;
        for (NodeDef node : ui.def().script.nodes) {
            if (!type.equals(node.type)) continue;
            if (filter != null && !filter.test(node)) continue;
            String perm = node.prop("permission", "");
            if (!perm.isEmpty() && player != null && !player.hasPermission(perm)) continue;
            run(ui, player, node.out("then"), extra == null ? Map.of() : extra);
        }
    }

    public void fire(UiInstance ui, Player player, String type) {
        fire(ui, player, type, null, null);
    }

    /** Fire element-scoped events (click / hover / text). */
    public void fireElement(UiInstance ui, Player player, String type, String elementId, Map<String, String> extra) {
        fire(ui, player, type, n -> {
            String el = n.prop("element", "");
            return el.isEmpty() || el.equals(elementId);
        }, extra);
    }

    private void run(UiInstance ui, Player player, List<String> start, Map<String, String> extra) {
        Deque<String> queue = new ArrayDeque<>(start);
        Map<String, NodeDef> byId = new HashMap<>();
        for (NodeDef n : ui.def().script.nodes) byId.put(n.id, n);
        int steps = 0;
        while (!queue.isEmpty()) {
            if (++steps > MAX_STEPS) {
                plugin.getLogger().warning("[SilentUI] Script in '" + ui.def().id + "' stopped after " + MAX_STEPS + " steps (loop?).");
                return;
            }
            if (!ui.isActive()) return;
            NodeDef node = byId.get(queue.pollFirst());
            if (node == null) continue;
            if ("wait".equals(node.type)) {
                long ticks = Math.max(1, (long) Text.num(node.prop("ticks", "20"), 20));
                List<String> rest = new java.util.ArrayList<>(node.out("then"));
                rest.addAll(queue); // anything else queued continues after the wait too
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (ui.isActive() && (player == null || player.isOnline())) run(ui, player, rest, extra);
                }, ticks);
                return;
            }
            String port;
            try {
                port = execute(ui, player, node, extra);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("[SilentUI] Script node '" + node.type + "' failed in '" + ui.def().id + "': " + ex);
                port = "then";
            }
            if (port != null) queue.addAll(node.out(port));
        }
    }

    private String p(UiInstance ui, Player player, NodeDef n, String key, String def, Map<String, String> extra) {
        String raw = n.prop(key, def);
        Map<String, String> vars = player == null ? new HashMap<>() : new HashMap<>(plugin.vars(player));
        vars.putAll(extra);
        String out = Text.apply(raw, player, vars);
        for (Map.Entry<String, String> e : extra.entrySet()) out = out.replace("{" + e.getKey() + "}", e.getValue());
        return out;
    }

    /** Executes one action node and returns the output port to follow. */
    private String execute(UiInstance ui, Player player, NodeDef n, Map<String, String> x) {
        String el = n.prop("element", "");
        ElementState st = ui.state(el);
        switch (n.type) {
            case "show", "hide", "toggle" -> {
                if (st != null) {
                    st.visible = switch (n.type) {
                        case "show" -> true;
                        case "hide" -> false;
                        default -> !st.visible;
                    };
                    ui.refresh(el, 0);
                }
            }
            case "set_text" -> {
                if (st != null) {
                    // World UIs are shared by everyone, so placeholders resolve now (for the clicking player).
                    // Screens and HUDs keep the raw text so placeholders stay live.
                    st.text = ui.def().isWorld() ? p(ui, player, n, "text", "", x) : n.prop("text", "");
                    ui.refresh(el, 0);
                }
            }
            case "set_color" -> {
                if (st != null) {
                    String c = p(ui, player, n, "color", "#ffffff", x);
                    if ("text".equals(st.type())) st.textColor = c;
                    else st.fill = c;
                    ui.refresh(el, 0);
                }
            }
            case "set_text_color" -> {
                if (st != null) {
                    st.textColor = p(ui, player, n, "color", "#ffffff", x);
                    ui.refresh(el, 0);
                }
            }
            case "set_sprite" -> {
                if (st != null) {
                    st.sprite = n.prop("sprite", "");
                    ui.refresh(el, 0);
                }
            }
            case "set_progress" -> {
                if (st != null) {
                    st.value = n.prop("value", "0");
                    String max = n.prop("max", "");
                    if (!max.isEmpty()) st.max = max;
                    ui.refresh(el, 0);
                }
            }
            case "move" -> {
                if (st != null) {
                    st.x = Text.num(p(ui, player, n, "x", String.valueOf(st.x), x), st.x);
                    st.y = Text.num(p(ui, player, n, "y", String.valueOf(st.y), x), st.y);
                    ui.refresh(el, (int) Text.num(n.prop("ticks", "0"), 0));
                }
            }
            case "play_animation" -> ui.playAnimation(n.prop("animation", ""));
            case "stop_animation" -> ui.stopAnimation(n.prop("animation", ""));
            case "open_ui" -> {
                if (player != null) {
                    String target = n.prop("ui", "");
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.manager().openAny(player, target));
                }
            }
            case "close_ui" -> {
                if (player != null) Bukkit.getScheduler().runTask(plugin, () -> ui.close(player));
            }
            case "focus_input" -> {
                if (player != null && st != null) ui.focusInput(player, el);
            }
            case "run_command" -> {
                String cmd = stripSlash(p(ui, player, n, "command", "", x));
                if (cmd.isEmpty()) break;
                if ("console".equalsIgnoreCase(n.prop("as", "player")) || player == null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                } else {
                    player.performCommand(cmd);
                }
            }
            case "message" -> {
                if (player != null) player.sendMessage(Text.mm(p(ui, player, n, "text", "", x)));
            }
            case "broadcast" -> Bukkit.broadcast(Text.mm(p(ui, player, n, "text", "", x)));
            case "actionbar" -> {
                if (player != null) player.sendActionBar(Text.mm(p(ui, player, n, "text", "", x)));
            }
            case "title" -> {
                if (player != null) {
                    Title.Times times = Title.Times.times(
                            ticks(n.prop("in", "10")), ticks(n.prop("stay", "40")), ticks(n.prop("out", "10")));
                    player.showTitle(Title.title(Text.mm(p(ui, player, n, "title", "", x)),
                            Text.mm(p(ui, player, n, "subtitle", "", x)), times));
                }
            }
            case "sound" -> {
                if (player != null) {
                    String id = n.prop("sound", "minecraft:ui.button.click");
                    try {
                        player.playSound(Sound.sound(Key.key(id.toLowerCase(java.util.Locale.ROOT)), Sound.Source.MASTER,
                                (float) Text.num(n.prop("volume", "1"), 1), (float) Text.num(n.prop("pitch", "1"), 1)));
                    } catch (RuntimeException ignored) {
                        // invalid key
                    }
                }
            }
            case "screen_fade" -> {
                if (player != null) {
                    Component glyph = Component.text(String.valueOf(GlyphRegistry.FADE))
                            .font(GlyphRegistry.FONT_FX)
                            .color(Text.color(n.prop("color", "#000000"), 0));
                    Title.Times times = Title.Times.times(
                            ticks(n.prop("in", "10")), ticks(n.prop("stay", "10")), ticks(n.prop("out", "10")));
                    player.showTitle(Title.title(glyph, Component.empty(), times));
                }
            }
            case "set_var" -> {
                if (player != null) plugin.vars(player).put(n.prop("name", "var"), p(ui, player, n, "value", "", x));
            }
            case "add_var" -> {
                if (player != null) {
                    Map<String, String> vars = plugin.vars(player);
                    String name = n.prop("name", "var");
                    double v = Text.num(vars.get(name), 0) + Text.num(p(ui, player, n, "amount", "1", x), 1);
                    vars.put(name, v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v));
                }
            }
            case "if_var" -> {
                String left = player == null ? "" : plugin.vars(player).getOrDefault(n.prop("name", "var"), "");
                return compare(left, n.prop("op", "=="), p(ui, player, n, "value", "", x)) ? "true" : "false";
            }
            case "if_compare" -> {
                return compare(p(ui, player, n, "left", "", x), n.prop("op", "=="), p(ui, player, n, "right", "", x)) ? "true" : "false";
            }
            case "if_permission" -> {
                return player != null && player.hasPermission(n.prop("permission", "")) ? "true" : "false";
            }
            case "random" -> {
                double chance = Text.num(n.prop("chance", "50"), 50) / 100.0;
                return ThreadLocalRandom.current().nextDouble() < chance ? "true" : "false";
            }
            default -> {
                // event nodes or unknown types just pass through
            }
        }
        return "then";
    }

    private static boolean compare(String a, String op, String b) {
        double na = Text.num(a, Double.NaN), nb = Text.num(b, Double.NaN);
        boolean numeric = !Double.isNaN(na) && !Double.isNaN(nb);
        return switch (op) {
            case "!=" -> numeric ? na != nb : !a.equals(b);
            case ">" -> numeric && na > nb;
            case ">=" -> numeric && na >= nb;
            case "<" -> numeric && na < nb;
            case "<=" -> numeric && na <= nb;
            case "contains" -> a.toLowerCase().contains(b.toLowerCase());
            default -> numeric ? na == nb : a.equalsIgnoreCase(b);
        };
    }

    private static Duration ticks(String s) {
        return Duration.ofMillis((long) (Math.max(0, Text.num(s, 0)) * 50));
    }

    private static String stripSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }

    /** True if the UI has any event node of this type (cheap checks before doing work). */
    public static boolean has(UiDefinition def, String type) {
        for (NodeDef n : def.script.nodes) if (type.equals(n.type)) return true;
        return false;
    }
}
