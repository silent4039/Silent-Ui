package dev.silent.silentui.assets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws inventory-style icons from vanilla models: flat items from their layers, blocks as isometric cuboids using
 * the model's own GUI display transform (so slabs, stairs, fences, walls and so on look like they do in the inventory),
 * and player heads / bodies from skins.
 */
public final class IconRenderer {

    /** Inventory lighting per face direction. */
    private static final Map<String, Double> SHADE = Map.of("up", 1.0, "down", 0.5, "north", 0.8, "south", 0.8, "east", 0.6, "west", 0.6);

    private final AssetManager assets;

    IconRenderer(AssetManager assets) {
        this.assets = assets;
    }

    /** Model after following "parent" links. */
    static final class Resolved {
        final Map<String, String> textures = new HashMap<>();
        JsonArray elements;
        float[] rotation, translation, scale;
        boolean generated, builtinEntity;
    }

    Resolved resolve(String location) {
        Resolved r = new Resolved();
        String loc = location;
        for (int depth = 0; depth < 16 && loc != null; depth++) {
            String plain = loc.replace("minecraft:", "");
            if (plain.equals("builtin/generated") || plain.equals("item/generated") || plain.equals("item/handheld")
                    || plain.equals("item/handheld_rod") || plain.equals("item/handheld_mace")) {
                if (!plain.startsWith("builtin")) {
                    JsonObject m = assets.model(loc); // still pick up display + textures of the base
                    if (m != null) merge(r, m);
                }
                r.generated = true;
                break;
            }
            if (plain.equals("builtin/entity")) {
                r.builtinEntity = true;
                break;
            }
            JsonObject m = assets.model(loc);
            if (m == null) return depth == 0 ? null : r;
            merge(r, m);
            loc = m.has("parent") ? m.get("parent").getAsString() : null;
        }
        return r;
    }

    private static void merge(Resolved r, JsonObject m) {
        if (m.has("textures")) {
            for (Map.Entry<String, JsonElement> e : m.getAsJsonObject("textures").entrySet()) {
                r.textures.putIfAbsent(e.getKey(), e.getValue().getAsString());
            }
        }
        if (r.elements == null && m.has("elements")) r.elements = m.getAsJsonArray("elements");
        if (r.rotation == null && m.has("display")) {
            JsonObject gui = m.getAsJsonObject("display").getAsJsonObject("gui");
            if (gui != null) {
                r.rotation = floats(gui.getAsJsonArray("rotation"), 0);
                r.translation = floats(gui.getAsJsonArray("translation"), 0);
                r.scale = floats(gui.getAsJsonArray("scale"), 1);
            }
        }
    }

