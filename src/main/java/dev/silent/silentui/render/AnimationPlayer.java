package dev.silent.silentui.render;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.AnimationDef;
import dev.silent.silentui.model.UiDefinition.KeyDef;
import dev.silent.silentui.model.UiDefinition.TrackDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiConsumer;

/**
 * Keyframe animations.
 * <p>Display-entity UIs run in "segment" mode: at each keyframe the client is told where the element will be at the
 * NEXT keyframe and how many ticks it has to get there, so motion is interpolated client-side at full FPS
 * with only one packet per segment. Non-linear easings are split into 2-tick sub-segments.
 * <p>HUDs run in "per tick" mode because bossbar text cannot interpolate.
 */
public final class AnimationPlayer {

    private final UiDefinition def;
    private final Map<String, ElementState> states;
    /** (elementId, interpolationTicks) */
    private final BiConsumer<String, Integer> push;
    private final boolean segmentMode;
    private final List<Active> active = new ArrayList<>();

    private static final class Active {
        final AnimationDef def;
        final Map<String, List<TrackDef>> tracksByElement = new HashMap<>();
        final Map<String, int[]> breakpoints = new HashMap<>();
        final Map<String, Boolean> stepped = new HashMap<>();
        int t = 0;
        boolean primed = false;

        Active(AnimationDef def) {
            this.def = def;
            for (TrackDef tr : def.tracks) {
                if (tr.keys.isEmpty()) continue;
                tracksByElement.computeIfAbsent(tr.element, k -> new ArrayList<>()).add(tr);
            }
            for (Map.Entry<String, List<TrackDef>> e : tracksByElement.entrySet()) {
                TreeSet<Integer> bp = new TreeSet<>();
                bp.add(0);
                bp.add(def.length);
                boolean step = false;
                for (TrackDef tr : e.getValue()) {
                    for (int i = 0; i < tr.keys.size(); i++) {
                        KeyDef k = tr.keys.get(i);
                        if (k.t >= 0 && k.t <= def.length) bp.add(k.t);
                        if ("step".equals(k.ease)) step = true;
                        // colours can't be interpolated by the client and big turns would take the short way round,
                        // so those always move in 2 tick steps, like non-linear easings
                        boolean fine = isColor(tr.property) || "turn".equals(tr.property)
                                ? !"step".equals(k.ease) : !"linear".equals(k.ease) && !"step".equals(k.ease);
                        if (i + 1 < tr.keys.size() && fine) {
                            for (int s = k.t + 2; s < tr.keys.get(i + 1).t; s += 2) bp.add(s);
                        }
                    }
                }
                breakpoints.put(e.getKey(), bp.stream().mapToInt(Integer::intValue).toArray());
                stepped.put(e.getKey(), step);
            }
        }
    }

    public AnimationPlayer(UiDefinition def, Map<String, ElementState> states, BiConsumer<String, Integer> push, boolean segmentMode) {
        this.def = def;
        this.states = states;
        this.push = push;
        this.segmentMode = segmentMode;
    }

    public void play(String idOrName) {
        AnimationDef a = def.animation(idOrName);
        if (a == null) return;
        stop(a.id);
        active.add(new Active(a));
    }

    public void stop(String idOrName) {
        active.removeIf(a -> a.def.id.equals(idOrName) || a.def.name.equalsIgnoreCase(idOrName));
    }

    public boolean running() {
        return !active.isEmpty();
    }

    public void autoplay() {
        for (AnimationDef a : def.animations) if (a.autoplay) play(a.id);
    }

