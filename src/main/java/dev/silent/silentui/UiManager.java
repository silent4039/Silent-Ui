package dev.silent.silentui;

import dev.silent.silentui.hud.HudInstance;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.render.PlaneUi;
import dev.silent.silentui.render.ScreenSession;
import dev.silent.silentui.render.WorldUi;
import dev.silent.silentui.script.UiInstance;
import dev.silent.silentui.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns open screen UIs, placed world UIs, clicks, keys and chat text input. */
public final class UiManager {

    private final SilentUI plugin;
    private final Map<UUID, ScreenSession> screens = new HashMap<>();
    private final Map<String, WorldUi> worlds = new LinkedHashMap<>();
    private final Map<UUID, PlaneUi> hitboxes = new HashMap<>();
    /** Read from the async chat thread, so it must be concurrent. */
    private final Map<UUID, PendingInput> inputs = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<UUID> internalTeleports = new HashSet<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private final File worldFile;

    record PendingInput(UiInstance ui, String elementId, long expiresAt) {}

    public UiManager(SilentUI plugin) {
        this.plugin = plugin;
        this.worldFile = new File(plugin.getDataFolder(), "world-uis.yml");
    }

    // ------------------------------------------------------------------ screens

    public ScreenSession screen(Player p) {
        return screens.get(p.getUniqueId());
    }

    private final Map<String, dev.silent.silentui.script.LogicInstance> logic = new HashMap<>();

    /** The running instance of a UI for this player (open screen, shown HUD, placed world UI) or a logic runtime. */
    public UiInstance instanceFor(Player p, String id) {
        UiDefinition def = plugin.registry().get(id);
        if (def == null) return null;
        if (p != null) {
            ScreenSession s = screen(p);
            if (s != null && s.def().id.equals(def.id)) return s;
            UiInstance h = plugin.hud().instance(p, def.id);
            if (h != null) return h;
        }
        for (WorldUi w : worlds.values()) if (w.def().id.equals(def.id) && w.isActive()) return w;
        return logic.computeIfAbsent(def.id, k -> new dev.silent.silentui.script.LogicInstance(def));
    }

    public void clearLogic() {
        logic.clear();
    }

    // ------------------------------------------------------------------ in-game editor

    public void startEditing(Player p, UiDefinition def) {
        new dev.silent.silentui.render.EditController(plugin, p, def).open();
        p.sendMessage(Text.mm("<white><bold>Silent UI editor</bold></white> <gray>· click to select · click again to move · right click for properties"));
        p.sendMessage(Text.mm("<gray>WASD nudge · scroll resize · Space pick up/drop · Q delete · F duplicate · Shift deselect/exit"));
    }

    public void stopEditing(Player p) {
        ScreenSession s = screen(p);
        if (s != null && s.editor() != null) closeScreen(p);
        p.sendActionBar(net.kyori.adventure.text.Component.empty());
    }

    public dev.silent.silentui.render.EditController editor(Player p) {
        ScreenSession s = screen(p);
        return s == null ? null : s.editor();
    }

    /** Saving reloads everything (pack, commands); the editor then reopens where it was. */
    public void reloadAfterEditorSave(dev.silent.silentui.render.EditController ctrl) {
        plugin.reloadAll();
        if (ctrl.player().isOnline()) ctrl.open();
    }

    public boolean openAny(Player p, String id) {
        UiDefinition def = plugin.registry().get(id);
        if (def == null) return false;
        if (def.isHud()) return plugin.hud().show(p, def.id);
        if (def.isWorld() || def.isLogic()) return false;
        openScreen(p, def);
        return true;
    }

    public ScreenSession openScreen(Player p, UiDefinition def) {
        return openSession(p, new ScreenSession(plugin, def, p));
    }

    public ScreenSession openSession(Player p, ScreenSession s) {
        closeScreen(p);
        screens.put(p.getUniqueId(), s);
        s.spawn();
        if (s.hitbox() != null) hitboxes.put(s.hitbox().getUniqueId(), s);
        plugin.scripts().fire(s, p, "on_open");
        return s;
    }

