package dev.silent.silentui.script;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.render.ElementState;
import org.bukkit.entity.Player;

/** What scripts can do to a running UI (screen, world or HUD). */
public interface UiInstance {

    UiDefinition def();

    ElementState state(String elementId);

    /** Push the element's current state to the client(s). */
    void refresh(String elementId, int interpolationTicks);

    void playAnimation(String idOrName);

    void stopAnimation(String idOrName);

    /** Close this UI for the given player (world UIs ignore it). */
    void close(Player player);

    boolean isActive();

    /** Start chat text input for an input element. */
    void focusInput(Player player, String elementId);
}
