package dev.silent.silentui.script;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.render.ElementState;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/** Runtime for "logic" files (commands + functions only) and for calling functions of UIs that are not open. */
public final class LogicInstance implements UiInstance {

    private final UiDefinition def;

    public LogicInstance(UiDefinition def) {
        this.def = def;
    }

    @Override public UiDefinition def() { return def; }
    @Override public ElementState state(String elementId) { return null; }
    @Override public Collection<ElementState> states() { return List.of(); }
    @Override public void refresh(String elementId, int interpolationTicks) { }
    @Override public void playAnimation(String idOrName) { }
    @Override public void stopAnimation(String idOrName) { }
    @Override public void close(Player player) { }
    @Override public boolean isActive() { return true; }
    @Override public void focusInput(Player player, String elementId) { }
}