    private static float[] floats(JsonArray a, float def) {
        float[] f = {def, def, def};
        if (a != null) for (int i = 0; i < 3 && i < a.size(); i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    String texRef(Resolved r, String ref) {
        String t = ref;
        for (int i = 0; i < 8 && t != null && t.startsWith("#"); i++) t = r.textures.get(t.substring(1));
        return t;
    }

    // ------------------------------------------------------------------ items

    public BufferedImage renderItem(String id, int size) {
        size = Math.max(16, Math.min(256, size));
        if (id.equals("player_head")) {
            BufferedImage steve = assets.texture("minecraft:entity/player/wide/steve");
            if (steve != null) return renderHead(steve, size);
        }
        switch (id) {
            case "chest", "trapped_chest", "ender_chest", "christmas_chest" -> {
                String tex = id.equals("ender_chest") ? "ender" : id.equals("trapped_chest") ? "trapped" : "normal";
                if (assets.texture("minecraft:entity/chest/" + tex) != null) return chest("minecraft:entity/chest/" + tex, size);
            }
            case "shield" -> {
                BufferedImage s = assets.texture("minecraft:entity/shield_base_nopattern");
                if (s != null && s.getWidth() >= 14) {
                    int k = s.getWidth() / 64;
                    return scale(s.getSubimage(k, k, 12 * k, 22 * k), size);
                }
            }
            default -> { }
        }
        AssetManager.ItemModelRef ref = assets.itemModel(id);
        Resolved r = resolve(ref.model());
        if (r == null || r.builtinEntity || (r.elements == null && !r.generated)) {
            // special items (chests, beds, banners, shields...): try a flat texture, else a cube of the particle texture
            BufferedImage flat = assets.texture("minecraft:item/" + id);
            if (flat != null) return scale(flat, size);
            if (r != null && r.textures.containsKey("particle")) return cube(texRef(r, "#particle"), size);
            BufferedImage block = assets.texture("minecraft:block/" + id);
            return block == null ? null : cube("minecraft:block/" + id, size);
        }
        if (r.generated || r.elements == null) {
            BufferedImage out = null;
            for (int layer = 0; layer < 5; layer++) {
                String t = texRef(r, "#layer" + layer);
                if (t == null) break;
                BufferedImage tex = assets.texture(t);
                if (tex == null) continue;
                int tint = layer < ref.tints().length ? ref.tints()[layer] : -1;
                if (tint != -1) tex = tint(tex, tint, 1.0);
                if (out == null) out = new BufferedImage(tex.getWidth(), tex.getHeight(), BufferedImage.TYPE_INT_ARGB);
                out.getGraphics().drawImage(tex, 0, 0, out.getWidth(), out.getHeight(), null);
            }
            return out == null ? null : scale(out, size);
        }
        return renderElements(r, ref.tints(), size);
    }

    private BufferedImage cube(String texture, int size) {
        Resolved r = new Resolved();
        r.textures.put("all", texture);
        JsonArray els = new JsonArray();
        JsonObject el = new JsonObject();
        el.add("from", arr(0, 0, 0));
        el.add("to", arr(16, 16, 16));
        JsonObject faces = new JsonObject();
        for (String f : new String[]{"up", "down", "north", "south", "east", "west"}) {
            JsonObject face = new JsonObject();
            face.addProperty("texture", "#all");
            faces.add(f, face);
        }
        el.add("faces", faces);
        els.add(el);
        r.elements = els;
        return renderElements(r, new int[0], size);
    }

    /**
     * Chests are entity models: body, lid and latch boxes with UVs from the 64x64 entity texture
     * (the entity is drawn upside down, so faces swap and flip accordingly).
     */
    private BufferedImage chest(String texture, int size) {
        Resolved r = new Resolved();
        r.textures.put("c", texture);
        JsonArray els = new JsonArray();
        // body 14x10x14 at texture (0,19)
        els.add(box(1, 0, 1, 15, 10, 15, Map.of(
                "up", new double[]{28, 33, 42, 19}, "north", new double[]{42, 43, 56, 33}, "south", new double[]{14, 43, 28, 33},
                "west", new double[]{14, 43, 0, 33}, "east", new double[]{42, 43, 28, 33}, "down", new double[]{14, 19, 28, 33})));
        // lid 14x5x14 at texture (0,0)
        els.add(box(1, 9, 1, 15, 14, 15, Map.of(
                "up", new double[]{28, 14, 42, 0}, "north", new double[]{42, 19, 56, 14}, "south", new double[]{14, 19, 28, 14},
                "west", new double[]{14, 19, 0, 14}, "east", new double[]{42, 19, 28, 14})));
        // latch 2x4x1 at texture (0,0)
        els.add(box(7, 7, 0, 9, 11, 1, Map.of(
                "up", new double[]{3, 1, 5, 0}, "north", new double[]{4, 5, 6, 1}, "west", new double[]{1, 5, 0, 1}, "east", new double[]{4, 5, 3, 1})));
        r.elements = els;
        return renderElements(r, new int[0], size);
    }

    /** Box element with per-face UVs given in texture pixels of a 64-wide texture. */
    private static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2, Map<String, double[]> uvs) {
        JsonObject el = new JsonObject();
        el.add("from", arr(x1, y1, z1));
        el.add("to", arr(x2, y2, z2));
        JsonObject faces = new JsonObject();
        for (Map.Entry<String, double[]> e : uvs.entrySet()) {
            JsonObject f = new JsonObject();
            double[] p = e.getValue();
            f.add("uv", arr(p[0] / 4, p[1] / 4, p[2] / 4, p[3] / 4));
            f.addProperty("texture", "#c");
            faces.add(e.getKey(), f);
        }
        el.add("faces", faces);
        return el;
    }

    private static JsonArray arr(double... v) {
        JsonArray a = new JsonArray();
        for (double d : v) a.add(d);
        return a;
    }

    // ------------------------------------------------------------------ 3D

    private record Face(double depth, double[][] pts, BufferedImage tex, double u1, double v1, double u2, double v2, int rot) {}

    private BufferedImage renderElements(Resolved r, int[] tints, int size) {
        float[] rot = r.rotation != null ? r.rotation : new float[]{30, 225, 0};
        float[] tr = r.translation != null ? r.translation : new float[]{0, 0, 0};
        float[] sc = r.scale != null ? r.scale : new float[]{0.625f, 0.625f, 0.625f};
        double[][] m = rotationXYZ(Math.toRadians(rot[0]), Math.toRadians(rot[1]), Math.toRadians(rot[2]));
        double px = size / 16.0; // one slot pixel
        List<Face> faces = new ArrayList<>();
        for (JsonElement je : r.elements) {
            JsonObject el = je.getAsJsonObject();
            double[] from = vec(el.getAsJsonArray("from")), to = vec(el.getAsJsonArray("to"));
            JsonObject elRot = el.has("rotation") ? el.getAsJsonObject("rotation") : null;
            JsonObject fs = el.getAsJsonObject("faces");
            if (fs == null) continue;
            for (Map.Entry<String, JsonElement> fe : fs.entrySet()) {
                String dir = fe.getKey();
                JsonObject f = fe.getValue().getAsJsonObject();
                double[][] corners = corners(dir, from, to); // TL, TR, BL
                double[] normal = normal(dir);
                if (elRot != null) {
                    for (int i = 0; i < 3; i++) corners[i] = rotateElement(corners[i], elRot);
                    normal = rotateElement(normal, elRot, true);
                }
                double[][] screen = new double[3][];
                double depth = 0;
                for (int i = 0; i < 3; i++) {
                    double[] p = {corners[i][0] - 8, corners[i][1] - 8, corners[i][2] - 8};
                    p = mul(m, p);
                    double sx = (p[0] * sc[0] + tr[0]) * px + size / 2.0;
                    double sy = size / 2.0 - (p[1] * sc[1] + tr[1]) * px;
                    screen[i] = new double[]{sx, sy};
                    depth += p[2];
                }
                double[] n = mul(m, normal);
                if (n[2] <= 1e-4) continue; // facing away from the camera
                String texLoc = f.has("texture") ? texRef(r, f.get("texture").getAsString()) : null;
                if (texLoc == null) continue;
                BufferedImage tex = assets.texture(texLoc);
                if (tex == null) continue;
                int tintIndex = f.has("tintindex") ? f.get("tintindex").getAsInt() : -1;
                int color = tintIndex >= 0 && tintIndex < tints.length ? tints[tintIndex] : (tintIndex >= 0 ? 0xFF91BD59 : -1);
                double shade = SHADE.getOrDefault(dir, 1.0);
                // brighter top-lit look like the inventory
                tex = tint(tex, color, shade);
                double[] uv = f.has("uv") ? vec4(f.getAsJsonArray("uv")) : defaultUv(dir, from, to);
                double ku = tex.getWidth() / 16.0, kv = tex.getHeight() / 16.0;
                int frot = f.has("rotation") ? ((f.get("rotation").getAsInt() % 360) + 360) % 360 : 0;
                faces.add(new Face(depth / 3, screen, tex, uv[0] * ku, uv[1] * kv, uv[2] * ku, uv[3] * kv, frot));
            }
        }
        faces.sort((a, b) -> Double.compare(a.depth(), b.depth()));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setComposite(AlphaComposite.SrcOver);
        for (Face f : faces) drawFace(g, f);
        g.dispose();
        return out;
    }

    /** Maps the face's UV rectangle onto the projected parallelogram. */
    private static void drawFace(Graphics2D g, Face f) {
        double[] tl = f.pts()[0], tr = f.pts()[1], bl = f.pts()[2];
        double[] br = {tr[0] + bl[0] - tl[0], tr[1] + bl[1] - tl[1]};
        double[][] quad = {tl, tr, br, bl};
        int k = f.rot() / 90;
        // texture corner i lands on face corner (i + k)
        double[] pTL = quad[k % 4], pTR = quad[(1 + k) % 4], pBL = quad[(3 + k) % 4];
        double du = f.u2() - f.u1(), dv = f.v2() - f.v1();
        if (Math.abs(du) < 1e-6 || Math.abs(dv) < 1e-6) return;
        double ax = (pTR[0] - pTL[0]) / du, ay = (pTR[1] - pTL[1]) / du;
        double bx = (pBL[0] - pTL[0]) / dv, by = (pBL[1] - pTL[1]) / dv;
        double ox = pTL[0] - ax * f.u1() - bx * f.v1(), oy = pTL[1] - ay * f.u1() - by * f.v1();
        AffineTransform t = new AffineTransform(ax, ay, bx, by, ox, oy);
        int u0 = (int) Math.floor(Math.min(f.u1(), f.u2())), v0 = (int) Math.floor(Math.min(f.v1(), f.v2()));
        int uw = (int) Math.ceil(Math.max(f.u1(), f.u2())) - u0, vh = (int) Math.ceil(Math.max(f.v1(), f.v2())) - v0;
        uw = Math.max(1, Math.min(uw, f.tex().getWidth() - Math.max(0, u0)));
        vh = Math.max(1, Math.min(vh, f.tex().getHeight() - Math.max(0, v0)));
        if (u0 < 0 || v0 < 0) return;
        BufferedImage sub = f.tex().getSubimage(u0, v0, uw, vh);
        t.translate(u0, v0);
        // slightly enlarge to hide hairline seams between faces
        g.drawImage(sub, t, null);
    }

    private static double[][] corners(String dir, double[] a, double[] b) {
        double x1 = a[0], y1 = a[1], z1 = a[2], x2 = b[0], y2 = b[1], z2 = b[2];
        return switch (dir) {
            case "up" -> new double[][]{{x1, y2, z1}, {x2, y2, z1}, {x1, y2, z2}};
            case "down" -> new double[][]{{x1, y1, z2}, {x2, y1, z2}, {x1, y1, z1}};
            case "north" -> new double[][]{{x2, y2, z1}, {x1, y2, z1}, {x2, y1, z1}};
            case "south" -> new double[][]{{x1, y2, z2}, {x2, y2, z2}, {x1, y1, z2}};
            case "west" -> new double[][]{{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z1}};
            default -> new double[][]{{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z2}}; // east
        };
    }

    private static double[] normal(String dir) {
        return switch (dir) {
            case "up" -> new double[]{0, 1, 0};
            case "down" -> new double[]{0, -1, 0};
            case "north" -> new double[]{0, 0, -1};
            case "south" -> new double[]{0, 0, 1};
            case "west" -> new double[]{-1, 0, 0};
            default -> new double[]{1, 0, 0};
        };
    }

    private static double[] defaultUv(String dir, double[] a, double[] b) {
        double x1 = a[0], y1 = a[1], z1 = a[2], x2 = b[0], y2 = b[1], z2 = b[2];
        return switch (dir) {
            case "up" -> new double[]{x1, z1, x2, z2};
            case "down" -> new double[]{x1, 16 - z2, x2, 16 - z1};
            case "north" -> new double[]{16 - x2, 16 - y2, 16 - x1, 16 - y1};
            case "south" -> new double[]{x1, 16 - y2, x2, 16 - y1};
            case "west" -> new double[]{z1, 16 - y2, z2, 16 - y1};
            default -> new double[]{16 - z2, 16 - y2, 16 - z1, 16 - y1};
        };
    }

    private static double[] rotateElement(double[] p, JsonObject rot) {
        return rotateElement(p, rot, false);
    }

    private static double[] rotateElement(double[] p, JsonObject rot, boolean direction) {
        double[] o = direction ? new double[]{0, 0, 0} : vec(rot.getAsJsonArray("origin"));
        double ang = Math.toRadians(rot.get("angle").getAsDouble());
        String axis = rot.get("axis").getAsString();
        double x = p[0] - o[0], y = p[1] - o[1], z = p[2] - o[2];
        double c = Math.cos(ang), s = Math.sin(ang);
        double nx = x, ny = y, nz = z;
        switch (axis) {
            case "x" -> { ny = y * c - z * s; nz = y * s + z * c; }
            case "y" -> { nx = x * c + z * s; nz = -x * s + z * c; }
            default -> { nx = x * c - y * s; ny = x * s + y * c; }
        }
        return new double[]{nx + o[0], ny + o[1], nz + o[2]};
    }

    private static double[] vec(JsonArray a) {
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    private static double[] vec4(JsonArray a) {
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(), a.get(3).getAsDouble()};
    }

    /** R = Rx * Ry * Rz, like Quaternionf.rotationXYZ in the game. */
    static double[][] rotationXYZ(double x, double y, double z) {
        double[][] rx = {{1, 0, 0}, {0, Math.cos(x), -Math.sin(x)}, {0, Math.sin(x), Math.cos(x)}};
        double[][] ry = {{Math.cos(y), 0, Math.sin(y)}, {0, 1, 0}, {-Math.sin(y), 0, Math.cos(y)}};
        double[][] rz = {{Math.cos(z), -Math.sin(z), 0}, {Math.sin(z), Math.cos(z), 0}, {0, 0, 1}};
        return mul(mul(rx, ry), rz);
    }

    private static double[][] mul(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j];
        return r;
    }

