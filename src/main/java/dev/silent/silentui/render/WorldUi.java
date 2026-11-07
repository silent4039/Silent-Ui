package dev.silent.silentui.render;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A UI placed in the world: boards, holograms, floor markers.
 * <ul>
 *   <li>Shared (default): everyone sees the same entities and can click them with their crosshair.</li>
 *   <li>Per player ({@code settings.perPlayer}): this placement spawns a private copy for every player in range,
 *       so {@code %placeholders%}, tabs and animations are personal (like "Welcome %player_name%").</li>
 * </ul>
 */
public final class WorldUi extends PlaneUi {

    private final String placementId;
    /** The only player who sees this copy (per-player copies), or null for shared / root placements. */
    private final Player viewer;
    private final Map<UUID, WorldUi> copies = new HashMap<>();

    public WorldUi(SilentUI plugin, UiDefinition def, String placementId, Location origin, float displayYaw) {
        this(plugin, def, placementId, origin, displayYaw, null);
    }

    private WorldUi(SilentUI plugin, UiDefinition def, String placementId, Location origin, float displayYaw, Player viewer) {
        super(plugin, def, origin, displayYaw, Math.max(0.0005, def.settings.worldScale / 100.0));
        this.placementId = placementId;
        this.viewer = viewer;
    }

    public String placementId() {
        return placementId;
    }

    public Location origin() {
        return origin.clone();
    }

    /** Root of a per-player placement: it spawns nothing itself, only manages copies. */
    private boolean isRoot() {
        return viewer == null && def.settings.perPlayer;
    }

    /** The instance a script for this player should act on. */
    public WorldUi forViewer(Player p) {
        if (!isRoot() || p == null) return this;
        WorldUi c = copies.get(p.getUniqueId());
        return c != null ? c : this;
    }

    @Override
    public void spawn() {
        if (isRoot()) {
            active = true;
            return;
        }
        super.spawn();
        if (!needsHitbox()) return;
        float w = (float) Math.max(0.3, def.canvas.width * unit);
        float h = (float) Math.max(0.3, def.canvas.height * unit);
        if (Math.abs(displayPitch) >= 45) {
            // lying on the floor / ceiling: a flat box around it
            float size = Math.max(w, h);
            hitbox = spawnHitbox(origin.clone().subtract(0, 0.05, 0), size, 0.1f);
        } else if (billboard != Display.Billboard.FIXED) {
            float size = Math.max(w, h);
            hitbox = spawnHitbox(origin.clone().subtract(0, size / 2.0, 0), size, size);
        } else {
            hitbox = spawnHitbox(origin.clone().subtract(0, h / 2.0, 0), w, h);
        }
    }

    /**
     * Flat boards are always clickable. Holograms and floor markers only get a hitbox when they react to
     * clicks or hovers, so decorative ones never block players hitting mobs or blocks behind them.
     */
    private boolean needsHitbox() {
        if (billboard == Display.Billboard.FIXED && Math.abs(displayPitch) < 45) return true;
        if (hasEvent("on_click") || hasEvent("on_hover_enter") || hasEvent("on_hover_leave")) return true;
        for (UiDefinition.ElementDef e : def.elements) {
            if ("input".equals(e.type) || (e.tabFor != null && !e.tabFor.isEmpty())) return true;
        }
        return false;
    }

    @Override
    public void despawn() {
        for (WorldUi c : copies.values()) {
            plugin.manager().untrackHitbox(c);
            c.despawn();
        }
        copies.clear();
        super.despawn();
    }

    public boolean isLoaded() {
        return origin.getWorld() != null && origin.isChunkLoaded();
    }

    /** True if some of our entities were unloaded with their chunk and need respawning. */
    public boolean broken() {
        if (isRoot()) return false;
        if (hitbox != null && !hitbox.isValid()) return true;
        if (hitbox == null && needsHitbox()) return true;
        for (Entity e : extras) if (!e.isValid()) return true;
        for (ElementView v : views.values()) if (!v.valid()) return true;
        return false;
    }

    @Override
    public void tick() {
        if (!active) return;
        if (isRoot()) {
            tickCopies();
            return;
        }
        super.tick();
        if (hitbox == null) return;
        double range = plugin.settings().worldInteractRange;
        double reach = range + Math.max(def.canvas.width, def.canvas.height) * unit;
        Set<UUID> seen = new HashSet<>();
        List<Player> players = viewer != null ? List.of(viewer) : origin.getWorld().getPlayers();
        for (Player p : players) {
            if (!p.isOnline() || p.getWorld() != origin.getWorld()) continue;
            if (p.getLocation().distanceSquared(origin) > reach * reach) continue;
            if (plugin.manager().screen(p) != null) continue;
            seen.add(p.getUniqueId());
            updateHover(p, cursorFor(p));
        }
        for (UUID id : new ArrayList<>(hovered.keySet())) {
            if (seen.contains(id)) continue;
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) updateHover(p, null);
            else hovered.remove(id);
        }
    }

    /** Spawns a copy for players walking into range and removes it when they leave (with a little slack). */
    private void tickCopies() {
        ticks++;
        double range = Math.max(4, def.settings.viewRange);
        double in = range * range, out = (range + 8) * (range + 8);
        if (ticks % 10 == 1) {
            for (Player p : origin.getWorld().getPlayers()) {
                if (copies.containsKey(p.getUniqueId())) continue;
                if (p.getLocation().distanceSquared(origin) > in) continue;
                WorldUi c = new WorldUi(plugin, def, placementId, origin, displayYaw, p);
                copies.put(p.getUniqueId(), c);
                c.spawn();
                plugin.manager().trackHitbox(c);
                plugin.scripts().fire(c, p, "on_open");
            }
        }
        for (var it = copies.entrySet().iterator(); it.hasNext(); ) {
            WorldUi c = it.next().getValue();
            Player p = c.viewer;
            boolean gone = !p.isOnline() || p.getWorld() != origin.getWorld() || p.getLocation().distanceSquared(origin) > out;
            if (gone || c.broken()) {
                plugin.manager().untrackHitbox(c);
                c.despawn();
                it.remove();
                continue;
            }
            c.tick();
        }
    }

    public double[] cursorFor(Player p) {
        double[] c = project(p.getEyeLocation(), plugin.settings().worldInteractRange);
        if (c == null || c[0] < 0 || c[1] < 0 || c[0] > def.canvas.width || c[1] > def.canvas.height) return null;
        return c;
    }

    @Override
    protected List<Player> timerPlayers() {
        return viewer != null ? List.of(viewer) : Collections.singletonList(null);
    }

    @Override
    protected void afterSpawn(Entity e) {
        if (viewer != null && viewer.isOnline()) viewer.showEntity(plugin, e);
    }

    @Override
    protected boolean visibleByDefault() {
        return viewer == null;
    }

    @Override
    protected float viewRangeMultiplier() {
        return (float) Math.max(0.05, Math.min(16, def.settings.viewRange / 64.0));
    }

    @Override
    public Player placeholderPlayer() {
        return viewer;
    }

    @Override
    public void close(Player player) {
        // world UIs stay in the world; use /sui world remove
    }
}
