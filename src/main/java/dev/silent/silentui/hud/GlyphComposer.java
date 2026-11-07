package dev.silent.silentui.hud;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.render.ElementState;
import dev.silent.silentui.util.FontWidths;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

/**
 * Builds one line of text that draws a whole 2D layout: every element is a pixel-exact glyph (or text in a font
 * shifted to its y), wrapped in negative/positive spaces so it lands at its x and takes zero net width.
 * Used for the HUD bossbar title (origin = screen centre) and for chest GUI titles (origin = title start, x=8).
 */
public final class GlyphComposer {

    private GlyphComposer() {}

    public static Component compose(UiDefinition def, Collection<ElementState> states, Player player, Map<String, String> vars,
                                    GlyphRegistry.GlyphSet set, int originX, int yShift, Map<String, TreeSet<Integer>> yValues) {
        TextComponent.Builder out = Component.text();
        for (ElementState s : states) {
            if (!s.visible || s.opacity <= 0.02) continue;
            ElementDef e = s.def;
            int x = HudLayout.round(s.x) - originX;
            int w = HudLayout.round(s.w), h = HudLayout.round(s.h);
            int y = HudLayout.nearest(yValues.get(e.id), HudLayout.round(s.y));
            switch (e.type) {
                case "item", "head", "model" -> { }
                case "progress" -> {
                    if (e.fillEnabled) glyph(out, def, set, e.sprite, s.fill, e.tintSprite, x, w, h, y + yShift);
                    double v = Text.num(Text.apply(s.value, player, vars), 0);
                    double m = Text.num(Text.apply(s.max, player, vars), 100);
                    int fw = m <= 0 ? 0 : (int) Math.round(w * Math.max(0, Math.min(1, v / m)));
                    bar(out, set, e.barColor, x, fw, h, y + yShift);
                }
                case "text" -> {
                    if (e.fillEnabled && !s.sprite.isEmpty()) glyph(out, def, set, s.sprite, s.effectiveFill(), e.tintSprite, x, w, h, y + yShift);
                    label(out, set, s, x, w, h, y, yShift, player, vars);
                }
                default -> {
                    String sprite = s.effectiveSprite();
                    if (e.fillEnabled || !sprite.isEmpty()) glyph(out, def, set, sprite, s.effectiveFill(), e.tintSprite, x, w, h, y + yShift);
                    if (!s.text.isEmpty() || "input".equals(e.type)) label(out, set, s, x, w, h, y, yShift, player, vars);
                }
            }
        }
        return out.build();
    }

    private static void glyph(TextComponent.Builder out, UiDefinition def, GlyphRegistry.GlyphSet set, String sprite, String fill,
                              boolean tint, int x, int w, int h, int effY) {
        String key = sprite == null || sprite.isEmpty() ? "#pixel" : GlyphRegistry.spriteKey(def.id, sprite);
        GlyphRegistry.HudGlyph hg = set.get(key, w, h, effY);
        if (hg == null) return;
        TextColor color = "#pixel".equals(key) || tint ? Text.color(fill, 0xFFFFFF) : TextColor.color(0xFFFFFF);
        out.append(space(x));
        out.append(Component.text(String.valueOf(hg.ch())).font(set.font()).color(color).shadowColor(ShadowColor.none()));
        out.append(space(-(x + hg.advance())));
    }

    /** Progress fill built from power-of-two column glyphs. */
    private static void bar(TextComponent.Builder out, GlyphRegistry.GlyphSet set, String color, int x, int fw, int h, int effY) {
        if (fw <= 0) return;
        out.append(space(x));
        int remaining = fw, drawn = 0;
        TextColor c = Text.color(color, 0xFFFFFF);
        for (int p = Integer.highestOneBit(Math.max(1, fw)); p >= 1; p >>= 1) {
            while (remaining >= p) {
                GlyphRegistry.HudGlyph hg = set.get("#pixel", p, h, effY);
                if (hg == null) break;
                out.append(Component.text(String.valueOf(hg.ch())).font(set.font()).color(c).shadowColor(ShadowColor.none()));
                out.append(space(-1)); // glyph advance is width+1
                remaining -= p;
                drawn += p;
            }
            if (remaining >= p) break;
        }
        out.append(space(-(x + drawn)));
    }

    private static void label(TextComponent.Builder out, GlyphRegistry.GlyphSet set, ElementState s, int x, int w, int h, int y, int yShift,
                              Player player, Map<String, String> vars) {
        String raw = s.text;
        boolean placeholder = "input".equals(s.def.type) && (raw == null || raw.isEmpty());
        if (placeholder) raw = s.def.placeholder;
        if (raw == null || raw.isEmpty()) return;
        String resolved = Text.apply(raw, player, vars);
        Component parsed = Text.mm(resolved);
        String plain = Text.plain(parsed);
        Key font = set.textFont(HudLayout.labelY(y, h) + yShift);
        if (font == null) return;
        int tw = FontWidths.width(plain, false);
        int pad = "text".equals(s.def.type) ? 0 : 4;
        int start = switch (s.def.align) {
            case "left" -> x + pad;
            case "right" -> x + w - tw - pad;
            default -> x + (w - tw) / 2;
        };
        TextColor color = placeholder ? TextColor.color(0x9a9a9a) : Text.color(s.effectiveTextColor(), 0xFFFFFF);
        Component text = parsed.colorIfAbsent(color).font(font);
        if (!s.def.shadow) text = text.shadowColor(ShadowColor.none());
        out.append(space(start));
        out.append(text);
        out.append(space(-(start + tw)));
    }

    public static Component space(int px) {
        if (px == 0) return Component.empty();
        return Component.text(GlyphRegistry.shift(px)).font(GlyphRegistry.FONT_SPACE);
    }
}