    private static double[] mul(double[][] m, double[] v) {
        return new double[]{
                m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
                m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
                m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    // ------------------------------------------------------------------ helpers

    static BufferedImage tint(BufferedImage src, int argb, double shade) {
        boolean tinted = argb != -1;
        int tr = 255, tg = 255, tb = 255;
        if (tinted) {
            tr = (argb >> 16) & 255;
            tg = (argb >> 8) & 255;
            tb = argb & 255;
        }
        if (!tinted && shade >= 0.999) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                int a = c >>> 24;
                if (a == 0) continue;
                int r = (int) (((c >> 16) & 255) * tr / 255.0 * shade);
                int g = (int) (((c >> 8) & 255) * tg / 255.0 * shade);
                int b = (int) ((c & 255) * tb / 255.0 * shade);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    static BufferedImage scale(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int w = src.getWidth(), h = src.getHeight();
        double k = Math.min(size / (double) w, size / (double) h);
        int dw = (int) Math.round(w * k), dh = (int) Math.round(h * k);
        g.drawImage(src, (size - dw) / 2, (size - dh) / 2, dw, dh, null);
        g.dispose();
        return out;
    }

    // ------------------------------------------------------------------ skins

    /** Isometric 3D head with the hat layer, like a head in the inventory. */
    public BufferedImage renderHead(BufferedImage skin, int size) {
        BufferedImage s = normalizeSkin(skin);
        // faces of the 8x8x8 head: (u, v) offsets on a 64x64 skin
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        double[][] m = rotationXYZ(Math.toRadians(30), Math.toRadians(225), 0);
        drawSkinBox(g, m, s, 0, 0, 1.0, size, 1.05);
        drawSkinBox(g, m, s, 32, 0, 1.125, size, 1.05); // hat layer, slightly bigger
        g.dispose();
        return out;
    }

    private void drawSkinBox(Graphics2D g, double[][] m, BufferedImage skin, int ou, int ov, double grow, int size, double zoom) {
        double h = 4 * grow; // half extent in head pixels
        double px = size / 16.0 * zoom * 1.25;
        Map<String, int[]> uv = new HashMap<>();
        uv.put("up", new int[]{ou + 8, ov, ou + 16, ov + 8});
        uv.put("north", new int[]{ou + 8, ov + 8, ou + 16, ov + 16});   // face
        uv.put("east", new int[]{ou, ov + 8, ou + 8, ov + 16});        // right side of the head
        uv.put("west", new int[]{ou + 16, ov + 8, ou + 24, ov + 16});
        uv.put("south", new int[]{ou + 24, ov + 8, ou + 32, ov + 16});
        List<Face> faces = new ArrayList<>();
        double[] from = {8 - h, 8 - h, 8 - h}, to = {8 + h, 8 + h, 8 + h};
        for (Map.Entry<String, int[]> e : uv.entrySet()) {
            String dir = e.getKey();
            double[] n = mul(m, normal(dir));
            if (n[2] <= 1e-4) continue;
            double[][] c = corners(dir, from, to);
            double[][] scr = new double[3][];
            double depth = 0;
            for (int i = 0; i < 3; i++) {
                double[] p = mul(m, new double[]{c[i][0] - 8, c[i][1] - 8, c[i][2] - 8});
                scr[i] = new double[]{p[0] * px + size / 2.0, size / 2.0 - p[1] * px};
                depth += p[2];
            }
            int[] r = e.getValue();
            BufferedImage tex = tint(skin, -1, SHADE.get(dir));
            faces.add(new Face(depth / 3, scr, tex, r[0], r[1], r[2], r[3], 0));
        }
        faces.sort((a, b) -> Double.compare(a.depth(), b.depth()));
        for (Face f : faces) drawFace(g, f);
    }

    /** Front view of the whole player (head, body, arms, legs + overlay layers). Width:height = 1:2. */
    public BufferedImage renderBody(BufferedImage skin, boolean slim, int height) {
        BufferedImage s = normalizeSkin(skin);
        BufferedImage flat = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flat.createGraphics();
        int arm = slim ? 3 : 4;
        // base layer
        blit(g, s, 8, 8, 8, 8, 4, 0);            // head
        blit(g, s, 20, 20, 8, 12, 4, 8);         // body
        blit(g, s, 44, 20, arm, 12, 4 - arm, 8); // right arm (viewer's left)
        blit(g, s, 36, 52, arm, 12, 12, 8);      // left arm
        blit(g, s, 4, 20, 4, 12, 4, 20);         // right leg
        blit(g, s, 20, 52, 4, 12, 8, 20);        // left leg
        // overlay layer
        blit(g, s, 40, 8, 8, 8, 4, 0);
        blit(g, s, 20, 36, 8, 12, 4, 8);
        blit(g, s, 44, 36, arm, 12, 4 - arm, 8);
        blit(g, s, 52, 52, arm, 12, 12, 8);
        blit(g, s, 4, 36, 4, 12, 4, 20);
        blit(g, s, 4, 52, 4, 12, 8, 20);
        g.dispose();
        int h = Math.max(32, Math.min(512, height));
        BufferedImage out = new BufferedImage(h / 2, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D o = out.createGraphics();
        o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        o.drawImage(flat, 0, 0, h / 2, h, null);
        o.dispose();
        return out;
    }

    private static void blit(Graphics2D g, BufferedImage s, int sx, int sy, int w, int h, int dx, int dy) {
        if (sx + w > s.getWidth() || sy + h > s.getHeight()) return;
        g.drawImage(s.getSubimage(sx, sy, w, h), dx, dy, null);
    }

    /** Old 64x32 skins get their left limbs mirrored into the 64x64 layout. */
    static BufferedImage normalizeSkin(BufferedImage skin) {
        BufferedImage s = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = s.createGraphics();
        g.drawImage(skin, 0, 0, 64, skin.getHeight() * 64 / skin.getWidth(), null);
        if (skin.getHeight() * 64 / skin.getWidth() == 32) {
            g.drawImage(mirror(s.getSubimage(0, 16, 16, 16)), 16, 48, null);
            g.drawImage(mirror(s.getSubimage(40, 16, 16, 16)), 32, 48, null);
        }
        g.dispose();
        return s;
    }

    private static BufferedImage mirror(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) out.setRGB(src.getWidth() - 1 - x, y, src.getRGB(x, y));
        return out;
    }
}
