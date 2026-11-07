package dev.silent.silentui.script;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.render.ElementState;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/** What scripts can do to a running UI (screen, world, HUD or logic). */
public interface UiInstance {

    UiDefinition def();

    ElementState state(String elementId);

    Collection<ElementState> states();

    /** Push the element's current state to the client(s). */
    void refresh(String elementId, int interpolationTicks);

    void playAnimation(String idOrName);

    void stopAnimation(String idOrName);

    /** Close this UI for the given player (world UIs ignore it). */
    void close(Player player);

    boolean isActive();

    /** Start text input for an input element. */
    void focusInput(Player player, String elementId);

    /** Show one page of a tab set: elements in "tabset/page" become visible, other pages of the set hide. */
    default void switchTab(String tabset, String page) {
        if (tabset == null || tabset.isEmpty()) return;
        String target = tabset + "/" + page;
        for (ElementState s : states()) {
            String g = s.def.group;
            boolean changed = false;
            if (g != null && g.startsWith(tabset + "/")) {
                boolean show = g.equals(target) || g.startsWith(target + "/");
                if (s.visible != show) {
                    s.visible = show;
                    changed = true;
                }
            }
            String tf = s.def.tabFor;
            if (tf != null && tf.startsWith(tabset + "/")) {
                boolean on = tf.equals(target);
                if (s.active != on) {
                    s.active = on;
                    changed = true;
                }
            }
            if (changed) refresh(s.def.id, 0);
        }
    }

    default void setGroupVisible(String group, Boolean visible) {
        for (ElementState s : states()) {
            if (!s.inGroup(group)) continue;
            s.visible = visible == null ? !s.visible : visible;
            refresh(s.def.id, 0);
        }
    }

    /** Open every tab set on its default page. */
    default void applyDefaultTabs() {
        UiDefinition d = def();
        if (d.tabs == null) return;
        for (Map.Entry<String, String> e : d.tabs.entrySet()) switchTab(e.getKey(), e.getValue());
    }

    /** Clicking a tab button switches its page automatically. */
    default void clickedTabButton(ElementState s) {
        String tf = s.def.tabFor;
        if (tf == null || tf.isEmpty()) return;
        int slash = tf.lastIndexOf('/');
        if (slash <= 0) return;
        switchTab(tf.substring(0, slash), tf.substring(slash + 1));
    }
}
