package dev.silent.silentui.util;

/**
 * Advance widths (in font pixels, including the 1px spacing) of the vanilla default font.
 * Needed to position HUD text and left/right aligned text precisely.
 */
public final class FontWidths {

    private FontWidths() {}

    public static int advance(char c) {
        switch (c) {
            case ' ': return 4;
            case '!': case '\'': case ',': case '.': case ':': case ';': case 'i': case '|': return 2;
            case '`': case 'l': return 3;
            case '"': case '(': case ')': case '*': case 'I': case '[': case ']': case 't': case '{': case '}': return 4;
            case '<': case '>': case 'f': case 'k': return 5;
            case '@': case '~': return 7;
            default:
                if (c < 32) return 0;
                return 6;
        }
    }

    /** Width of a plain string in font pixels (the trailing spacing pixel included). */
    public static int width(String s, boolean bold) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int a = advance(c);
            w += a + (bold && c != ' ' ? 1 : 0);
        }
        return w;
    }

    /** Rough line count when text wraps at {@code maxWidth} font pixels. */
    public static int lines(String s, int maxWidth) {
        int lines = 0;
        for (String raw : s.split("\n", -1)) {
            lines++;
            if (maxWidth <= 0) continue;
            int cur = 0;
            for (String word : raw.split(" ")) {
                int ww = width(word, false);
                if (cur > 0 && cur + 4 + ww > maxWidth) {
                    lines++;
                    cur = ww;
                } else {
                    cur += (cur > 0 ? 4 : 0) + ww;
                }
            }
        }
        return Math.max(1, lines);
    }
}