    public void closeScreen(Player p) {
        ScreenSession s = screens.remove(p.getUniqueId());
        if (s == null) return;
        inputs.remove(p.getUniqueId());
        plugin.scripts().fire(s, p, "on_close");
        if (s.hitbox() != null) hitboxes.remove(s.hitbox().getUniqueId());
        s.despawn();
    }

    public void closeAll() {
        for (ScreenSession s : new ArrayList<>(screens.values())) closeScreen(s.player());
    }

    public Collection<ScreenSession> screens() {
        return screens.values();
    }

    /** Teleport that does not count as "player moved away" (used to aim the camera). */
    public void internalTeleport(Player p, Location to) {
        internalTeleports.add(p.getUniqueId());
        try {
            p.teleport(to);
        } finally {
            internalTeleports.remove(p.getUniqueId());
        }
    }

    public boolean isInternalTeleport(Player p) {
        return internalTeleports.contains(p.getUniqueId());
    }

    // ------------------------------------------------------------------ input

    public PlaneUi byHitbox(Entity e) {
        return e == null ? null : hitboxes.get(e.getUniqueId());
    }

    /** Left/right click on one of our interaction hitboxes. Returns true if it was ours. */
    public boolean click(Player p, Entity target, boolean left) {
        PlaneUi ui = byHitbox(target);
        if (ui == null) return false;
        long now = plugin.currentTick();
        Long last = lastClick.get(p.getUniqueId());
        if (last != null && now - last < 2) return true; // swallow duplicate events in the same tick window
        lastClick.put(p.getUniqueId(), now);
        if (ui instanceof ScreenSession s) {
            if (s.player() != p || s.inClickGrace()) return true;
            if (s.editor() != null) {
                s.editor().click(left);
                return true;
            }
            ui.click(p, left, s.cursor());
        } else if (ui instanceof WorldUi w) {
            ui.click(p, left, w.cursorFor(p));
        }
        return true;
    }

    /** Key press while a screen UI is open: W A S D JUMP SNEAK SPRINT DROP SWAP SCROLL_UP SCROLL_DOWN */
    public void key(Player p, String key) {
        ScreenSession s = screen(p);
        if (s == null) return;
        if (s.editor() != null) {
            s.editor().key(key);
            return;
        }
        if (key.startsWith("SCROLL_") && s.scrollRotate(p, key.equals("SCROLL_UP") ? 1 : -1)) return;
        plugin.scripts().fire(s, p, "on_key", n -> {
            String k = n.prop("key", "any");
            return "any".equalsIgnoreCase(k) || k.equalsIgnoreCase(key);
        }, Map.of("key", key));
        if ("SNEAK".equals(key) && s.def().settings.closeOnSneak) closeScreen(p);
    }

    public void beginInput(Player p, UiInstance ui, String elementId) {
        dev.silent.silentui.render.ElementState st = ui.state(elementId);
        String current = st == null || st.text == null ? "" : st.text;
        String label = st == null || st.def.placeholder.isEmpty() ? "Text" : st.def.placeholder;
        dev.silent.silentui.util.Dialogs.Form form = new dev.silent.silentui.util.Dialogs.Form("<white>" + label)
                .text("value", label, current, 96).saveLabel("Done");
        boolean shown = dev.silent.silentui.util.Dialogs.show(plugin, p, form, values -> {
            if (!ui.isActive()) return;
            String clean = sanitize(values.getOrDefault("value", ""));
            if (ui instanceof PlaneUi plane) plane.submitInput(p, elementId, clean);
            else if (ui instanceof HudInstance hud) hud.submitInput(p, elementId, clean);
        });
        if (shown) return;
        inputs.put(p.getUniqueId(), new PendingInput(ui, elementId, plugin.currentTick() + 20L * 60));
        p.sendMessage(Text.mm("<#ff8fd1>✎</#ff8fd1> <gray>Type your text in chat. Send <white>cancel</white> to stop."));
    }

