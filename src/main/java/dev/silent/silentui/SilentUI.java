package dev.silent.silentui;

import dev.silent.silentui.command.DynamicCommands;
import dev.silent.silentui.command.SuiCommand;
import dev.silent.silentui.hud.HudManager;
import dev.silent.silentui.listener.InputListener;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiRegistry;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.pack.PackBuilder;
import dev.silent.silentui.pack.PackServer;
import dev.silent.silentui.render.PlaneUi;
import dev.silent.silentui.render.ScreenSession;
import dev.silent.silentui.script.ScriptRunner;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class SilentUI extends JavaPlugin {

    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("silentui:pack".getBytes(StandardCharsets.UTF_8));
    private static final String[] EXAMPLES = {"example_menu.json", "example_shop.json", "example_hud.json", "example_board.json"};

    private UiRegistry registry;
    private UiManager manager;
    private HudManager hud;
    private ScriptRunner scripts;
    private DynamicCommands dynamicCommands;
    private PackServer server;
    private GlyphRegistry glyphs = new GlyphRegistry();
    private PlaneUi.Settings settings = new PlaneUi.Settings();
    private final Map<UUID, Map<String, String>> vars = new ConcurrentHashMap<>();
    private byte[] packSha1 = new byte[0];
    private String packSha1Hex = "";
    private long tick;
    private BossBar.Color hudColor = BossBar.Color.WHITE;
    private dev.silent.silentui.util.ProfileCache profiles;
    private dev.silent.silentui.assets.AssetManager assets;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureToken();
        readSettings();
        copyExamples();

        registry = new UiRegistry(getDataFolder().toPath().resolve("uis"), getLogger());
        manager = new UiManager(this);
        hud = new HudManager(this);
        scripts = new ScriptRunner(this);
        dynamicCommands = new DynamicCommands(this);
        profiles = new dev.silent.silentui.util.ProfileCache(this);
        assets = new dev.silent.silentui.assets.AssetManager(getLogger(), getDataFolder().toPath().resolve("cache"), Bukkit.getMinecraftVersion());
        if (getConfig().getBoolean("assets.enabled", true)) {
            Thread t = new Thread(assets::load, "SilentUI-Assets");
            t.setDaemon(true);
            t.start();
        }

        cleanupLeftovers();
        try {
            registry.load();
        } catch (IOException e) {
            getLogger().severe("[SilentUI] Could not read uis folder: " + e.getMessage());
        }
        buildPack();
        startServer();

        getServer().getPluginManager().registerEvents(new InputListener(this), this);
        PluginCommand cmd = getCommand("silentui");
        if (cmd != null) {
            SuiCommand sc = new SuiCommand(this);
            cmd.setExecutor(sc);
            cmd.setTabCompleter(sc);
        }
        dynamicCommands.registerAll();
        manager.loadWorlds();

        getServer().getScheduler().runTaskTimer(this, this::tickAll, 1L, 1L);

        for (Player p : Bukkit.getOnlinePlayers()) {
            ScreenSession.removeFreeze(this, p);
            if (packSending()) sendPack(p);
            hud.autoShow(p);
        }
        getLogger().info("[SilentUI] Enabled. Editor: " + (editorUrl() == null ? "disabled" : editorUrl()));
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.closeAll();
            manager.despawnAllWorlds();
        }
        if (hud != null) hud.clear();
        if (dynamicCommands != null) dynamicCommands.unregisterAll();
        if (server != null) server.stop();
        if (assets != null) assets.close();
    }

    private void tickAll() {
        tick++;
        try {
            manager.tick();
            hud.tick(tick);
        } catch (RuntimeException ex) {
            getLogger().warning("[SilentUI] Tick error: " + ex);
            if (tick % 200 == 0) ex.printStackTrace();
        }
    }

    // ------------------------------------------------------------------ config

    private void ensureToken() {
        if (getConfig().getString("web.upload-token", "").isBlank()) {
            byte[] b = new byte[18];
            new SecureRandom().nextBytes(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            getConfig().set("web.upload-token", sb.toString());
            saveConfig();
        }
    }

    private void readSettings() {
        FileConfiguration c = getConfig();
        PlaneUi.Settings s = new PlaneUi.Settings();
        s.screenDistance = clamp(c.getDouble("screen.distance", 1.4), 0.6, 6);
        s.viewFill = clamp(c.getDouble("screen.view-fill", 0.82), 0.1, 1.0);
        s.fov = clamp(c.getDouble("screen.fov", 70), 30, 110);
        s.openClickGrace = Math.max(0, c.getInt("screen.open-click-grace-ticks", 6));
        s.textRefreshTicks = Math.max(1, c.getInt("screen.text-refresh-ticks", 10));
        s.hoverInterpolation = Math.max(0, c.getInt("screen.hover-interpolation", 3));
        s.hudRefreshTicks = Math.max(1, c.getInt("hud.refresh-ticks", 5));
        s.worldInteractRange = clamp(c.getDouble("world-ui.interact-range", 8), 1, 64);
        s.glyphCenterX = c.getDouble("calibration.glyph-center-x", 0.5);
        s.glyphCenterY = c.getDouble("calibration.glyph-center-y", 5.0);
        s.textLineHeight = c.getDouble("calibration.text-line-height", 10.0);
        s.itemFlip = c.getBoolean("calibration.item-flip", true);
        s.layerDepth = clamp(c.getDouble("calibration.layer-depth", 0.0018), 0.0001, 0.05);
        settings = s;
        try {
            hudColor = BossBar.Color.valueOf(c.getString("hud.bossbar-color", "WHITE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            hudColor = BossBar.Color.WHITE;
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void copyExamples() {
        Path uis = getDataFolder().toPath().resolve("uis");
        if (Files.isDirectory(uis)) return; // only on first start
        try {
            Files.createDirectories(uis);
            for (String name : EXAMPLES) {
                try (InputStream in = getResource("uis/" + name)) {
                    if (in != null) Files.copy(in, uis.resolve(name));
                }
            }
        } catch (IOException e) {
            getLogger().warning("[SilentUI] Could not copy example UIs: " + e.getMessage());
        }
    }

    private void cleanupLeftovers() {
        int removed = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e.getScoreboardTags().contains(PlaneUi.TAG)) {
                    e.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) getLogger().info("[SilentUI] Removed " + removed + " leftover UI entities.");
    }

    // ------------------------------------------------------------------ pack

    private void buildPack() {
        try {
            PackBuilder builder = new PackBuilder(getLogger(), getConfig().getInt("resource-pack.pack-format", 75),
                    hudColor.name());
            PackBuilder.Result r = builder.build(registry.all());
            glyphs = r.glyphs();
            packSha1 = r.sha1();
            packSha1Hex = r.sha1Hex();
            PackBuilder.write(getDataFolder().toPath().resolve("output").resolve("SilentUI-pack.zip"), r.zip());
            if (server != null) server.setPack(r.zip(), r.sha1Hex());
            getLogger().info("[SilentUI] Resource pack built (" + (r.zip().length / 1024) + " KB, sha1 " + r.sha1Hex() + ").");
        } catch (IOException | RuntimeException e) {
            getLogger().severe("[SilentUI] Resource pack build failed: " + e);
        }
    }

    private void startServer() {
        if (!getConfig().getBoolean("web.enabled", true)) return;
        server = new PackServer(getLogger(), getConfig().getInt("web.port", 8163), getConfig().getString("web.upload-token", ""),
                () -> registry, this::handleUpload);
        try {
            new dev.silent.silentui.assets.AssetApi(assets, profiles, getDataFolder().toPath().resolve("presets")).register(server);
            server.start();
            buildPackToServer();
        } catch (IOException e) {
            getLogger().severe("[SilentUI] Web server failed to start on port " + getConfig().getInt("web.port", 8163)
                    + ": " + e.getMessage());
            server = null;
        }
    }

    private void buildPackToServer() {
        try {
            byte[] zip = Files.readAllBytes(getDataFolder().toPath().resolve("output").resolve("SilentUI-pack.zip"));
            server.setPack(zip, packSha1Hex);
        } catch (IOException ignored) {
            // pack not built yet
        }
    }

    /** Upload from the editor (runs on a web thread). */
    private String handleUpload(UiDefinition def) {
        try {
            return Bukkit.getScheduler().callSyncMethod(this, () -> {
                try {
                    registry.save(def);
                } catch (IOException e) {
                    return e.getMessage();
                }
                String err = reloadAll();
                if (err == null) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("silentui.admin")) {
                            p.sendMessage(Text.mm("<#ff8fd1><bold>Silent UI</bold></#ff8fd1> <dark_gray>»</dark_gray> <gray>UI <white>"
                                    + def.id + "</white> was pushed from the editor and reloaded."));
                        }
                    }
                }
                return err;
            }).get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "Server busy: " + e.getMessage();
        }
    }

    public boolean packSending() {
        return server != null && getConfig().getBoolean("resource-pack.send-on-join", true);
    }

    public UUID packId() {
        return PACK_ID;
    }

    public void sendPack(Player p) {
        if (server == null || packSha1Hex.isEmpty()) return;
        String url = "http://" + getConfig().getString("web.public-host", "127.0.0.1") + ":"
                + getConfig().getInt("web.port", 8163) + server.packPath();
        try {
            ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), packSha1Hex);
            ResourcePackRequest req = ResourcePackRequest.resourcePackRequest()
                    .packs(info)
                    .required(getConfig().getBoolean("resource-pack.required", false))
                    .prompt(Text.mm(getConfig().getString("resource-pack.prompt", "")))
                    .replace(false)
                    .build();
            p.sendResourcePacks(req);
        } catch (IllegalArgumentException e) {
            getLogger().warning("[SilentUI] Bad pack URL " + url + ": " + e.getMessage());
        }
    }

    public String editorUrl() {
        if (server == null) return null;
        return "http://" + getConfig().getString("web.public-host", "127.0.0.1") + ":" + getConfig().getInt("web.port", 8163) + "/editor";
    }

    /** Full reload: config, UIs, pack, commands, world UIs; open screens are closed, HUDs reshown. */
    public String reloadAll() {
        Map<UUID, List<String>> shownHuds = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) shownHuds.put(p.getUniqueId(), new ArrayList<>(hud.shown(p)));
        manager.closeAll();
        manager.despawnAllWorlds();
        hud.clear();
        reloadConfig();
        readSettings();
        Text.resetPapi();
        manager.clearLogic();
        try {
            registry.load();
        } catch (IOException e) {
            return e.getMessage();
        }
        String oldHash = packSha1Hex;
        buildPack();
        dynamicCommands.registerAll();
        manager.loadWorlds();
        boolean newPack = !packSha1Hex.equals(oldHash);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (newPack && packSending()) sendPack(p);
            List<String> ids = shownHuds.getOrDefault(p.getUniqueId(), List.of());
            if (ids.isEmpty()) hud.autoShow(p);
            else for (String id : ids) hud.show(p, id);
        }
        return null;
    }

    // ------------------------------------------------------------------ accessors

    public UiRegistry registry() { return registry; }
    public UiManager manager() { return manager; }
    public HudManager hud() { return hud; }
    public ScriptRunner scripts() { return scripts; }
    public GlyphRegistry glyphs() { return glyphs; }
    public PlaneUi.Settings settings() { return settings; }
    public long currentTick() { return tick; }
    public BossBar.Color hudBarColor() { return hudColor; }
    public dev.silent.silentui.util.ProfileCache profiles() { return profiles; }
    public dev.silent.silentui.assets.AssetManager assets() { return assets; }

    public Map<String, String> vars(Player p) {
        return vars.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>());
    }

    public void forgetVars(Player p) {
        vars.remove(p.getUniqueId());
    }

    public Component message(String key) {
        if ("no-permission".equals(key)) return Text.mm("<red>You don't have permission to do that.");
        return Component.text(key);
    }
}
