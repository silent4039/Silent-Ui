package dev.silent.silentui;

import dev.silent.silentui.model.UiRegistry;
import dev.silent.silentui.pack.PackServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class EditorServerTest {
    @TempDir Path folder;
    private PackServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach void start() throws Exception {
        UiRegistry registry = new UiRegistry(folder, Logger.getLogger("test"));
        registry.load();
        server = new PackServer(Logger.getLogger("test"), 0, "test-token", () -> registry, def -> {
            try { registry.save(def); registry.load(); return null; }
            catch (java.io.IOException e) { return e.getMessage(); }
        });
        server.start();
    }

    @AfterEach void stop() { if (server != null) server.stop(); }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void servesACompleteDocumentAtEveryEditorEntryPoint() throws Exception {
        for (String path : new String[]{"/", "/editor", "/editor/"}) {
            var response = get(path);
            assertEquals(200, response.statusCode());
            assertTrue(response.body().startsWith("<!doctype html>"));
            assertEquals(1, response.body().split("<html", -1).length - 1);
            assertTrue(response.body().contains("src=\"editor.js\""));
            assertEquals("no-cache", response.headers().firstValue("Cache-Control").orElseThrow());
        }
    }

    @Test void servesBundledAssetsWithCorrectMimeTypes() throws Exception {
        for (String prefix : new String[]{"/", "/editor/"}) {
            var css = get(prefix + "editor.css");
            assertEquals(200, css.statusCode());
            assertTrue(css.headers().firstValue("Content-Type").orElseThrow().startsWith("text/css"));
            assertTrue(css.body().contains("#navigator"));
            var js = get(prefix + "editor.js");
            assertEquals(200, js.statusCode());
            assertTrue(js.headers().firstValue("Content-Type").orElseThrow().startsWith("text/javascript"));
            assertEquals("nosniff", js.headers().firstValue("X-Content-Type-Options").orElseThrow());
            assertTrue(js.body().contains("initializeWorkspace"));
        }
        assertEquals(404, get("/editor/pom.xml").statusCode());
    }

    @Test void requiresATokenForUiDataAndUploads() throws Exception {
        assertEquals(401, get("/api/uis").statusCode());
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/api/upload"))
                .header("X-SilentUI-Token", "wrong").POST(HttpRequest.BodyPublishers.ofString("{}")) .build();
        assertEquals(401, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test void uploadsAValidUiAndRejectsInvalidDocuments() throws Exception {
        for (String body : new String[]{"{\"id\":\"menu\"}", "{\"id\":\"menu\",\"elements\":[null]}", "{broken"}) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/api/upload"))
                    .header("X-SilentUI-Token", "test-token").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            int expected = body.equals("{\"id\":\"menu\"}") ? 200 : 400;
            assertEquals(expected, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        }
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/api/ui?id=menu"))
                .header("X-SilentUI-Token", "test-token").GET().build();
        assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
