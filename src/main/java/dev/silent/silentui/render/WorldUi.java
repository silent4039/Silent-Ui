package dev.silent.silentui.render;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A UI placed in the world (screens, boards, mini-games). Everyone sees it and can click it with their crosshair. */
public final class WorldUi extends PlaneUi {

    private final String placementId;

    public WorldUi(SilentUI plugin, UiDefinition def, String placementId, Location origin, float displayYaw) {
        super(plugin, def, origin, displayYaw, Math.max(0.0005, def.settings.worldScale / 100.0));
        this.placementId = placementId;
    }

    public String placementId() {
        return placementId;
    }

    public Location origin() {
        return origin.clone();
    }

    @Override
    public void spawn() {
        super.spawn();
        float w = (float) Math.max(0.3, def.canvas.width * unit);
        float h = (float) Math.max(0.3, def.canvas.height * unit);
        hitbox = spawnHitbox(origin.clone().subtract(0, h / 2.0, 0), w, h);
    }

    public boolean isLoaded() {
        return origin.getWorld() != null && origin.isChunkLoaded();
    }

    /** True if some of our entities were unloaded with their chunk and need respawning. */
    public boolean broken() {
        if (hitbox == null || !hitbox.isValid()) return true;
        for (Entity e : extras) if (!e.isValid()) return true;
        return false;
    }

    @Override
    public void tick() {
        if (!active) return;
        super.tick();
        double range = plugin.settings().worldInteractRange;
        double reach = range + Math.max(def.canvas.width, def.canvas.height) * unit;
        Set<UUID> seen = new HashSet<>();
        for (Player p : origin.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(origin) > reach * reach) continue;
            if (plugin.manager().screen(p) != null) continue;
            seen.add(p.getUniqueId());
            double[] c = cursorFor(p);
            updateHover(p, c);
        }
        for (UUID id : new ArrayList<>(hovered.keySet())) {
            if (seen.contains(id)) continue;
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) updateHover(p, null);
            else hovered.remove(id);
        }
    }

    public double[] cursorFor(Player p) {
        double[] c = project(p.getEyeLocation(), plugin.settings().worldInteractRange);
        if (c == null || c[0] < 0 || c[1] < 0 || c[0] > def.canvas.width || c[1] > def.canvas.height) return null;
        return c;
    }

    @Override
    protected List<Player> timerPlayers() {
        return Collections.singletonList(null);
    }

    @Override
    protected void afterSpawn(Entity e) {
        // visible by default
    }

    @Override
    protected boolean visibleByDefault() {
        return true;
    }

    @Override
    public Player placeholderPlayer() {
        return null;
    }

    @Override
    public void close(Player player) {
        // world UIs stay in the world; use /sui world remove
    }
}