    /** Called from the async chat event. Returns true if the message was captured as input. */
    public boolean captureChat(Player p, String message) {
        PendingInput pi = inputs.get(p.getUniqueId());
        if (pi == null) return false;
        Bukkit.getScheduler().runTask(plugin, () -> {
            PendingInput cur = inputs.remove(p.getUniqueId());
            if (cur == null || !cur.ui().isActive() || plugin.currentTick() > cur.expiresAt()) return;
            if (message.equalsIgnoreCase("cancel")) return;
            String clean = sanitize(message);
            if (cur.ui() instanceof PlaneUi plane) plane.submitInput(p, cur.elementId(), clean);
            else if (cur.ui() instanceof HudInstance hud) hud.submitInput(p, cur.elementId(), clean);
        });
        return true;
    }

    /** Strips characters that could inject formatting or commands. */
    static String sanitize(String s) {
        String out = s.replaceAll("[<>\\\\\\n\\r§&{}%]", "").trim();
        while (out.startsWith("/")) out = out.substring(1);
        return out.length() > 96 ? out.substring(0, 96) : out;
    }

    // ------------------------------------------------------------------ world UIs

    public WorldUi placeWorld(String placementId, UiDefinition def, Location origin, float displayYaw) {
        removeWorld(placementId, false);
        WorldUi w = new WorldUi(plugin, def, placementId, origin, displayYaw);
        worlds.put(placementId, w);
        if (w.isLoaded()) spawnWorld(w);
        saveWorlds();
        return w;
    }

    private void spawnWorld(WorldUi w) {
        w.spawn();
        if (w.hitbox() != null) hitboxes.put(w.hitbox().getUniqueId(), w);
        plugin.scripts().fire(w, null, "on_open");
    }

    private void despawnWorld(WorldUi w) {
        if (w.hitbox() != null) hitboxes.remove(w.hitbox().getUniqueId());
        w.despawn();
    }

    public boolean removeWorld(String placementId, boolean save) {
        WorldUi w = worlds.remove(placementId);
        if (w == null) return false;
        despawnWorld(w);
        if (save) saveWorlds();
        return true;
    }

    public Map<String, WorldUi> worlds() {
        return worlds;
    }

    public void loadWorlds() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(worldFile);
        ConfigurationSection sec = yml.getConfigurationSection("placements");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection c = sec.getConfigurationSection(key);
            if (c == null) continue;
            UiDefinition def = plugin.registry().get(c.getString("ui"));
            World world = Bukkit.getWorld(c.getString("world", ""));
            if (def == null || world == null) {
                plugin.getLogger().warning("[SilentUI] World UI '" + key + "' skipped (missing UI or world).");
                continue;
            }
            Location loc = new Location(world, c.getDouble("x"), c.getDouble("y"), c.getDouble("z"));
            WorldUi w = new WorldUi(plugin, def, key, loc, (float) c.getDouble("yaw"));
            worlds.put(key, w);
            if (w.isLoaded()) spawnWorld(w);
        }
    }

    public void saveWorlds() {
        YamlConfiguration yml = new YamlConfiguration();
        for (WorldUi w : worlds.values()) {
            Location o = w.origin();
            String base = "placements." + w.placementId() + ".";
            yml.set(base + "ui", w.def().id);
            yml.set(base + "world", o.getWorld().getName());
            yml.set(base + "x", o.getX());
            yml.set(base + "y", o.getY());
            yml.set(base + "z", o.getZ());
            yml.set(base + "yaw", (double) displayYawOf(w));
        }
        try {
            yml.save(worldFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[SilentUI] Could not save world-uis.yml: " + e.getMessage());
        }
    }

    private float displayYawOf(WorldUi w) {
        return w.displayYawValue();
    }

    public void despawnAllWorlds() {
        for (WorldUi w : worlds.values()) despawnWorld(w);
        worlds.clear();
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        for (ScreenSession s : new ArrayList<>(screens.values())) {
            if (!s.player().isOnline()) {
                closeScreen(s.player());
                continue;
            }
            s.tick();
        }
        for (WorldUi w : worlds.values()) {
            boolean loaded = w.isLoaded();
            if (!w.isActive()) {
                if (loaded) spawnWorld(w);
                continue;
            }
            if (!loaded || w.broken()) {
                despawnWorld(w); // respawned when its chunk is loaded again
                continue;
            }
            w.tick();
        }
    }
}
