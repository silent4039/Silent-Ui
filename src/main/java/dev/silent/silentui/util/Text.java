package dev.silent.silentui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** MiniMessage / legacy colours plus placeholders ({player}, {var:x}, %papi%). */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern VAR = Pattern.compile("\\{var:([A-Za-z0-9_.-]+)}");
    private static Method papiMethod;
    private static boolean papiChecked;

    private Text() {}

    public static Component mm(String s) {
        if (s == null || s.isEmpty()) return Component.empty();
        try {
            if (s.indexOf('&') >= 0 && s.indexOf('<') < 0) return LEGACY.deserialize(s);
            return MM.deserialize(s);
        } catch (RuntimeException ex) {
            return Component.text(s);
        }
    }

    public static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    /** Resolve all placeholders. {@code vars} may be null. */
    public static String apply(String s, Player p, Map<String, String> vars) {
        if (s == null || s.isEmpty()) return "";
        String out = s;
        if (out.indexOf('{') >= 0) {
            if (p != null) {
                out = out.replace("{player}", p.getName())
                        .replace("{uuid}", p.getUniqueId().toString())
                        .replace("{world}", p.getWorld().getName())
                        .replace("{level}", String.valueOf(p.getLevel()))
                        .replace("{health}", String.valueOf((int) Math.ceil(p.getHealth())))
                        .replace("{food}", String.valueOf(p.getFoodLevel()))
                        .replace("{ping}", String.valueOf(p.getPing()))
                        .replace("{x}", String.valueOf(p.getLocation().getBlockX()))
                        .replace("{y}", String.valueOf(p.getLocation().getBlockY()))
                        .replace("{z}", String.valueOf(p.getLocation().getBlockZ()));
            }
            out = out.replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("{max_players}", String.valueOf(Bukkit.getMaxPlayers()));
            if (out.contains("{time}")) {
                out = out.replace("{time}", java.time.LocalTime.now().withNano(0).toString());
            }
            if (out.contains("{date}")) {
                out = out.replace("{date}", java.time.LocalDate.now().toString());
            }
            if (vars != null && out.contains("{var:")) {
                Matcher m = VAR.matcher(out);
                StringBuilder sb = new StringBuilder();
                while (m.find()) {
                    String v = vars.getOrDefault(m.group(1), "");
                    m.appendReplacement(sb, Matcher.quoteReplacement(v));
                }
                m.appendTail(sb);
                out = sb.toString();
            }
        }
        if (p != null && out.indexOf('%') >= 0) out = papi(p, out);
        return out;
    }

    private static String papi(OfflinePlayer p, String s) {
        if (!papiChecked) {
            papiChecked = true;
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    Class<?> c = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    papiMethod = c.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
                } catch (ReflectiveOperationException ignored) {
                    papiMethod = null;
                }
            }
        }
        if (papiMethod == null) return s;
        try {
            return (String) papiMethod.invoke(null, p, s);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return s;
        }
    }

    public static void resetPapi() {
        papiChecked = false;
        papiMethod = null;
    }

    /** "#rrggbb" or "#aarrggbb" or "rrggbb" -> rgb int; falls back to white. */
    public static int rgb(String hex, int fallback) {
        if (hex == null) return fallback;
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        try {
            if (h.length() == 8) h = h.substring(2);
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            if (h.length() != 6) return fallback;
            return Integer.parseInt(h, 16);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static TextColor color(String hex, int fallback) {
        return TextColor.color(rgb(hex, fallback));
    }

    public static double num(String s, double def) {
        if (s == null) return def;
        try {
            return Double.parseDouble(s.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
