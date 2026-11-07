package dev.silent.silentui.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Tiny built-in web server: serves the resource pack, the editor, and a token-protected
 * API the editor uses to push UIs straight to the server.
 */
public final class PackServer {

    private static final int MAX_UPLOAD = 24 * 1024 * 1024;

    private final Logger log;
    private final int port;
    private final String token;
    private final Supplier<UiRegistry> registry;
    /** Called (async thread) with an uploaded definition; returns an error or null. */
    private final Function<UiDefinition, String> uploadHandler;

    private HttpServer server;
    private volatile byte[] pack = new byte[0];
    private volatile String packHash = "";
    private byte[] editorHtml;

    public PackServer(Logger log, int port, String token, Supplier<UiRegistry> registry,
                      Function<UiDefinition, String> uploadHandler) {
        this.log = log;
        this.port = port;
        this.token = token;
        this.registry = registry;
        this.uploadHandler = uploadHandler;
    }

    public void start() throws IOException {
        try (InputStream in = PackServer.class.getResourceAsStream("/editor/index.html")) {
            String body = in == null ? "<h1>Editor missing from jar</h1>" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // the editor file is a page body (it is also published as a hosted artifact), so wrap it in a document here
            String page = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                    + "</head><body>" + body + "</body></html>";
            editorHtml = page.getBytes(StandardCharsets.UTF_8);
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "SilentUI-Web");
            t.setDaemon(true);
            return t;
        }));
        server.createContext("/", this::handle);
        server.start();
        log.info("[SilentUI] Web server running on port " + port + " (pack + editor).");
    }

    public void stop() {
        if (server != null) server.stop(0);
        server = null;
    }

    public void setPack(byte[] zip, String sha1Hex) {
        this.pack = zip;
        this.packHash = sha1Hex;
    }

    public String packPath() {
        return "/pack/" + packHash + ".zip";
    }

    private void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-SilentUI-Token");
            ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                send(ex, 204, "text/plain", new byte[0]);
                return;
            }
            if (path.startsWith("/pack/") || path.equals("/pack.zip")) {
                ex.getResponseHeaders().add("Cache-Control", "no-cache");
                send(ex, 200, "application/zip", pack);
                return;
            }
            if (path.equals("/") || path.equals("/editor") || path.equals("/editor/")) {
                send(ex, 200, "text/html; charset=utf-8", editorHtml);
                return;
            }
            if (path.startsWith("/api/")) {
                api(ex, path);
                return;
            }
            send(ex, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            log.warning("[SilentUI] Web request failed: " + e);
            send(ex, 500, "text/plain", "Internal error".getBytes(StandardCharsets.UTF_8));
        }
    }

    private void api(HttpExchange ex, String path) throws IOException {
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        String given = ex.getRequestHeaders().getFirst("X-SilentUI-Token");
        if (given == null) given = q.get("token");
        if (token == null || token.isEmpty() || given == null || !constantEquals(token, given)) {
            json(ex, 401, error("Invalid token. Copy it from plugins/SilentUI/config.yml (web.upload-token)."));
            return;
        }
        switch (path) {
            case "/api/ping" -> {
                JsonObject o = new JsonObject();
                o.addProperty("ok", true);
                o.addProperty("server", "SilentUI");
                json(ex, 200, o);
            }
            case "/api/uis" -> {
                JsonArray arr = new JsonArray();
                for (UiDefinition d : registry.get().all()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("id", d.id);
                    o.addProperty("name", d.name);
                    o.addProperty("type", d.type);
                    arr.add(o);
                }
                JsonObject o = new JsonObject();
                o.add("uis", arr);
                json(ex, 200, o);
            }
            case "/api/ui" -> {
                UiDefinition d = registry.get().get(q.get("id"));
                if (d == null) {
                    json(ex, 404, error("Unknown UI"));
                    return;
                }
                send(ex, 200, "application/json", UiRegistry.GSON.toJson(d).getBytes(StandardCharsets.UTF_8));
            }
            case "/api/upload" -> {
                if (!"POST".equals(ex.getRequestMethod())) {
                    json(ex, 405, error("POST only"));
                    return;
                }
                byte[] body = readLimited(ex.getRequestBody());
                if (body == null) {
                    json(ex, 413, error("Upload too large"));
                    return;
                }
                UiDefinition def;
                try {
                    def = UiRegistry.GSON.fromJson(new String(body, StandardCharsets.UTF_8), UiDefinition.class);
                } catch (RuntimeException e) {
                    json(ex, 400, error("Invalid JSON: " + e.getMessage()));
                    return;
                }
                if (def == null) {
                    json(ex, 400, error("Empty body"));
                    return;
                }
                String err = uploadHandler.apply(def);
                if (err != null) {
                    json(ex, 400, error(err));
                    return;
                }
                JsonObject o = new JsonObject();
                o.addProperty("ok", true);
                o.addProperty("id", def.id);
                json(ex, 200, o);
            }
            default -> json(ex, 404, error("Unknown endpoint"));
        }
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        byte[] data = in.readNBytes(MAX_UPLOAD + 1);
        return data.length > MAX_UPLOAD ? null : data;
    }

    private static boolean constantEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject error(String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", false);
        o.addProperty("error", msg);
        return o;
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String part : raw.split("&")) {
            int i = part.indexOf('=');
            if (i <= 0) continue;
            m.put(URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    private static void json(HttpExchange ex, int code, JsonObject o) throws IOException {
        send(ex, code, "application/json", o.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        if (code == 204) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
