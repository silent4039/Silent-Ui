package dev.silent.silentui.hud;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.model.UiDefinition.NodeDef;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.render.AnimationPlayer;
import dev.silent.silentui.render.ElementState;
import dev.silent.silentui.script.UiInstance;
import dev.silent.silentui.util.FontWidths;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * One HUD shown to one player. Drawn as bossbar text: every element is a glyph (or shifted text) wrapped in
 * negative/positive spaces so it lands at an exact pixel position and takes zero net width.
 */
public final class HudInstance implements UiInstance {

    private final SilentUI plugin;
    private final UiDefinition def;
    private final Player player;
    private final Map<String, ElementState> states = new LinkedHashMap<>();
    private final Map<String, TreeSet<Integer>> yValues;
    private final AnimationPlayer animations;
    private boolean active = true;
    private boolean dirty = true;
    private long ticks;

    public HudInstance(SilentUI plugin, UiDefinition def, Player player) {
        this.plugin = plugin;
        this.def = def;
        this.player = player;
        for (ElementDef e : def.elements) states.put(e.id, new ElementState(e));
        this.yValues = HudLayout.yValues(def);
        this.animations = new AnimationPlayer(def, states, (id, t) -> dirty = true, false);
        applyDefaultTabs();
        animations.autoplay();
    }

    /** Ticks animations and timers; returns true when the HUD needs redrawing. */
    boolean tick() {
        ticks++;
        animations.tick();
        for (NodeDef n : def.script.nodes) {
            if (!"on_timer".equals(n.type)) continue;
            long interval = Math.max(1, Math.round(Text.num(n.prop("seconds", "1"), 1) * 20));
            if (ticks % interval == 0) plugin.scripts().fire(this, player, "on_timer", node -> node == n, null);
        }
        boolean d = dirty;
        dirty = false;
        return d;
    }

    /** Builds this HUD's part of the bossbar title. */
    Component render() {
        GlyphRegistry g = plugin.glyphs();
        TextComponent.Builder out = Component.text();
        int half = def.canvas.width / 2;
        Map<String, String> vars = plugin.vars(player);
        for (ElementState s : states.values()) {
            if (!s.visible || s.opacity <= 0.02) continue;
            ElementDef e = s.def;
            int x = HudLayout.round(s.x) - half;
            int w = HudLayout.round(s.w), h = HudLayout.round(s.h);
            int y = HudLayout.nearest(yValues.get(e.id), HudLayout.round(s.y));
            switch (e.type) {
                case "item", "head", "model" -> { }
                case "progress" -> {
                    if (e.fillEnabled) glyph(out, g, e.sprite, s.fill, x, w, h, y);
                    double v = Text.num(Text.apply(s.value, player, vars), 0);
                    double m = Text.num(Text.apply(s.max, player, vars), 100);
                    int fw = m <= 0 ? 0 : (int) Math.round(w * Math.max(0, Math.min(1, v / m)));
                    bar(out, g, e.barColor, x, fw, h, y);
                }
                case "text" -> {
                    if (e.fillEnabled && !s.sprite.isEmpty()) glyph(out, g, s.sprite, s.fill, x, w, h, y);
                    label(out, g, s, x, w, h, y, vars);
                }
                default -> {
                    if (e.fillEnabled || !s.sprite.isEmpty()) glyph(out, g, s.sprite, s.fill, x, w, h, y);
                    if (!s.text.isEmpty() || "input".equals(e.type)) label(out, g, s, x, w, h, y, vars);
                }
            }
        }
        return out.build();
    }

    private void glyph(TextComponent.Builder out, GlyphRegistry g, String sprite, String fill, int x, int w, int h, int y) {
        String key = sprite == null || sprite.isEmpty() ? "#pixel" : GlyphRegistry.spriteKey(def.id, sprite);
        GlyphRegistry.HudGlyph hg = g.hud(key, w, h, y);
        if (hg == null) return;
        TextColor color = "#pixel".equals(key) ? Text.color(fill, 0xFFFFFF) : TextColor.color(0xFFFFFF);
        out.append(space(x));
        out.append(Component.text(String.valueOf(hg.ch())).font(GlyphRegistry.FONT_HUD).color(color)
                .shadowColor(ShadowColor.none()));
        out.append(space(-(x + hg.advance())));
    }

