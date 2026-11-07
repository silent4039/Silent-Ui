package dev.silent.silentui.util;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.model.UiRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Converts DecentHolograms files (plugins/DecentHolograms/holograms/**.yml) into Silent UI holograms:
 * world UIs that always face the player, one text element per line, items for #ICON lines and heads for #HEAD lines.
 * Multiple pages become a tab set that a click moves through.
 */
public final class DecentHologramsImporter {

    /** 1 canvas pixel = 1 font pixel of a text display at scale 1 (0.025 blocks). */
    private static final double WORLD_SCALE = 2.5;
    private static final Pattern DH_GRADIENT = Pattern.compile("<#([0-9a-fA-F]{6})>(.*?)</#([0-9a-fA-F]{6})>");
    private static final Pattern AMP_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern BRACE_HEX = Pattern.compile("\\{#([0-9a-fA-F]{6})}");

    public record Imported(UiDefinition def, Location origin) {}

    private DecentHologramsImporter() {}

    public static List<Imported> load(Path folder, List<String> problems) throws IOException {
        List<Imported> out = new ArrayList<>();
        if (!Files.isDirectory(folder)) return out;
        List<Path> files;
        try (Stream<Path> s = Files.walk(folder)) {
            files = s.filter(p -> p.toString().endsWith(".yml")).sorted().toList();
        }
        for (Path f : files) {
            try {
                Imported i = convert(folder, f);
                if (i != null) out.add(i);
            } catch (RuntimeException ex) {
                problems.add(folder.relativize(f) + ": " + ex.getMessage());
            }
        }
        return out;
    }

    private static Imported convert(Path root, Path file) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file.toFile());
        if (!y.getBoolean("enabled", true)) return null;
        String rel = root.relativize(file).toString().replace('\\', '/').replace(".yml", "");
        String id = UiRegistry.cleanId("dh_" + rel);
        UiDefinition def = new UiDefinition();
        def.id = id;
        def.name = rel;
        def.type = "world";
        def.settings.billboard = "center";
        def.settings.worldScale = WORLD_SCALE;
        def.settings.viewRange = y.getDouble("display-range", 48);

        List<Map<?, ?>> pages = y.getMapList("pages");
        if (pages.isEmpty()) return null;
        int width = 40;
        List<List<Line>> pageLines = new ArrayList<>();
        for (Map<?, ?> page : pages) {
            List<Line> lines = new ArrayList<>();
            Object raw = page.get("lines");
            if (raw instanceof List<?> list) {
                for (Object o : list) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    String content = String.valueOf(m.get("content") == null ? "" : m.get("content"));
                    double h = m.get("height") instanceof Number n ? n.doubleValue() : 0.3;
                    Line l = new Line(content, Math.max(4, (int) Math.round(h / 0.025)));
                    lines.add(l);
                    if (!l.isItem() && !l.isHead()) width = Math.max(width, FontWidths.width(Text.plain(Text.mm(toMini(content))), false) + 8);
                }
            }
            pageLines.add(lines);
        }
        int height = 8;
        for (List<Line> lines : pageLines) height = Math.max(height, lines.stream().mapToInt(Line::height).sum() + 4);
        def.canvas.width = Math.min(2048, width);
        def.canvas.height = Math.min(2048, height);

        boolean placeholders = false;
        int n = 0;
        for (int p = 0; p < pageLines.size(); p++) {
            int yy = 2;
            for (Line l : pageLines.get(p)) {
                ElementDef e = new ElementDef();
                e.id = "l" + (++n);
                e.name = "Line " + n;
                if (pageLines.size() > 1) e.group = "pages/p" + (p + 1);
                if (l.isItem()) {
                    e.type = "item";
                    e.item = "minecraft:" + l.arg().toLowerCase().replace("minecraft:", "").trim();
                    e.w = e.h = Math.min(32, l.height() * 2);
                    e.x = def.canvas.width / 2.0 - e.w / 2;
                    e.y = yy;
                    e.fillEnabled = false;
                    yy += Math.max(l.height(), (int) e.h);
                } else if (l.isHead()) {
                    e.type = "head";
                    e.headOwner = l.arg().replace("%player_name%", "{player}");
                    e.w = e.h = 24;
                    e.x = def.canvas.width / 2.0 - 12;
                    e.y = yy;
                    e.fillEnabled = false;
                    yy += Math.max(l.height(), 24);
                } else {
                    e.type = "text";
                    e.text = toMini(l.content());
                    e.fillEnabled = false;
                    e.x = 0;
                    e.y = yy;
                    e.w = def.canvas.width;
                    e.h = 10;
                    e.blocked = true;
                    yy += l.height();
                    if (l.content().contains("%")) placeholders = true;
                }
                def.elements.add(e);
            }
        }
        def.settings.perPlayer = placeholders;
        if (pageLines.size() > 1) {
            def.tabs.put("pages", "p1");
            UiDefinition.NodeDef click = new UiDefinition.NodeDef();
            click.id = "dh_click";
            click.type = "on_click";
            UiDefinition.NodeDef next = new UiDefinition.NodeDef();
            next.id = "dh_next";
            next.type = "next_tab";
            next.props.put("tabset", "pages");
            click.next.put("then", new ArrayList<>(List.of("dh_next")));
            def.script.nodes.add(click);
            def.script.nodes.add(next);
            for (ElementDef e : def.elements) e.blocked = false;
        }
        Location loc = parseLocation(y.getString("location", ""));
        if (loc == null) throw new IllegalArgumentException("bad location");
        // DecentHolograms grows downwards from its location; our origin is the canvas centre
        double heightBlocks = def.canvas.height * WORLD_SCALE / 100.0;
        loc.add(0, y.getBoolean("down-origin", false) ? heightBlocks / 2 : -heightBlocks / 2 + 0.25, 0);
        return new Imported(def, loc);
    }

    private record Line(String content, int height) {
        boolean isItem() { return content.trim().toUpperCase().startsWith("#ICON"); }
        boolean isHead() { return content.trim().toUpperCase().startsWith("#HEAD") || content.trim().toUpperCase().startsWith("#SMALLHEAD"); }
        String arg() {
            String c = content.trim();
            int i = c.indexOf(':');
            String a = i >= 0 ? c.substring(i + 1).trim() : "";
            int sp = a.indexOf(' ');
            int brace = a.indexOf('{');
            if (brace > 0) a = a.substring(0, brace);
            if (sp > 0 && !isHead()) a = a.substring(0, sp);
            return a.isEmpty() ? "stone" : a.trim();
        }
    }

    static Location parseLocation(String s) {
        String[] p = s.split(":");
        if (p.length < 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        try {
            return new Location(w, Double.parseDouble(p[1].replace(',', '.')), Double.parseDouble(p[2].replace(',', '.')),
                    Double.parseDouble(p[3].replace(',', '.')));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** DecentHolograms / legacy formatting to MiniMessage. */
    public static String toMini(String s) {
        String out = s;
        Matcher g = DH_GRADIENT.matcher(out);
        out = g.replaceAll("<gradient:#$1:#$3>$2</gradient>");
        out = AMP_HEX.matcher(out).replaceAll("<reset><#$1>");
        out = BRACE_HEX.matcher(out).replaceAll("<reset><#$1>");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < out.length()) {
                String tag = switch (Character.toLowerCase(out.charAt(i + 1))) {
                    case '0' -> "<reset><black>";
                    case '1' -> "<reset><dark_blue>";
                    case '2' -> "<reset><dark_green>";
                    case '3' -> "<reset><dark_aqua>";
                    case '4' -> "<reset><dark_red>";
                    case '5' -> "<reset><dark_purple>";
                    case '6' -> "<reset><gold>";
                    case '7' -> "<reset><gray>";
                    case '8' -> "<reset><dark_gray>";
                    case '9' -> "<reset><blue>";
                    case 'a' -> "<reset><green>";
                    case 'b' -> "<reset><aqua>";
                    case 'c' -> "<reset><red>";
                    case 'd' -> "<reset><light_purple>";
                    case 'e' -> "<reset><yellow>";
                    case 'f' -> "<reset><white>";
                    case 'l' -> "<bold>";
                    case 'o' -> "<italic>";
                    case 'n' -> "<underlined>";
                    case 'm' -> "<strikethrough>";
                    case 'k' -> "<obfuscated>";
                    case 'r' -> "<reset>";
                    default -> null;
                };
                if (tag != null) {
                    sb.append(tag);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString().replace("{player}", "%player_name%").replace("%player_name%", "{player}");
    }
}