    public void tick() {
        Iterator<Active> it = active.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            if (!a.primed) {
                a.primed = true;
                for (String el : a.tracksByElement.keySet()) {
                    apply(a, el, 0, 0);
                    push.accept(el, 0);
                }
                if (segmentMode) continue; // start segments next tick
            }
            if (segmentMode) {
                for (Map.Entry<String, int[]> e : a.breakpoints.entrySet()) {
                    int[] bps = e.getValue();
                    int idx = Arrays.binarySearch(bps, a.t);
                    if (idx < 0) continue;
                    String el = e.getKey();
                    if (Boolean.TRUE.equals(a.stepped.get(el))) {
                        apply(a, el, a.t, a.t);
                        push.accept(el, 0);
                    } else if (idx + 1 < bps.length) {
                        int next = bps[idx + 1];
                        apply(a, el, next, a.t);
                        push.accept(el, next - a.t);
                    }
                }
            } else {
                for (String el : a.tracksByElement.keySet()) {
                    apply(a, el, a.t, a.t);
                    push.accept(el, 1);
                }
            }
            a.t++;
            if (a.t > a.def.length) {
                if (a.def.loop) {
                    a.t = 0;
                    a.primed = false;
                } else {
                    it.remove();
                }
            }
        }
    }

    static boolean isColor(String property) {
        return "color".equals(property) || "fill".equals(property) || "textColor".equals(property);
    }

    /**
     * Applies every track of the element. Motion uses {@code t} (where the client should glide to);
     * colours use {@code colorT} (now), because colour changes are instant on the client.
     */
    private void apply(Active a, String elementId, int t, int colorT) {
        ElementState st = states.get(elementId);
        if (st == null) return;
        for (TrackDef tr : a.tracksByElement.getOrDefault(elementId, List.of())) {
            if (isColor(tr.property)) {
                String hex = String.format("#%06x", sampleColor(tr.keys, colorT));
                boolean textual = "text".equals(st.type()) || "button".equals(st.type()) || "input".equals(st.type());
                switch (tr.property) {
                    case "fill" -> st.fill = hex;
                    case "textColor" -> st.textColor = hex;
                    default -> {
                        if (textual) st.textColor = hex;
                        else st.fill = hex;
                    }
                }
                continue;
            }
            double v = sample(tr.keys, t);
            switch (tr.property) {
                case "x" -> st.x = v;
                case "y" -> st.y = v;
                case "w" -> st.w = Math.max(0, v);
                case "h" -> st.h = Math.max(0, v);
                case "scale" -> st.scale = Math.max(0, v);
                case "rotation" -> st.rotation = v;
                case "opacity" -> st.opacity = Math.max(0, Math.min(1, v));
                case "turn" -> st.turn = v;
                default -> { }
            }
        }
    }

    public static double sample(List<KeyDef> keys, double t) {
        if (keys.isEmpty()) return 0;
        KeyDef first = keys.get(0);
        if (t <= first.t) return first.v;
        for (int i = 0; i < keys.size() - 1; i++) {
            KeyDef a = keys.get(i), b = keys.get(i + 1);
            if (t >= a.t && t <= b.t) {
                if (b.t == a.t) return b.v;
                if ("step".equals(a.ease)) return t >= b.t ? b.v : a.v;
                double f = ease(a.ease, (t - a.t) / (double) (b.t - a.t));
                return a.v + (b.v - a.v) * f;
            }
        }
        return keys.get(keys.size() - 1).v;
    }

    /** Colour keys store 0xRRGGBB in v; channels blend separately so red to blue passes through purple. */
    public static int sampleColor(List<KeyDef> keys, double t) {
        if (keys.isEmpty()) return 0xFFFFFF;
        KeyDef first = keys.get(0);
        if (t <= first.t) return (int) first.v;
        for (int i = 0; i < keys.size() - 1; i++) {
            KeyDef a = keys.get(i), b = keys.get(i + 1);
            if (t >= a.t && t <= b.t) {
                if (b.t == a.t || "step".equals(a.ease)) return (int) (t >= b.t ? b.v : a.v);
                double f = ease(a.ease, (t - a.t) / (double) (b.t - a.t));
                int ca = (int) a.v, cb = (int) b.v;
                int r = (int) Math.round(((ca >> 16) & 255) + (((cb >> 16) & 255) - ((ca >> 16) & 255)) * f);
                int g = (int) Math.round(((ca >> 8) & 255) + (((cb >> 8) & 255) - ((ca >> 8) & 255)) * f);
                int bl = (int) Math.round((ca & 255) + ((cb & 255) - (ca & 255)) * f);
                return (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, bl));
            }
        }
        return (int) keys.get(keys.size() - 1).v;
    }

    public static double ease(String type, double f) {
        if (type == null) return f;
        return switch (type) {
            case "ease-in" -> f * f;
            case "ease-out" -> 1 - (1 - f) * (1 - f);
            case "ease-in-out" -> f < 0.5 ? 2 * f * f : 1 - Math.pow(-2 * f + 2, 2) / 2;
            case "back" -> {
                double c1 = 1.70158, c3 = c1 + 1;
                yield 1 + c3 * Math.pow(f - 1, 3) + c1 * Math.pow(f - 1, 2);
            }
            case "bounce" -> bounce(f);
            default -> f;
        };
    }

    private static double bounce(double x) {
        double n1 = 7.5625, d1 = 2.75;
        if (x < 1 / d1) return n1 * x * x;
        if (x < 2 / d1) return n1 * (x -= 1.5 / d1) * x + 0.75;
        if (x < 2.5 / d1) return n1 * (x -= 2.25 / d1) * x + 0.9375;
        return n1 * (x -= 2.625 / d1) * x + 0.984375;
    }
}
