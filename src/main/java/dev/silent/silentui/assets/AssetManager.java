package dev.silent.silentui.assets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Vanilla client assets (textures, models, item definitions, language) read straight from the official client jar,
 * which is downloaded once from Mojang for the server's exact version and cached in plugins/SilentUI/cache.
 * Nothing from Mojang is bundled in the plugin itself.
 */
public final class AssetManager {

    public enum State { DISABLED, LOADING, READY, FAILED }

    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final Logger log;
    private final Path cacheDir;
    private final String version;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private volatile State state = State.DISABLED;
    private volatile String error = "";
    private volatile ZipFile zip;
    private Map<String, String> lang = Map.of();
    private List<String> items = List.of();
    private final Map<String, BufferedImage> textureCache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) {
            return size() > 1024;
        }
    });
    private final Map<String, byte[]> pngCache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> e) {
            return size() > 3000;
        }
    });
    private final IconRenderer renderer = new IconRenderer(this);

    public AssetManager(Logger log, Path cacheDir, String version) {
        this.log = log;
        this.cacheDir = cacheDir;
        this.version = version;
    }

    public State state() { return state; }
    public String error() { return error; }
    public String version() { return version; }
    public IconRenderer renderer() { return renderer; }

    /** Download (if needed) and open the client jar. Blocking: call from a background thread. */
    public void load() {
        state = State.LOADING;
        try {
            Path jar = ensureJar();
            zip = new ZipFile(jar.toFile());
            lang = readLang();
            items = scanItems();
            state = State.READY;
            log.info("[SilentUI] Minecraft " + version + " assets ready (" + items.size() + " items).");
        } catch (IOException | RuntimeException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            error = e.getMessage() == null ? e.toString() : e.getMessage();
            state = State.FAILED;
            log.warning("[SilentUI] Could not load Minecraft assets: " + error
                    + " (the editor still works, just without vanilla icons; you can drop client-" + version + ".jar into plugins/SilentUI/cache)");
        }
    }

    public void close() {
        try {
            if (zip != null) zip.close();
        } catch (IOException ignored) {
            // closing
        }
    }

    private Path ensureJar() throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Path jar = cacheDir.resolve("client-" + version + ".jar");
        if (Files.exists(jar) && Files.size(jar) > 1_000_000) return jar;
        log.info("[SilentUI] Downloading Minecraft " + version + " client assets from Mojang (one time, about 30 MB)...");
        JsonObject manifest = getJson(MANIFEST);
        String versionUrl = null;
        for (JsonElement el : manifest.getAsJsonArray("versions")) {
            JsonObject v = el.getAsJsonObject();
            if (version.equals(v.get("id").getAsString())) versionUrl = v.get("url").getAsString();
        }
        if (versionUrl == null) throw new IOException("Mojang has no version " + version);
        JsonObject client = getJson(versionUrl).getAsJsonObject("downloads").getAsJsonObject("client");
        String url = client.get("url").getAsString();
        String sha1 = client.get("sha1").getAsString();
        Path tmp = cacheDir.resolve("client-" + version + ".jar.part");
        HttpResponse<Path> res = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(5)).build(),
                HttpResponse.BodyHandlers.ofFile(tmp));
        if (res.statusCode() != 200) throw new IOException("Download failed: HTTP " + res.statusCode());
        String got = sha1(tmp);
        if (!got.equalsIgnoreCase(sha1)) {
            Files.deleteIfExists(tmp);
            throw new IOException("Checksum mismatch for client jar");
        }
        Files.move(tmp, jar, StandardCopyOption.REPLACE_EXISTING);
        return jar;
    }

    private JsonObject getJson(String url) throws IOException, InterruptedException {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode() + " for " + url);
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static String sha1(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    // ------------------------------------------------------------------ raw access

    public byte[] raw(String path) {
        ZipFile z = zip;
        if (z == null) return null;
        ZipEntry e = z.getEntry(path);
        if (e == null) return null;
        try (InputStream in = z.getInputStream(e)) {
            return in.readAllBytes();
        } catch (IOException ex) {
            return null;
        }
    }

    public JsonObject json(String path) {
        byte[] b = raw(path);
        if (b == null) return null;
        try {
            return JsonParser.parseString(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** "minecraft:block/stone" -> assets/minecraft/<kind>/block/stone<ext> */
    static String path(String location, String kind, String ext) {
        String ns = "minecraft", p = location;
        int c = location.indexOf(':');
        if (c >= 0) {
            ns = location.substring(0, c);
            p = location.substring(c + 1);
        }
        return "assets/" + ns + "/" + kind + "/" + p + ext;
    }

    /** Texture by resource location, first animation frame only. */
    public BufferedImage texture(String location) {
        String path = path(location, "textures", ".png");
        BufferedImage cached = textureCache.get(path);
        if (cached != null) return cached;
        byte[] b = raw(path);
        if (b == null) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(b));
            if (img == null) return null;
            if (img.getHeight() > img.getWidth() && img.getHeight() % img.getWidth() == 0) {
                img = img.getSubimage(0, 0, img.getWidth(), img.getWidth()); // animated strip
            }
            BufferedImage argb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            argb.getGraphics().drawImage(img, 0, 0, null);
            textureCache.put(path, argb);
            return argb;
        } catch (IOException e) {
            return null;
        }
    }

    public JsonObject model(String location) {
        return json(path(location, "models", ".json"));
    }

    // ------------------------------------------------------------------ catalogue

    private Map<String, String> readLang() {
        JsonObject o = json("assets/minecraft/lang/en_us.json");
        if (o == null) return Map.of();
        Map<String, String> m = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("item.minecraft.") || k.startsWith("block.minecraft.")) m.put(k, e.getValue().getAsString());
        }
        return m;
    }

    private List<String> scanItems() {
        List<String> out = new ArrayList<>();
        Enumeration<? extends ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            String n = en.nextElement().getName();
            if (n.startsWith("assets/minecraft/items/") && n.endsWith(".json")) {
                out.add(n.substring("assets/minecraft/items/".length(), n.length() - 5));
            }
        }
        if (out.isEmpty()) { // pre-1.21.4 layout
            en = zip.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                if (n.startsWith("assets/minecraft/models/item/") && n.endsWith(".json")) {
                    out.add(n.substring("assets/minecraft/models/item/".length(), n.length() - 5));
                }
            }
        }
        Collections.sort(out);
        return out;
    }

    public List<String> items() {
        return items;
    }

    public String displayName(String id) {
        String n = lang.get("item.minecraft." + id);
        if (n == null) n = lang.get("block.minecraft." + id);
        if (n == null) {
            String s = id.replace('_', ' ');
            n = s.isEmpty() ? id : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        return n;
    }

    /** Texture paths under a folder (block, item, gui/sprites, mob_effect, particle, painting, entity...). */
    public List<String> textures(String folder, String query) {
        ZipFile z = zip;
        if (z == null) return List.of();
        String prefix = "assets/minecraft/textures/" + folder.replaceAll("[^a-z0-9_/]", "") + "/";
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        TreeMap<String, Boolean> out = new TreeMap<>();
        Enumeration<? extends ZipEntry> en = z.entries();
        while (en.hasMoreElements()) {
            String n = en.nextElement().getName();
            if (!n.startsWith(prefix) || !n.endsWith(".png")) continue;
            String rel = n.substring("assets/minecraft/textures/".length(), n.length() - 4);
            if (q.isEmpty() || rel.contains(q)) out.put(rel, true);
            if (out.size() >= 3000) break;
        }
        return new ArrayList<>(out.keySet());
    }

    // ------------------------------------------------------------------ rendered PNGs

    public byte[] icon(String id, int size) {
        String key = "icon:" + id + ":" + size;
        byte[] c = pngCache.get(key);
        if (c != null) return c;
        BufferedImage img = renderer.renderItem(id, size);
        if (img == null) return null;
        byte[] png = png(img);
        pngCache.put(key, png);
        return png;
    }

    /** Texture as PNG (first frame), for the editor's texture browser and sprite import. */
    public byte[] texturePng(String rel) {
        BufferedImage img = texture("minecraft:" + rel);
        return img == null ? null : png(img);
    }

    public byte[] cached(String key) {
        return pngCache.get(key);
    }

    public void cache(String key, byte[] png) {
        pngCache.put(key, png);
    }

    public static byte[] png(BufferedImage img) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ item definition -> model + tints

    /** Result of reading items/<id>.json: which model to draw and the tint per tint index. */
    public record ItemModelRef(String model, int[] tints, boolean special) {}

    public ItemModelRef itemModel(String id) {
        JsonObject def = json("assets/minecraft/items/" + id + ".json");
        if (def == null) return new ItemModelRef("minecraft:item/" + id, new int[0], false);
        return pick(def.getAsJsonObject("model"), id);
    }

    private ItemModelRef pick(JsonObject m, String id) {
        if (m == null) return new ItemModelRef("minecraft:item/" + id, new int[0], false);
        String type = m.has("type") ? m.get("type").getAsString().replace("minecraft:", "") : "model";
        switch (type) {
            case "model" -> {
                return new ItemModelRef(m.get("model").getAsString(), tints(m.getAsJsonArray("tints")), false);
            }
            case "special" -> {
                return new ItemModelRef(m.has("base") ? m.get("base").getAsString() : "minecraft:item/" + id, new int[0], true);
            }
            case "composite" -> {
                JsonArray models = m.getAsJsonArray("models");
                return models != null && !models.isEmpty() ? pick(models.get(0).getAsJsonObject(), id)
                        : new ItemModelRef("minecraft:item/" + id, new int[0], false);
            }
            default -> {
                // condition / select / range_dispatch: prefer the fallback, else the first branch
                if (m.has("fallback")) return pick(m.getAsJsonObject("fallback"), id);
                if (m.has("on_false")) return pick(m.getAsJsonObject("on_false"), id);
                if (m.has("cases")) {
                    JsonArray cases = m.getAsJsonArray("cases");
                    if (!cases.isEmpty()) return pick(cases.get(0).getAsJsonObject().getAsJsonObject("model"), id);
                }
                if (m.has("entries")) {
                    JsonArray entries = m.getAsJsonArray("entries");
                    if (!entries.isEmpty()) return pick(entries.get(0).getAsJsonObject().getAsJsonObject("model"), id);
                }
                return new ItemModelRef("minecraft:item/" + id, new int[0], false);
            }
        }
    }

    private static int[] tints(JsonArray arr) {
        if (arr == null) return new int[0];
        int[] out = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            JsonObject t = arr.get(i).getAsJsonObject();
            String type = t.has("type") ? t.get("type").getAsString().replace("minecraft:", "") : "";
            int color = switch (type) {
                case "constant" -> t.get("value").getAsInt();
                case "grass" -> 0xFF91BD59;
                case "foliage", "dry_foliage" -> 0xFF77AB2F;
                default -> t.has("default") ? t.get("default").getAsInt() : -1;
            };
            out[i] = color;
        }
        return out;
    }
}
