package dev.silent.silentui.render;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.pack.GlyphRegistry;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * A cursor UI locked in front of one player. The player's crosshair is the cursor: they move it by looking around,
 * which the client renders instantly (no server round-trip, no cursor delay). Hover is resolved server side every tick.
 */
public final class ScreenSession extends PlaneUi {

    private final Player player;
    private final double distance;
    private float savedFlySpeed = -1;
    private TextDisplay cursor;
    private ElementState cursorState;
    private TextDisplay dim;
    private double[] lastCursor;
    private final long openedAtTick;

    public ScreenSession(SilentUI plugin, UiDefinition def, Player player) {
        this(plugin, def, player, null, originFor(plugin, player), player.getEyeLocation().getYaw() + 180f);
    }

    /** Editor sessions keep the same spot in the world across rebuilds. */
    public ScreenSession(SilentUI plugin, UiDefinition def, Player player, EditController editor, Location origin, float displayYaw) {
        super(plugin, def, origin, displayYaw, unitFor(plugin, def));
        this.player = player;
        this.distance = plugin.settings().screenDistance;
        this.openedAtTick = plugin.currentTick();
        this.editor = editor;
        this.editMode = editor != null;
    }

    private final EditController editor;
    private boolean keepView;

    public EditController editor() {
        return editor;
    }

    /** Skip re-aiming the camera on spawn (editor rebuilds). */
    public ScreenSession keepView(boolean keep) {
        this.keepView = keep;
        return this;
    }

    public Location originLocation() {
        return origin.clone();
    }

    private static Location originFor(SilentUI plugin, Player p) {
        Location eye = p.getEyeLocation();
        double yaw = Math.toRadians(eye.getYaw());
        Vector forward = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        return eye.clone().add(forward.multiply(plugin.settings().screenDistance));
    }

    private static double unitFor(SilentUI plugin, UiDefinition def) {
        PlaneUi.Settings s = plugin.settings();
        double visibleHeight = 2 * s.screenDistance * Math.tan(Math.toRadians(s.fov / 2));
        double size = Math.max(0.1, Math.min(2.5, def.settings.screenSize));
        double byHeight = visibleHeight * s.viewFill * size / def.canvas.height;
        // keep wide canvases inside a 16:9 view too
        double byWidth = visibleHeight * (16.0 / 9.0) * s.viewFill * size / def.canvas.width;
        return Math.min(byHeight, byWidth);
    }

    public Player player() {
        return player;
    }

    @Override
    public void spawn() {
        // look straight at the centre so the cursor starts in the middle
        if (!keepView) {
            Location look = player.getLocation();
            look.setPitch(0);
            plugin.manager().internalTeleport(player, look);
        }

        UiDefinition.Settings st = def.settings;
        if (st.dim > 0.001) {
            UiDefinition.ElementDef dd = new UiDefinition.ElementDef();
            dd.x = -def.canvas.width * 2;
            dd.y = -def.canvas.height * 2;
            dd.w = def.canvas.width * 5;
            dd.h = def.canvas.height * 5;
            ElementState ds = new ElementState(dd);
            GlyphRegistry.UiGlyph px = plugin.glyphs().pixel();
            dim = spawnDisplay(TextDisplay.class, d -> {
                d.text(net.kyori.adventure.text.Component.text(String.valueOf(px.ch())).font(GlyphRegistry.FONT_UI)
                        .color(net.kyori.adventure.text.format.TextColor.color(0)));
                d.setTextOpacity((byte) Math.max(4, Math.min(255, (int) Math.round(st.dim * 255))));
            });
            dim.setTransformation(glyphTransform(ds, dd.x, dd.y, dd.w, dd.h, px, -2, 0));
            extras.add(dim);
        }
        super.spawn();
        if (!st.cursorSprite.isEmpty() && plugin.glyphs().sprite(def.id, st.cursorSprite) != null) {
            UiDefinition.SpriteDef sd = def.sprite(st.cursorSprite);
            UiDefinition.ElementDef cd = new UiDefinition.ElementDef();
            cd.w = sd == null ? 12 : Math.min(48, sd.width);
            cd.h = sd == null ? 12 : Math.min(48, sd.height);
            cd.anchor = "top-left";
            cursorState = new ElementState(cd);
            GlyphRegistry.UiGlyph g = plugin.glyphs().sprite(def.id, st.cursorSprite);
            cursor = spawnDisplay(TextDisplay.class, d -> d.text(net.kyori.adventure.text.Component.text(String.valueOf(g.ch()))
                    .font(GlyphRegistry.FONT_UI).color(net.kyori.adventure.text.format.TextColor.color(0xFFFFFF))));
            extras.add(cursor);
        }
        Location eye = player.getEyeLocation();
        hitbox = spawnHitbox(eye.clone().subtract(0, 0.7, 0), 1.4f, 1.4f);
        if (st.freeze) freeze();
    }

