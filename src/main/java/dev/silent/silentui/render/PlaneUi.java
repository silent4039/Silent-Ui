package dev.silent.silentui.render;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.model.UiDefinition.NodeDef;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.script.ScriptRunner;
import dev.silent.silentui.script.UiInstance;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A UI drawn on a flat plane in the world with display entities.
 * Used for screen UIs (a plane locked in front of one player) and world UIs (a plane anyone can use).
 */
public abstract class PlaneUi implements UiInstance {

    public static final String TAG = "silentui";
    /** Text display font unit in blocks. */
    static final double FONT_UNIT = 0.025;

    protected final SilentUI plugin;
    protected final UiDefinition def;
    protected final Location origin;
    protected final float displayYaw;
    /** Blocks per canvas pixel. */
    protected final double unit;
    protected final Vector right;
    protected final Vector up = new Vector(0, 1, 0);
    protected final Vector normal;

    protected final Map<String, ElementState> states = new LinkedHashMap<>();
    protected final Map<String, ElementView> views = new LinkedHashMap<>();
    protected final List<Entity> extras = new ArrayList<>();
    protected final AnimationPlayer animations;
    protected Interaction hitbox;
    protected boolean active;
    protected long ticks;
    /** Hovered element per player. */
    protected final Map<java.util.UUID, String> hovered = new HashMap<>();

    protected PlaneUi(SilentUI plugin, UiDefinition def, Location origin, float displayYaw, double unit) {
        this.plugin = plugin;
        this.def = def;
        this.origin = origin.clone();
        this.displayYaw = displayYaw;
        this.unit = unit;
        double rad = Math.toRadians(displayYaw);
        this.normal = new Vector(-Math.sin(rad), 0, Math.cos(rad));
        this.right = new Vector(Math.cos(rad), 0, Math.sin(rad));
        for (ElementDef e : def.elements) states.put(e.id, new ElementState(e));
        this.animations = new AnimationPlayer(def, states, this::refresh, true);
    }

    // ------------------------------------------------------------------ lifecycle

    public void spawn() {
        active = true;
        int layer = 0;
        for (ElementDef e : def.elements) {
            ElementView v = new ElementView(this, states.get(e.id), layer++);
            v.spawn();
            views.put(e.id, v);
        }
        applyDefaultTabs();
        for (ElementView v : views.values()) v.update(0);
        if (!editMode) animations.autoplay();
    }

    /** In-game editor sessions draw the UI but never run its scripts or animations. */
    protected boolean editMode;

    public boolean isEditMode() {
        return editMode;
    }

    @Override
    public java.util.Collection<ElementState> states() {
        return states.values();
    }

    /** Scroll over a rotatable item/head/model turns it. Returns true if something rotated. */
    public boolean scrollRotate(Player p, int dir) {
        String id = hovered.get(p.getUniqueId());
        ElementState s = id == null ? null : states.get(id);
        if (s == null || !s.def.rotatable) return false;
        s.turn += dir * 22.5;
        refresh(id, 3);
        return true;
    }

    /** Re-applies an element after its item / head / sprite changed in a way the view must rebuild. */
    public void respawnElement(String id) {
        ElementView old = views.get(id);
        ElementState st = states.get(id);
        if (old == null || st == null) return;
        int layer = new ArrayList<>(views.keySet()).indexOf(id);
        old.remove();
        ElementView v = new ElementView(this, st, layer);
        v.spawn();
        views.put(id, v);
        v.update(0);
    }

    public void despawn() {
        active = false;
        for (ElementView v : views.values()) v.remove();
        views.clear();
        for (Entity e : extras) if (e.isValid()) e.remove();
        extras.clear();
        if (hitbox != null && hitbox.isValid()) hitbox.remove();
        hitbox = null;
    }

    public void tick() {
        if (!active) return;
        ticks++;
        animations.tick();
        if (ticks % 10 == 0) {
            for (ElementState s : states.values()) {
                if (s.def.spin != 0 && s.visible && isTurnable(s)) {
                    s.turn += s.def.spin * 0.5;
                    refresh(s.def.id, 10);
                }
            }
        }
        int refresh = plugin.settings().textRefreshTicks;
        if (ticks % refresh == 0) for (ElementView v : views.values()) v.refreshText();
        fireTimers();
    }

