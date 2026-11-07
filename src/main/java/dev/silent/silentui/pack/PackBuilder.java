package dev.silent.silentui.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.silent.silentui.hud.HudLayout;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.model.UiDefinition.SpriteDef;
import net.kyori.adventure.key.Key;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates the Silent UI resource pack: sprite fonts for screen/world UIs, pixel-exact HUD glyphs,
 * shifted ASCII fonts for HUD text, a space font for horizontal positioning and invisible bossbar textures.
 */
public final class PackBuilder {

    public record Result(byte[] zip, byte[] sha1, String sha1Hex, GlyphRegistry glyphs) {}

    private final Logger log;
    private final int packFormat;
    private final String bossbarColor;

    private final Map<String, byte[]> files = new LinkedHashMap<>();
    private final GlyphRegistry glyphs = new GlyphRegistry();
    private final JsonArray uiProviders = new JsonArray();
    private final Map<String, BufferedImage> spriteImages = new HashMap<>();
    private char nextUi = '';
    private int imageCounter = 0;

    public PackBuilder(Logger log, int packFormat, String bossbarColor) {
        this.log = log;
        this.packFormat = packFormat;
        this.bossbarColor = bossbarColor.toLowerCase(java.util.Locale.ROOT);
    }

    public Result build(Collection<UiDefinition> uis) throws IOException {
        writeMeta();
        writePixelGlyph();
        writeSpaceFont();
        writeFxFont();
        writeBossbarTextures();
        // invisible item model: clickable chest slots that show nothing
        files.put("assets/silentui/items/blank.json", "{\"model\":{\"type\":\"minecraft:empty\"}}".getBytes(StandardCharsets.UTF_8));
        hudTarget = new Target("hud", glyphs.hudGlyphs, glyphs.hudTextFonts, GlyphRegistry.HUD_BASE_Y + 7);
        guiTarget = new Target("gui", glyphs.guiGlyphs, glyphs.guiTextFonts, GlyphRegistry.GUI_BASELINE);

        for (UiDefinition ui : uis) {
            for (SpriteDef s : ui.sprites) {
                BufferedImage img = decode(s.data);
                if (img == null) {
                    log.warning("[SilentUI] Sprite '" + s.id + "' in UI '" + ui.id + "' is not a valid PNG, skipped.");
                    continue;
                }
                spriteImages.put(GlyphRegistry.spriteKey(ui.id, s.id), img);
                if (!ui.isHud() && !ui.isChest()) addUiSprite(ui.id, s.id, img);
            }
            if (ui.isHud()) addGlyphUi(ui, hudTarget, 0);
            if (ui.isChest()) addGlyphUi(ui, guiTarget, -ui.settings.guiTop);
        }

        files.put("assets/silentui/font/ui.json", fontJson(uiProviders));
        files.put("assets/silentui/font/hud.json", fontJson(hudTarget.providers));
        files.put("assets/silentui/font/gui.json", fontJson(guiTarget.providers));

        byte[] zip = zip();
        byte[] sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1").digest(zip);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : sha1) hex.append(String.format("%02x", b));
        return new Result(zip, sha1, hex.toString(), glyphs);
    }

    // ------------------------------------------------------------------ base files

    private void writeMeta() {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Silent UI");
        pack.addProperty("pack_format", packFormat);
        JsonObject supported = new JsonObject();
        supported.addProperty("min_inclusive", 34);
        supported.addProperty("max_inclusive", 999);
        pack.add("supported_formats", supported);
        // 1.21.9+ style range
        pack.addProperty("min_format", 34);
        pack.addProperty("max_format", 999);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        files.put("pack.mcmeta", root.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writePixelGlyph() throws IOException {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) img.setRGB(x, y, 0xFFFFFFFF);
        String path = "silentui:ui/pixel.png";
        files.put("assets/silentui/textures/ui/pixel.png", png(img));
        uiProviders.add(bitmap(path, GlyphRegistry.UI_GLYPH_HEIGHT, 7, GlyphRegistry.PIXEL));
        glyphs.uiGlyphs.put("#pixel", new GlyphRegistry.UiGlyph(GlyphRegistry.PIXEL, 8f, 9));
    }

    private void writeSpaceFont() {
        JsonObject adv = new JsonObject();
        for (int i = 0; i <= 11; i++) {
            adv.addProperty(String.valueOf(GlyphRegistry.negSpace(i)), -(1 << i));
            adv.addProperty(String.valueOf(GlyphRegistry.posSpace(i)), 1 << i);
        }
        JsonObject space = new JsonObject();
        space.addProperty("type", "space");
        space.add("advances", adv);
        JsonArray providers = new JsonArray();
        providers.add(space);
        files.put("assets/silentui/font/space.json", fontJson(providers));
    }

    private void writeFxFont() throws IOException {
        BufferedImage img = new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) img.setRGB(x, y, 0xFFFFFFFF);
        files.put("assets/silentui/textures/fx/fade.png", png(img));
        JsonArray providers = new JsonArray();
        providers.add(bitmap("silentui:fx/fade.png", 256, 125, GlyphRegistry.FADE));
        files.put("assets/silentui/font/fx.json", fontJson(providers));
    }