    /** Progress fill built from power-of-two column glyphs. */
    private void bar(TextComponent.Builder out, GlyphRegistry g, String color, int x, int fw, int h, int y) {
        if (fw <= 0) return;
        out.append(space(x));
        int remaining = fw, drawn = 0;
        TextColor c = Text.color(color, 0xFF8FD1);
        for (int p = Integer.highestOneBit(Math.max(1, fw)); p >= 1; p >>= 1) {
            while (remaining >= p) {
                GlyphRegistry.HudGlyph hg = g.hud("#pixel", p, h, y);
                if (hg == null) break;
                out.append(Component.text(String.valueOf(hg.ch())).font(GlyphRegistry.FONT_HUD).color(c)
                        .shadowColor(ShadowColor.none()));
                out.append(space(-1)); // glyph advance is width+1
                remaining -= p;
                drawn += p;
            }
        }
        out.append(space(-(x + drawn)));
    }

    private void label(TextComponent.Builder out, GlyphRegistry g, ElementState s, int x, int w, int h, int y,
                       Map<String, String> vars) {
        String raw = s.text;
        boolean placeholder = "input".equals(s.def.type) && (raw == null || raw.isEmpty());
        if (placeholder) raw = s.def.placeholder;
        if (raw == null || raw.isEmpty()) return;
        String resolved = Text.apply(raw, player, vars);
        Component parsed = Text.mm(resolved);
        String plain = Text.plain(parsed);
        int ty = HudLayout.labelY(y, h);
        Key font = g.hudTextFont(ty);
        if (font == null) return;
        int tw = FontWidths.width(plain, false);
        int start = switch (s.def.align) {
            case "left" -> x + ("text".equals(s.def.type) ? 0 : 4);
            case "right" -> x + w - tw - ("text".equals(s.def.type) ? 0 : 4);
            default -> x + (w - tw) / 2;
        };
        TextColor color = placeholder ? TextColor.color(0x9a9a9a) : Text.color(s.effectiveTextColor(), 0xFFFFFF);
        Component text = parsed.colorIfAbsent(color).font(font);
        if (!s.def.shadow) text = text.shadowColor(ShadowColor.none());
        out.append(space(start));
        out.append(text);
        out.append(space(-(start + tw)));
    }

    private static Component space(int px) {
        if (px == 0) return Component.empty();
        return Component.text(GlyphRegistry.shift(px)).font(GlyphRegistry.FONT_SPACE);
    }

    // ------------------------------------------------------------------ UiInstance

    @Override
    public UiDefinition def() {
        return def;
    }

    @Override
    public ElementState state(String elementId) {
        return states.get(elementId);
    }

    @Override
    public java.util.Collection<ElementState> states() {
        return states.values();
    }

    @Override
    public void refresh(String elementId, int interpolationTicks) {
        dirty = true;
    }

    @Override
    public void playAnimation(String idOrName) {
        animations.play(idOrName);
    }

    @Override
    public void stopAnimation(String idOrName) {
        animations.stop(idOrName);
    }

    @Override
    public void close(Player p) {
        plugin.hud().hide(player, def.id);
    }

    @Override
    public boolean isActive() {
        return active && player.isOnline();
    }

    void deactivate() {
        active = false;
    }

    @Override
    public void focusInput(Player p, String elementId) {
        plugin.manager().beginInput(p, this, elementId);
    }

    public void submitInput(Player p, String elementId, String text) {
        ElementState s = states.get(elementId);
        if (s == null) return;
        s.text = text;
        plugin.vars(p).put("input_" + elementId, text);
        dirty = true;
        plugin.scripts().fireElement(this, p, "on_text", elementId, Map.of("text", text));
    }

    public Player player() {
        return player;
    }
}