    @Override
    public void despawn() {
        super.despawn();
        unfreeze();
    }

    @Override
    public void tick() {
        if (!active) return;
        super.tick();
        Location eye = player.getEyeLocation();
        double[] c = project(eye, distance * 6);
        if (def.settings.clampCursor) c = clamp(eye, c);
        lastCursor = c;
        updateHover(player, c);
        if (editor != null) editor.tick(c);
        if (cursor != null && c != null) {
            cursorState.x = c[0];
            cursorState.y = c[1];
            GlyphRegistry.UiGlyph g = plugin.glyphs().sprite(def.id, def.settings.cursorSprite);
            cursor.setInterpolationDelay(0);
            cursor.setInterpolationDuration(1);
            cursor.setTransformation(glyphTransform(cursorState, c[0], c[1], cursorState.w, cursorState.h, g,
                    def.elements.size() + 2, 0));
        }
    }

    /** Keeps the crosshair inside the canvas by nudging the camera back when it leaves. */
    private double[] clamp(Location eye, double[] c) {
        double w = def.canvas.width, h = def.canvas.height;
        double margin = 1;
        if (c != null && c[0] >= -margin && c[0] <= w + margin && c[1] >= -margin && c[1] <= h + margin) return c;
        double tx = c == null ? w / 2 : Math.max(0, Math.min(w, c[0]));
        double ty = c == null ? h / 2 : Math.max(0, Math.min(h, c[1]));
        Vector target = worldPoint(tx, ty).subtract(eye.toVector());
        Location look = player.getLocation();
        look.setDirection(target);
        plugin.manager().internalTeleport(player, look);
        return new double[]{tx, ty};
    }

    public double[] cursor() {
        return lastCursor;
    }

    /** Re-evaluates hover right now (after the editor moved things under the cursor). */
    public void rehover() {
        hovered.remove(player.getUniqueId());
    }

    public boolean inClickGrace() {
        return plugin.currentTick() - openedAtTick < plugin.settings().openClickGrace;
    }

    // ------------------------------------------------------------------ freeze

    private NamespacedKey freezeKey() {
        return new NamespacedKey(plugin, "freeze");
    }

    private void freeze() {
        apply(Attribute.MOVEMENT_SPEED, true);
        apply(Attribute.JUMP_STRENGTH, true);
        if (player.isFlying()) {
            savedFlySpeed = player.getFlySpeed();
            player.setFlySpeed(0f);
        }
    }

    private void unfreeze() {
        removeFreeze(plugin, player);
        if (savedFlySpeed >= 0) {
            player.setFlySpeed(savedFlySpeed);
            savedFlySpeed = -1;
        }
    }

    private void apply(Attribute attribute, boolean on) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(freezeKey());
        if (on) inst.addModifier(new AttributeModifier(freezeKey(), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlotGroup.ANY));
    }

    /** Also used on join to clean up after a crash. */
    public static void removeFreeze(SilentUI plugin, Player p) {
        NamespacedKey key = new NamespacedKey(plugin, "freeze");
        for (Attribute a : List.of(Attribute.MOVEMENT_SPEED, Attribute.JUMP_STRENGTH)) {
            AttributeInstance inst = p.getAttribute(a);
            if (inst != null) inst.removeModifier(key);
        }
    }

    // ------------------------------------------------------------------ PlaneUi

    @Override
    protected List<Player> timerPlayers() {
        return List.of(player);
    }

    @Override
    protected void afterSpawn(Entity e) {
        player.showEntity(plugin, e);
    }

    @Override
    protected boolean visibleByDefault() {
        return false;
    }

    @Override
    public Player placeholderPlayer() {
        return player;
    }

    @Override
    public void close(Player p) {
        plugin.manager().closeScreen(player);
    }
}