    private void writeBossbarTextures() throws IOException {
        BufferedImage clear = new BufferedImage(182, 5, BufferedImage.TYPE_INT_ARGB);
        byte[] data = png(clear);
        files.put("assets/minecraft/textures/gui/sprites/boss_bar/" + bossbarColor + "_background.png", data);
        files.put("assets/minecraft/textures/gui/sprites/boss_bar/" + bossbarColor + "_progress.png", data);
    }

    // ------------------------------------------------------------------ screen / world sprites

    private void addUiSprite(String uiId, String spriteId, BufferedImage src) throws IOException {
        BufferedImage img = keepWidth(copy(src));
        String name = "s" + (imageCounter++);
        files.put("assets/silentui/textures/ui/" + name + ".png", png(img));
        char ch = nextUi++;
        uiProviders.add(bitmap("silentui:ui/" + name + ".png", GlyphRegistry.UI_GLYPH_HEIGHT, 7, ch));
        float drawn = img.getWidth() * (GlyphRegistry.UI_GLYPH_HEIGHT / (float) img.getHeight());
        int advance = (int) (0.5f + drawn) + 1;
        glyphs.uiGlyphs.put(GlyphRegistry.spriteKey(uiId, spriteId), new GlyphRegistry.UiGlyph(ch, drawn, advance));
    }

    // ------------------------------------------------------------------ HUD + chest GUI title glyphs

    /** Where pixel-exact glyphs go: the bossbar HUD (text baseline at y=10) or a chest title (baseline at y=13). */
    private final class Target {
        final String name;
        final JsonArray providers = new JsonArray();
        final Map<String, GlyphRegistry.HudGlyph> map;
        final Map<Integer, Key> textFonts;
        final int baseline;
        char next = '';

        Target(String name, Map<String, GlyphRegistry.HudGlyph> map, Map<Integer, Key> textFonts, int baseline) {
            this.name = name;
            this.map = map;
            this.textFonts = textFonts;
            this.baseline = baseline;
        }
    }

    private Target hudTarget, guiTarget;

    private void addGlyphUi(UiDefinition ui, Target t, int yShift) throws IOException {
        Map<String, TreeSet<Integer>> yValues = HudLayout.yValues(ui);
        TreeSet<Integer> textYs = new TreeSet<>();
        for (ElementDef e : ui.elements) {
            TreeSet<Integer> ys = yValues.getOrDefault(e.id, new TreeSet<>(List.of(HudLayout.round(e.y))));
            int w = HudLayout.round(e.w), h = HudLayout.round(e.h);
            switch (e.type) {
                case "text" -> {
                    if (e.fillEnabled && !e.sprite.isEmpty()) for (int y : ys) glyph(t, ui, e.sprite, w, h, y, yShift);
                    for (int y : ys) textYs.add(HudLayout.labelY(y, h));
                }
                case "progress" -> {
                    for (int y : ys) {
                        if (e.fillEnabled) glyph(t, ui, e.sprite, w, h, y, yShift);
                        for (int p = 1; p <= w; p <<= 1) glyph(t, ui, "", p, h, y, yShift);
                    }
                }
                case "item", "head", "model" -> { /* drawn as real items (chest slots) or not at all (HUD) */ }
                default -> { // rect, image, button, input
                    for (int y : ys) {
                        if (e.fillEnabled || !e.sprite.isEmpty()) glyph(t, ui, e.sprite, w, h, y, yShift);
                        for (String alt : HudLayout.alternateSprites(ui, e)) glyph(t, ui, alt, w, h, y, yShift);
                        if (!e.text.isEmpty() || "input".equals(e.type) || "button".equals(e.type)) {
                            textYs.add(HudLayout.labelY(y, h));
                        }
                    }
                }
            }
        }
        for (int y : textYs) addTextFont(t, y, yShift);
    }

