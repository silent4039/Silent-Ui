package dev.silent.silentui.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Loads and holds every UI definition from plugins/SilentUI/uis. */
public final class UiRegistry {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private final Path folder;
    private final Logger log;
    private final Map<String, UiDefinition> uis = new LinkedHashMap<>();

    public UiRegistry(Path folder, Logger log) {
        this.folder = folder;
        this.log = log;
    }

    public Path folder() { return folder; }

    public void load() throws IOException {
        uis.clear();
        Files.createDirectories(folder);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : files) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    UiDefinition def = GSON.fromJson(r, UiDefinition.class);
                    if (def == null) continue;
                    String problem = sanitize(def, file.getFileName().toString());
                    if (problem != null) {
                        log.warning("[SilentUI] Skipping " + file.getFileName() + ": " + problem);
                        continue;
                    }
                    if (uis.containsKey(def.id)) {
                        log.warning("[SilentUI] Duplicate UI id '" + def.id + "' in " + file.getFileName() + ", skipped.");
                        continue;
                    }
                    uis.put(def.id, def);
                } catch (JsonParseException | IllegalStateException ex) {
                    log.warning("[SilentUI] Invalid JSON in " + file.getFileName() + ": " + ex.getMessage());
                }
            }
        }
        log.info("[SilentUI] Loaded " + uis.size() + " UI(s).");
    }

    /** Saves a definition (used by the editor's "Push to server"). Returns the written path. */
    public Path save(UiDefinition def) throws IOException {
        String problem = sanitize(def, "upload");
        if (problem != null) throw new IOException(problem);
        Files.createDirectories(folder);
        Path file = folder.resolve(def.id + ".json");
        Files.writeString(file, GSON.toJson(def), StandardCharsets.UTF_8);
        return file;
    }

    public UiDefinition get(String id) {
        return id == null ? null : uis.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<UiDefinition> all() {
        return Collections.unmodifiableCollection(uis.values());
    }

    /** Deep copy (used by the in-game editor's working copy and undo). */
    public static UiDefinition copy(UiDefinition def) {
        return GSON.fromJson(GSON.toJson(def), UiDefinition.class);
    }

    /** Validates and cleans a definition that did not come from disk. Returns an error or null. */
    public static String check(UiDefinition def) {
        return sanitize(def, def.id == null ? "ui" : def.id);
    }

    public static String cleanId(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        return s.length() > 48 ? s.substring(0, 48) : s;
    }

    /** Fixes up defaults; returns an error message if the file is unusable. */
    static String sanitize(UiDefinition def, String fileName) {
        if (def.id == null || def.id.isBlank()) def.id = fileName.replace(".json", "");
        def.id = cleanId(def.id);
        if (def.id.isEmpty()) return "missing id";
        if (def.canvas == null) def.canvas = new UiDefinition.Canvas();
        def.canvas.width = Math.max(16, Math.min(2048, def.canvas.width));
        def.canvas.height = Math.max(16, Math.min(2048, def.canvas.height));
        if (def.settings == null) def.settings = new UiDefinition.Settings();
        if (def.sprites == null) def.sprites = new java.util.ArrayList<>();
        if (def.elements == null) def.elements = new java.util.ArrayList<>();
        if (def.animations == null) def.animations = new java.util.ArrayList<>();
        if (def.script == null) def.script = new UiDefinition.ScriptDef();
        if (def.script.nodes == null) def.script.nodes = new java.util.ArrayList<>();
        if (def.tabs == null) def.tabs = new java.util.LinkedHashMap<>();
        if (def.commands == null) def.commands = new java.util.ArrayList<>();
        def.commands.removeIf(c -> c == null || c.name == null || c.name.isBlank());
        for (UiDefinition.CommandDef c : def.commands) {
            c.name = c.name.trim().toLowerCase(Locale.ROOT).replaceFirst("^/+", "").replaceAll("[^a-z0-9_\\-]", "");
            if (c.aliases == null) c.aliases = new java.util.ArrayList<>();
            if (c.args == null) c.args = new java.util.ArrayList<>();
            for (UiDefinition.ArgDef a : c.args) if (a.choices == null) a.choices = new java.util.ArrayList<>();
        }

        Set<String> spriteIds = new HashSet<>();
        def.sprites.removeIf(s -> s == null || s.id == null || s.data == null || s.data.isEmpty());
        for (UiDefinition.SpriteDef s : def.sprites) {
            s.id = cleanId(s.id);
            if (!spriteIds.add(s.id)) return "duplicate sprite id " + s.id;
        }
        Set<String> ids = new HashSet<>();
        int n = 0;
        for (UiDefinition.ElementDef e : def.elements) {
            n++;
            if (e.id == null || e.id.isBlank()) e.id = "el" + n;
            if (!ids.add(e.id)) return "duplicate element id " + e.id;
            if (e.type == null) e.type = "rect";
            e.type = e.type.toLowerCase(Locale.ROOT);
            if (e.group == null) e.group = "";
            if (e.tabFor == null) e.tabFor = "";
            if (e.activeFill == null) e.activeFill = "";
            if (e.activeSprite == null) e.activeSprite = "";
            if (e.activeTextColor == null) e.activeTextColor = "";
            if (e.itemModel == null) e.itemModel = "";
            if (e.sprite == null) e.sprite = "";
            if (e.text == null) e.text = "";
            if (e.hoverFill == null) e.hoverFill = "";
            if (e.hoverSprite == null) e.hoverSprite = "";
            if (e.hoverTextColor == null) e.hoverTextColor = "";
            if (e.barSprite == null) e.barSprite = "";
            if (e.headOwner == null) e.headOwner = "";
            if (e.anchor == null) e.anchor = "center";
            if (e.align == null) e.align = "center";
            e.w = Math.max(1, e.w);
            e.h = Math.max(1, e.h);
            if (e.textSize <= 0) e.textSize = 1;
            if (e.scale <= 0) e.scale = 1;
            if (e.hoverScale <= 0) e.hoverScale = 1;
            e.opacity = Math.max(0, Math.min(1, e.opacity));
        }
        for (UiDefinition.AnimationDef a : def.animations) {
            if (a.tracks == null) a.tracks = new java.util.ArrayList<>();
            a.length = Math.max(1, a.length);
            for (UiDefinition.TrackDef t : a.tracks) {
                if (t.keys == null) t.keys = new java.util.ArrayList<>();
                t.keys.sort((k1, k2) -> Integer.compare(k1.t, k2.t));
            }
        }
        for (UiDefinition.NodeDef node : def.script.nodes) {
            if (node.props == null) node.props = new java.util.HashMap<>();
            if (node.next == null) node.next = new java.util.HashMap<>();
        }
        return null;
    }
}
