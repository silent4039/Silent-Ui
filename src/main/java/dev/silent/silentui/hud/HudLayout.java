package dev.silent.silentui.hud;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Shared HUD geometry so the pack builder and the renderer always agree. */
public final class HudLayout {

    /** Animated y ranges up to this size get a glyph for every pixel; larger ones only keyframe values. */
    public static final int MAX_DENSE_RANGE = 96;

    private HudLayout() {}

    public static int round(double v) {
        return (int) Math.round(v);
    }

    /** Text line top for a box at y with height h (8px tall glyphs, vertically centred). */
    public static int labelY(int y, int h) {
        return y + Math.max(0, (h - 8) / 2);
    }

    /** Every y value an element can be drawn at (base + y animation keys). */
    public static Map<String, TreeSet<Integer>> yValues(UiDefinition ui) {
        Map<String, TreeSet<Integer>> out = new HashMap<>();
        for (ElementDef e : ui.elements) {
            TreeSet<Integer> set = new TreeSet<>();
            set.add(round(e.y));
            out.put(e.id, set);
        }
        for (UiDefinition.AnimationDef a : ui.animations) {
            for (UiDefinition.TrackDef t : a.tracks) {
                if (!"y".equals(t.property)) continue;
                TreeSet<Integer> set = out.get(t.element);
                if (set == null || t.keys.isEmpty()) continue;
                int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
                for (UiDefinition.KeyDef k : t.keys) {
                    int v = round(k.v);
                    set.add(v);
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
                if (max - min <= MAX_DENSE_RANGE) for (int v = min; v <= max; v++) set.add(v);
            }
        }
        return out;
    }

    /** Sprites a script may switch this element to (set_sprite nodes). */
    public static Set<String> alternateSprites(UiDefinition ui, ElementDef e) {
        Set<String> out = new LinkedHashSet<>();
        for (UiDefinition.NodeDef n : ui.script.nodes) {
            if ("set_sprite".equals(n.type) && e.id.equals(n.prop("element", ""))) {
                String s = n.prop("sprite", "");
                if (!s.isEmpty()) out.add(s);
            }
        }
        return out;
    }

    /** Nearest y in the set (used when an animation passes a y without a generated glyph). */
    public static int nearest(TreeSet<Integer> set, int y) {
        if (set == null || set.isEmpty()) return y;
        Integer lo = set.floor(y), hi = set.ceiling(y);
        if (lo == null) return hi;
        if (hi == null) return lo;
        return (y - lo) <= (hi - y) ? lo : hi;
    }
}