    private void glyph(Target t, UiDefinition ui, String spriteId, int w, int h, int y, int yShift) throws IOException {
        String spriteKey = spriteId == null || spriteId.isEmpty() ? "#pixel" : GlyphRegistry.spriteKey(ui.id, spriteId);
        // keys use the effective (container-relative) y so chest UIs with different guiTop never collide
        String key = GlyphRegistry.hudKey(spriteKey, w, h, y + yShift);
        if (t.map.containsKey(key) || w <= 0 || h <= 0) return;
        BufferedImage base;
        if ("#pixel".equals(spriteKey)) {
            base = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < w; x++) for (int yy = 0; yy < h; yy++) base.setRGB(x, yy, 0xFFFFFFFF);
        } else {
            BufferedImage src = spriteImages.get(spriteKey);
            if (src == null) return;
            base = resize(src, w, h);
        }
        int ascent = t.baseline - (y + yShift);
        int height = h;
        if (ascent > height) {
            // pad with transparent rows so ascent <= height (glyph stays in the same place)
            BufferedImage padded = new BufferedImage(w, ascent, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < w; x++) for (int yy = 0; yy < h; yy++) padded.setRGB(x, yy, base.getRGB(x, yy));
            base = padded;
            height = ascent;
        }
        base = keepWidth(base);
        if (t.next >= '') {
            log.warning("[SilentUI] Too many " + t.name + " glyphs, some pieces will not render.");
            return;
        }
        char ch = t.next++;
        String name = t.name.charAt(0) + "" + (imageCounter++);
        files.put("assets/silentui/textures/" + t.name + "/" + name + ".png", png(base));
        t.providers.add(bitmap("silentui:" + t.name + "/" + name + ".png", height, ascent, ch));
        t.map.put(key, new GlyphRegistry.HudGlyph(ch, w, w + 1));
    }

    private void addTextFont(Target t, int y, int yShift) {
        if (t.textFonts.containsKey(y + yShift)) return;
        int ascent = Math.min(8, t.baseline - (y + yShift));
        JsonArray providers = new JsonArray();
        JsonObject space = new JsonObject();
        space.addProperty("type", "space");
        JsonObject adv = new JsonObject();
        adv.addProperty(" ", 4);
        space.add("advances", adv);
        providers.add(space);

        JsonObject ascii = new JsonObject();
        ascii.addProperty("type", "bitmap");
        ascii.addProperty("file", "minecraft:font/ascii.png");
        ascii.addProperty("ascent", ascent);
        ascii.addProperty("height", 8);
        JsonArray chars = new JsonArray();
        for (int row = 0; row < 16; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < 16; col++) {
                int cp = row * 16 + col;
                boolean ascii7 = row >= 2 && row <= 7 && cp != 0x7F && cp != 0x20;
                sb.append(ascii7 ? (char) cp : '\u0000');
            }
            chars.add(sb.toString());
        }
        ascii.add("chars", chars);
        providers.add(ascii);
        int eff = y + yShift;
        String name = t.name + "_t" + (eff < 0 ? "m" + (-eff) : String.valueOf(eff));
        if (!files.containsKey("assets/silentui/font/" + name + ".json")) {
            files.put("assets/silentui/font/" + name + ".json", fontJson(providers));
        }
        t.textFonts.put(eff, Key.key("silentui", name));
    }

    // ------------------------------------------------------------------ helpers

    private static JsonObject bitmap(String file, int height, int ascent, char ch) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "bitmap");
        o.addProperty("file", file);
        o.addProperty("height", height);
        o.addProperty("ascent", ascent);
        JsonArray chars = new JsonArray();
        chars.add(String.valueOf(ch));
        o.add("chars", chars);
        return o;
    }

    private static byte[] fontJson(JsonArray providers) {
        JsonObject root = new JsonObject();
        root.add("providers", providers);
        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    static BufferedImage decode(String dataUrl) {
        if (dataUrl == null) return null;
        try {
            String b64 = dataUrl;
            int comma = dataUrl.indexOf(',');
            if (dataUrl.startsWith("data:") && comma > 0) b64 = dataUrl.substring(comma + 1);
            byte[] bytes = Base64.getDecoder().decode(b64.trim());
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null || img.getWidth() < 1 || img.getHeight() < 1) return null;
            if (img.getWidth() > 512 || img.getHeight() > 512) {
                double f = 512.0 / Math.max(img.getWidth(), img.getHeight());
                img = resize(img, Math.max(1, (int) (img.getWidth() * f)), Math.max(1, (int) (img.getHeight() * f)));
            }
            return copy(img);
        } catch (IllegalArgumentException | IOException ex) {
            return null;
        }
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < src.getWidth(); x++) for (int y = 0; y < src.getHeight(); y++) out.setRGB(x, y, src.getRGB(x, y));
        return out;
    }

    /** Nearest-neighbour resize (keeps pixel art crisp). */
    static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < out.getWidth(); x++) {
            int sx = Math.min(src.getWidth() - 1, (int) ((x + 0.5) * src.getWidth() / out.getWidth()));
            for (int y = 0; y < out.getHeight(); y++) {
                int sy = Math.min(src.getHeight() - 1, (int) ((y + 0.5) * src.getHeight() / out.getHeight()));
                out.setRGB(x, y, src.getRGB(sx, sy));
            }
        }
        return out;
    }

    /**
     * Minecraft trims fully transparent columns on the right of a bitmap glyph, which would break sizing.
     * A single alpha=1 pixel (invisible in game) in the last column keeps the full width.
     */
    private static BufferedImage keepWidth(BufferedImage img) {
        int x = img.getWidth() - 1;
        for (int y = 0; y < img.getHeight(); y++) if ((img.getRGB(x, y) >>> 24) != 0) return img;
        img.setRGB(x, 0, 0x01000000);
        return img;
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private byte[] zip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            List<String> names = new ArrayList<>(files.keySet());
            for (String name : names) {
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0L); // deterministic hash
                zip.putNextEntry(entry);
                zip.write(files.get(name));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    public static void write(Path file, byte[] data) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(data);
        }
    }
}
