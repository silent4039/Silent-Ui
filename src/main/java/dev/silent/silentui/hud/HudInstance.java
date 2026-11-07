package dev.silent.silentui.hud;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.model.UiDefinition.NodeDef;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.render.AnimationPlayer;
import dev.silent.silentui.render.ElementState;
import dev.silent.silentui.script.UiInstance;
import dev.silent.silentui.util.FontWidths;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * One HUD shown to one player. Drawn as bossbar text: every element is a glyph (or shifted text) wrapped in
 * negative/positive spaces so it lands at an exact pixel position and takes zero net width.
 */
public final class HudInstance implements UiInstance {

    private final SilentUI plugin;
    private final UiDefinition def;
    private final Player player;
    private final Map<String, ElementState> states = new LinkedHashMap<>();
    private final Map<String, TreeSet<Integer>> yValues;
    private final AnimationPlayer animations;
    private boolean active = true;
    private boolean dirty = true;
    private long ticks;

    public HudInstance(SilentUI plugin, UiDefinition def, Player player) {
        this.plugin = plugin;
        this.def = def;
        this.player = player;
        for (ElementDef e : def.elements) states.put(e.id, new ElementState(e));
        this.yValues = HudLayout.yValues(def);
        this.animations = new AnimationPlayer(def, states, (id, t) -> dirty = true, false);
        applyDefaultTabs();
        animations.autoplay();
    }

    /** Ticks animations and timers; returns true when the HUD needs redrawing. */
    boolean tick() {
        ticks++;
        animations.tick();
        for (NodeDef n : def.script.nodes) {
            if (!"on_timer".equals(n.type)) continue;
            long interval = Math.max(1, Math.round(Text.num(n.prop("seconds", "1"), 1) * 20));
            if (ticks % interval == 0) plugin.scripts().fire(this, player, "on_timer", node -> node == n, null);
        }
        boolean d = dirty;
        dirty = false;
        return d;
    }

    /** Builds this HUD's part of the bossbar title. */
    Component render() {
        return GlyphComposer.compose(def, states.values(), player, plugin.vars(player), plugin.glyphs().hudSet(),
                def.canvas.width / 2, 0, yValues);
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
    public java.util.Collection<ElementState> states() {
        return states.values();
    }

    @Override
    public void refresh(String elementId, int interpolationTicks) {
        dirty = true;
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
    public void close(Player p) {
        plugin.hud().hide(player, def.id);
    }

    @Override
    public boolean isActive() {
        return active && player.isOnline();
    }

    void deactivate() {
        active = false;
    }

    @Override
    public void focusInput(Player p, String elementId) {
        plugin.manager().beginInput(p, this, elementId);
    }

    public void submitInput(Player p, String elementId, String text) {
        ElementState s = states.get(elementId);
        if (s == null) return;
        s.text = text;
        plugin.vars(p).put("input_" + elementId, text);
        dirty = true;
        plugin.scripts().fireElement(this, p, "on_text", elementId, Map.of("text", text));
    }

    public Player player() {
        return player;
    }
}
