package dev.silent.silentui.assets;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.silent.silentui.model.UiRegistry;
import dev.silent.silentui.pack.PackServer;
import dev.silent.silentui.pack.PackServer.Reply;
import dev.silent.silentui.util.ProfileCache;
import org.bukkit.profile.PlayerTextures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web routes for the editor:
 * <pre>
 *   GET  /assets/status                 asset loading state
 *   GET  /assets/items                  [{id,name}] every item in this version
 *   GET  /assets/icon/{id}.png?size=64  rendered inventory icon (3D blocks, flat items)
 *   GET  /assets/textures?folder=block&q=stone
 *   GET  /assets/texture/{path}.png     any vanilla texture (first frame)
 *   GET  /assets/head/{name}.png        3D head from the player's skin
 *   GET  /assets/body/{name}.png        front view of the player's skin
 *   GET  /assets/skin/{name}.png        raw skin
 *   GET  /api/presets                   saved presets           (token)
 *   POST /api/presets                   save a preset            (token)
 *   POST /api/presets/delete?id=x       delete a preset          (token)
 * </pre>
 */
public final class AssetApi {

    private final AssetManager assets;
    private final ProfileCache profiles;
    private final Path presetDir;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private final Map<String, Skin> skins = new ConcurrentHashMap<>();
    private long lastLookup;

    private record Skin(BufferedImage image, boolean slim, long time) {}

    public AssetApi(AssetManager assets, ProfileCache profiles, Path presetDir) {
        this.assets = assets;
        this.profiles = profiles;
        this.presetDir = presetDir;
    }

    public void register(PackServer server) {
        server.route("/assets/", (method, path, q, body, authed) -> assets(path, q));
        server.route("/api/presets", (method, path, q, body, authed) -> {
            if (!authed) return Reply.json(401, "{\"ok\":false,\"error\":\"Invalid token\"}");
            return presets(method, path, q, body);
        });
    }

    private Reply assets(String path, Map<String, String> q) {
        String rest = path.substring("/assets/".length());
        int size = parseSize(q.get("size"), 64);
        if (rest.equals("status")) {
            JsonObject o = new JsonObject();
            o.addProperty("state", assets.state().name().toLowerCase(Locale.ROOT));
            o.addProperty("version", assets.version());
            o.addProperty("error", assets.error());
            return Reply.json(200, o.toString());
        }
        if (rest.startsWith("head/") || rest.startsWith("body/") || rest.startsWith("skin/")) {
            String name = rest.substring(5).replace(".png", "");
            if (!name.matches("[A-Za-z0-9_]{1,16}")) return Reply.json(400, "{\"error\":\"bad name\"}");
            String key = rest.substring(0, 4) + ":" + name.toLowerCase(Locale.ROOT) + ":" + size;
            byte[] cached = assets.cached(key);
            if (cached != null) return Reply.png(cached, 600);
            Skin skin = skin(name);
            BufferedImage img;
            if (skin == null) {
                BufferedImage steve = assets.texture("minecraft:entity/player/wide/steve");
                if (steve == null) return null;
                skin = new Skin(steve, false, 0);
            }
            img = switch (rest.substring(0, 4)) {
                case "head" -> assets.renderer().renderHead(skin.image(), size);
                case "body" -> assets.renderer().renderBody(skin.image(), skin.slim(), size);
                default -> skin.image();
            };
            byte[] png = AssetManager.png(img);
            assets.cache(key, png);
            return Reply.png(png, 600);
        }
        if (assets.state() != AssetManager.State.READY) {
            return Reply.json(503, "{\"error\":\"Minecraft assets are " + assets.state().name().toLowerCase(Locale.ROOT) + "\"}");
        }
        if (rest.equals("items")) {
            JsonArray arr = new JsonArray();
            for (String id : assets.items()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", id);
                o.addProperty("name", assets.displayName(id));
                arr.add(o);
            }
            return Reply.json(200, arr.toString());
        }
        if (rest.startsWith("icon/")) {
            String id = rest.substring(5).replace(".png", "").replace("minecraft:", "");
            if (!id.matches("[a-z0-9_]{1,64}")) return null;
            byte[] png = assets.icon(id, size);
            return png == null ? null : Reply.png(png, 86400);
        }
        if (rest.equals("textures")) {
            JsonArray arr = new JsonArray();
            for (String t : assets.textures(q.getOrDefault("folder", "item"), q.get("q"))) arr.add(t);
            return Reply.json(200, arr.toString());
        }
        if (rest.startsWith("texture/")) {
            String rel = rest.substring(8).replace(".png", "");
            if (!rel.matches("[a-z0-9_/.\\-]{1,128}") || rel.contains("..")) return null;
            byte[] png = assets.texturePng(rel);
            return png == null ? null : Reply.png(png, 86400);
        }
        return null;
    }

    private static int parseSize(String s, int def) {
        try {
            return s == null ? def : Math.max(16, Math.min(256, Integer.parseInt(s)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Skin lookups go to Mojang, so they are cached for 30 minutes and spaced out a little. */
    private Skin skin(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Skin s = skins.get(key);
        if (s != null && System.currentTimeMillis() - s.time() < 30 * 60 * 1000L) return s;
        synchronized (this) {
            long wait = 300 - (System.currentTimeMillis() - lastLookup);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            lastLookup = System.currentTimeMillis();
        }
        PlayerProfile profile = profiles.getBlocking(name);
        if (profile == null) return null;
        PlayerTextures tex = profile.getTextures();
        URL url = tex.getSkin();
        if (url == null) return null;
        try {
            HttpResponse<byte[]> res = http.send(HttpRequest.newBuilder(URI.create(url.toString().replace("http://", "https://")))
                    .timeout(Duration.ofSeconds(15)).build(), HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) return null;
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(res.body()));
            if (img == null) return null;
            Skin skin = new Skin(img, tex.getSkinModel() == PlayerTextures.SkinModel.SLIM, System.currentTimeMillis());
            skins.put(key, skin);
            return skin;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return null;
        }
    }

    // ------------------------------------------------------------------ presets

    private Reply presets(String method, String path, Map<String, String> q, byte[] body) throws IOException {
        Files.createDirectories(presetDir);
        if (path.equals("/api/presets/delete")) {
            String id = UiRegistry.cleanId(q.get("id"));
            if (id.isEmpty()) return Reply.json(400, "{\"ok\":false,\"error\":\"missing id\"}");
            Files.deleteIfExists(presetDir.resolve(id + ".json"));
            return Reply.json(200, "{\"ok\":true}");
        }
        if ("POST".equals(method)) {
            JsonObject o;
            try {
                o = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (RuntimeException e) {
                return Reply.json(400, "{\"ok\":false,\"error\":\"invalid json\"}");
            }
            String id = UiRegistry.cleanId(o.has("id") ? o.get("id").getAsString() : o.has("name") ? o.get("name").getAsString() : "");
            if (id.isEmpty()) return Reply.json(400, "{\"ok\":false,\"error\":\"preset needs a name\"}");
            o.addProperty("id", id);
            Files.writeString(presetDir.resolve(id + ".json"), o.toString(), StandardCharsets.UTF_8);
            return Reply.json(200, "{\"ok\":true,\"id\":\"" + id + "\"}");
        }
        JsonArray arr = new JsonArray();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(presetDir, "*.json")) {
            for (Path p : ds) {
                try {
                    arr.add(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)));
                } catch (RuntimeException ignored) {
                    // skip broken preset
                }
            }
        }
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.add("presets", arr);
        return Reply.json(200, out.toString());
    }
}