    protected void fireTimers() {
        for (NodeDef n : def.script.nodes) {
            if (!"on_timer".equals(n.type)) continue;
            long interval = Math.max(1, Math.round(dev.silent.silentui.util.Text.num(n.prop("seconds", "1"), 1) * 20));
            if (ticks % interval == 0) {
                for (Player p : timerPlayers()) plugin.scripts().fire(this, p, "on_timer", node -> node == n, null);
            }
        }
    }

    static boolean isTurnable(ElementState s) {
        String t = s.type();
        return "item".equals(t) || "head".equals(t) || "model".equals(t);
    }

    /** Players timer events run for. */
    protected abstract List<Player> timerPlayers();

    /** Make a freshly spawned entity visible to the right players. */
    protected abstract void afterSpawn(Entity e);

    /** Visible-by-default for world UIs, hidden for screen UIs. */
    protected abstract boolean visibleByDefault();

    /** The player whose placeholders text uses (null for world UIs). */
    public abstract Player placeholderPlayer();

    // ------------------------------------------------------------------ entity helpers

    <T extends Display> T spawnDisplay(Class<T> cls, Consumer<T> init) {
        Location loc = origin.clone();
        loc.setYaw(displayYaw);
        loc.setPitch(0);
        World world = origin.getWorld();
        T d = world.spawn(loc, cls, e -> {
            e.setPersistent(false);
            e.addScoreboardTag(TAG);
            e.setVisibleByDefault(visibleByDefault());
            e.setBillboard(Display.Billboard.FIXED);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setShadowRadius(0);
            e.setShadowStrength(0);
            e.setViewRange(visibleByDefault() ? 1.0f : 0.6f);
            e.setInterpolationDuration(0);
            e.setTeleportDuration(0);
            if (e instanceof TextDisplay t) {
                t.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                t.setDefaultBackground(false);
                t.setShadowed(false);
                t.setSeeThrough(false);
                t.setLineWidth(100000);
                t.setAlignment(TextDisplay.TextAlignment.CENTER);
            }
            init.accept(e);
        });
        afterSpawn(d);
        return d;
    }

