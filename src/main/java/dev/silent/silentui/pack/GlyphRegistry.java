package dev.silent.silentui.pack;

import net.kyori.adventure.key.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps sprites / HUD pieces to the private-use characters the generated resource pack defines.
 * Built by {@link PackBuilder}, read by the renderers.
 */
public final class GlyphRegistry {

    public static final Key FONT_UI = Key.key("silentui", "ui");
    public static final Key FONT_HUD = Key.key("silentui", "hud");
    public static final Key FONT_SPACE = Key.key("silentui", "space");
    public static final Key FONT_FX = Key.key("silentui", "fx");

    /** Height every screen/world glyph is normalised to, in font pixels. */
    public static final int UI_GLYPH_HEIGHT = 8;
    /** Bossbar title text top on screen (GUI pixels). */
    public static final int HUD_BASE_Y = 3;

    public static final char PIXEL = '';
    public static final char FADE = '';

    /** Screen glyph info: character + drawn width in font pixels at height 8. */
    public record UiGlyph(char ch, float drawnWidth, int advance) {}

    /** HUD glyph: character + advance in GUI pixels. */
    public record HudGlyph(char ch, int width, int advance) {}

    final Map<String, UiGlyph> uiGlyphs = new HashMap<>();
    final Map<String, HudGlyph> hudGlyphs = new HashMap<>();
    final Map<Integer, Key> hudTextFonts = new HashMap<>();

    public static String spriteKey(String uiId, String spriteId) {
        return uiId + ":" + spriteId;
    }

    public static String hudKey(String spriteKey, int w, int h, int y) {
        return spriteKey + "|" + w + "|" + h + "|" + y;
    }

    public UiGlyph pixel() {
        return uiGlyphs.get("#pixel");
    }

    public UiGlyph sprite(String uiId, String spriteId) {
        if (spriteId == null || spriteId.isEmpty()) return null;
        return uiGlyphs.get(spriteKey(uiId, spriteId));
    }

    public HudGlyph hud(String spriteKey, int w, int h, int y) {
        return hudGlyphs.get(hudKey(spriteKey, w, h, y));
    }

    /** Font that renders default ASCII text with its top at screen y, or null if not generated. */
    public Key hudTextFont(int y) {
        return hudTextFonts.get(y);
    }

    /** Characters of the space font: index i shifts by -(2^i) or +(2^i). */
    public static char negSpace(int power) { return (char) (0xE000 + power); }
    public static char posSpace(int power) { return (char) (0xE020 + power); }

    /** String of space-font characters that moves the cursor by {@code px} GUI pixels. */
    public static String shift(int px) {
        if (px == 0) return "";
        StringBuilder sb = new StringBuilder();
        boolean neg = px < 0;
        int v = Math.abs(px);
        for (int i = 11; i >= 0; i--) {
            int p = 1 << i;
            while (v >= p) {
                sb.append(neg ? negSpace(i) : posSpace(i));
                v -= p;
            }
        }
        return sb.toString();
    }
}