    /** Player-shaped mannequin (1.21.9+). Returns null when the server has no mannequins. */
    @SuppressWarnings("unchecked")
    org.bukkit.entity.LivingEntity spawnModel() {
        org.bukkit.entity.EntityType type = org.bukkit.Registry.ENTITY_TYPE.get(org.bukkit.NamespacedKey.minecraft("mannequin"));
        if (type == null || type.getEntityClass() == null || !org.bukkit.entity.LivingEntity.class.isAssignableFrom(type.getEntityClass())) return null;
        Class<? extends org.bukkit.entity.LivingEntity> cls = (Class<? extends org.bukkit.entity.LivingEntity>) type.getEntityClass();
        Location loc = origin.clone();
        loc.setYaw(displayYaw);
        loc.setPitch(0);
        try {
            org.bukkit.entity.LivingEntity e = origin.getWorld().spawn(loc, cls, ent -> {
                ent.setPersistent(false);
                ent.addScoreboardTag(TAG);
                ent.setVisibleByDefault(visibleByDefault());
                ent.setGravity(false);
                ent.setInvulnerable(true);
                ent.setSilent(true);
                ent.setCollidable(false);
                ent.setAI(false);
                callIfPresent(ent, "setImmovable", true);
                callIfPresent(ent, "setHideDescription", true);
                callIfPresent(ent, "setDescriptionVisible", false);
            });
            afterSpawn(e);
            return e;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static void callIfPresent(Object target, String method, boolean value) {
        try {
            target.getClass().getMethod(method, boolean.class).invoke(target, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // not available on this version
        }
    }

    protected Interaction spawnHitbox(Location at, float width, float height) {
        World world = at.getWorld();
        Interaction box = world.spawn(at, Interaction.class, e -> {
            e.setPersistent(false);
            e.addScoreboardTag(TAG);
            e.setVisibleByDefault(visibleByDefault());
            e.setInteractionWidth(width);
            e.setInteractionHeight(height);
            e.setResponsive(false);
        });
        afterSpawn(box);
        return box;
    }

    public Interaction hitbox() {
        return hitbox;
    }

    // ------------------------------------------------------------------ geometry

    /** Canvas point after the element's scale + rotation around its anchor. */
    double[] warp(ElementState s, double ux, double uy) {
        double[] p = s.pivot();
        double sc = s.effectiveScale();
        double dx = (ux - p[0]) * sc, dy = (uy - p[1]) * sc;
        if (s.rotation != 0) {
            double r = Math.toRadians(s.rotation);
            double c = Math.cos(r), sn = Math.sin(r);
            double nx = dx * c - dy * sn, ny = dx * sn + dy * c;
            dx = nx;
            dy = ny;
        }
        return new double[]{p[0] + dx, p[1] + dy};
    }

    Vector3f local(double ux, double uy, double z) {
        return new Vector3f(
                (float) ((ux - def.canvas.width / 2.0) * unit),
                (float) ((def.canvas.height / 2.0 - uy) * unit),
                (float) z);
    }

    double layerZ(int layer, double extra) {
        return (layer + extra) * plugin.settings().layerDepth;
    }

    /** Transformation that stretches a glyph over the canvas rect (rx, ry, rw, rh). */
    Transformation glyphTransform(ElementState s, double rx, double ry, double rw, double rh,
                                  GlyphRegistry.UiGlyph g, int layer, double layerExtra) {
        Settings cfg = plugin.settings();
        double sc = s.effectiveScale();
        if (!s.visible || sc <= 0.0001 || rw <= 0.0001 || rh <= 0.0001) return zero();
        double[] c = warp(s, rx + rw / 2, ry + rh / 2);
        Vector3f center = local(c[0], c[1], layerZ(layer, layerExtra));
        float sx = (float) (sc * rw * unit / (g.drawnWidth() * FONT_UNIT));
        float sy = (float) (sc * rh * unit / (GlyphRegistry.UI_GLYPH_HEIGHT * FONT_UNIT));
        Quaternionf q = new Quaternionf().rotateZ((float) -Math.toRadians(s.rotation));
        double gcx = cfg.glyphCenterX + (g.drawnWidth() + 1 - g.advance()) / 2.0;
        Vector3f o = new Vector3f((float) (gcx * FONT_UNIT * sx), (float) (cfg.glyphCenterY * FONT_UNIT * sy), 0);
        q.transform(o);
        return new Transformation(center.sub(o), q, new Vector3f(sx, sy, 1), new Quaternionf());
    }

    /** Transformation for a text block whose visual centre lands on canvas point (cx, cy). */
    Transformation textTransform(ElementState s, double cx, double cy, int lines, int layer) {
        Settings cfg = plugin.settings();
        double sc = s.effectiveScale();
        if (!s.visible || sc <= 0.0001) return zero();
        double[] c = warp(s, cx, cy);
        Vector3f center = local(c[0], c[1], layerZ(layer, 0.5));
        float st = (float) (sc * s.def.textSize * unit / FONT_UNIT);
        double hb = cfg.textLineHeight * lines - 1;
        Quaternionf q = new Quaternionf().rotateZ((float) -Math.toRadians(s.rotation));
        Vector3f o = new Vector3f((float) (cfg.glyphCenterX * FONT_UNIT * st), (float) (hb / 2 * FONT_UNIT * st), 0);
        q.transform(o);
        return new Transformation(center.sub(o), q, new Vector3f(st, st, st), new Quaternionf());
    }

    Transformation itemTransform(ElementState s, int layer) {
        double sc = s.effectiveScale();
        if (!s.visible || sc <= 0.0001) return zero();
        double[] c = warp(s, s.x + s.w / 2, s.y + s.h / 2);
        Vector3f center = local(c[0], c[1], layerZ(layer, 0.5));
        float k = (float) (sc * Math.min(s.w, s.h) * unit);
        Quaternionf q = new Quaternionf().rotateZ((float) -Math.toRadians(s.rotation));
        if (plugin.settings().itemFlip) q.rotateY((float) Math.PI);
        Quaternionf spin = new Quaternionf().rotateY((float) Math.toRadians(s.turn));
        float depth = "item".equals(s.type()) && s.turn == 0 && s.def.spin == 0 ? k * 0.5f : k;
        return new Transformation(center, q, new Vector3f(k, k, depth), spin);
    }

    static Transformation zero() {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0, 0, 0), new Quaternionf());
    }

    /** Ray from the eye to the canvas, in canvas pixels; null if the ray misses the plane. */
    public double[] project(Location eye, double maxDistance) {
        Vector e = eye.toVector();
        Vector d = eye.getDirection();
        double denom = d.dot(normal);
        if (denom > -1e-4) return null; // looking away or from behind
        double t = origin.toVector().subtract(e).dot(normal) / denom;
        if (t <= 0 || t > maxDistance) return null;
        Vector hit = e.add(d.multiply(t)).subtract(origin.toVector());
        double ux = hit.dot(right) / unit + def.canvas.width / 2.0;
        double uy = def.canvas.height / 2.0 - hit.dot(up) / unit;
        return new double[]{ux, uy};
    }

    /** World position of a canvas point (used to aim the camera when clamping the cursor). */
    public Vector worldPoint(double ux, double uy) {
        double lx = (ux - def.canvas.width / 2.0) * unit, ly = (def.canvas.height / 2.0 - uy) * unit;
        return origin.toVector().add(right.clone().multiply(lx)).add(up.clone().multiply(ly));
    }

    /** Top-most interactive element under the canvas point. */
    public ElementState hit(double ux, double uy) {
        List<ElementState> list = new ArrayList<>(states.values());
        for (int i = list.size() - 1; i >= 0; i--) {
            ElementState s = list.get(i);
            if (s.interactive() && s.contains(ux, uy)) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------ input

    /** Called every tick with each relevant player's cursor position (or null when off-canvas). */
    protected void updateHover(Player p, double[] cursor) {
        ElementState now = cursor == null ? null : hit(cursor[0], cursor[1]);
        String newId = now == null ? null : now.def.id;
        String oldId = hovered.get(p.getUniqueId());
        if (java.util.Objects.equals(oldId, newId)) return;
        if (newId == null) hovered.remove(p.getUniqueId());
        else hovered.put(p.getUniqueId(), newId);
        int interp = plugin.settings().hoverInterpolation;
        if (oldId != null) {
            ElementState old = states.get(oldId);
            if (old != null) {
                old.hovered = hovered.containsValue(oldId);
                refresh(oldId, interp);
            }
            plugin.scripts().fireElement(this, p, "on_hover_leave", oldId, null);
        }
        if (newId != null) {
            now.hovered = true;
            refresh(newId, interp);
            plugin.scripts().fireElement(this, p, "on_hover_enter", newId, null);
        }
    }

    public void click(Player p, boolean left, double[] cursor) {
        if (!active || cursor == null) return;
        ElementState s = hit(cursor[0], cursor[1]);
        if (s == null) return;
        String button = left ? "left" : "right";
        plugin.scripts().fire(this, p, "on_click", n -> {
            String el = n.prop("element", "");
            String b = n.prop("button", "any");
            return (el.isEmpty() || el.equals(s.def.id)) && ("any".equals(b) || b.equals(button));
        }, Map.of("element", s.def.id, "button", button));
        clickedTabButton(s);
        if ("input".equals(s.type())) focusInput(p, s.def.id);
    }

    // ------------------------------------------------------------------ UiInstance

    @Override
    public UiDefinition def() {
        return def;
    }

    @Override
    public ElementState state(String elementId) {
        return states.get(elementId);
    }

    @Override
    public void refresh(String elementId, int interpolationTicks) {
        ElementView v = views.get(elementId);
        if (v != null) v.update(interpolationTicks);
    }

    @Override
    public void playAnimation(String idOrName) {
        animations.play(idOrName);
    }

    @Override
    public void stopAnimation(String idOrName) {
        animations.stop(idOrName);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void focusInput(Player player, String elementId) {
        plugin.manager().beginInput(player, this, elementId);
    }

    /** Text typed into an input element. */
    public void submitInput(Player p, String elementId, String text) {
        ElementState s = states.get(elementId);
        if (s == null || !active) return;
        s.text = text.replace("<", "\\<");
        plugin.vars(p).put("input_" + elementId, text);
        refresh(elementId, 0);
        plugin.scripts().fireElement(this, p, "on_text", elementId, Map.of("text", text));
    }

    public SilentUI plugin() {
        return plugin;
    }

    public float displayYawValue() {
        return displayYaw;
    }

    public boolean hasEvent(String type) {
        return ScriptRunner.has(def, type);
    }

    /** Runtime settings read from config.yml. */
    public static final class Settings {
        public double glyphCenterX = 0.5;
        public double glyphCenterY = 5.0;
        public double textLineHeight = 10.0;
        public boolean itemFlip = true;
        public double layerDepth = 0.0018;
        public int textRefreshTicks = 10;
        public int hoverInterpolation = 3;
        public int openClickGrace = 6;
        public double screenDistance = 1.4;
        public double viewFill = 0.82;
        public double fov = 70;
        public double worldInteractRange = 8;
        public int hudRefreshTicks = 5;
    }
}
