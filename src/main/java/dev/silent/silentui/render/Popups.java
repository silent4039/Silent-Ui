package dev.silent.silentui.render;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.script.UiInstance;
import dev.silent.silentui.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Short-lived floating text (money, damage, "+1 level"...): pops in, drifts upward and shrinks away.
 * Every step is interpolated on the client, so it costs four packets no matter how long it floats.
 */
public final class Popups {

    private Popups() {}

    /**
     * @param at      "front" (in front of the player's eyes), "above" (over their head) or "element" (on a world/screen element)
     * @param ticks   total life time
     * @param rise    blocks it drifts upward
     * @param scale   text size multiplier
     * @param everyone false = only the player sees it
     */
    public static void spawn(SilentUI plugin, UiInstance ui, Player player, String text, String at, String elementId,
                             int ticks, double rise, double scale, boolean everyone) {
        Location loc = where(ui, player, at, elementId);
        if (loc == null || loc.getWorld() == null) return;
        int life = Math.max(12, Math.min(200, ticks));
        float sc = (float) Math.max(0.1, Math.min(8, scale));
        World world = loc.getWorld();
        boolean onlyPlayer = !everyone && player != null;
        TextDisplay d = world.spawn(loc, TextDisplay.class, e -> {
            e.setPersistent(false);
            e.addScoreboardTag(PlaneUi.TAG);
            e.text(Text.mm(text));
            e.setBillboard(Display.Billboard.CENTER);
            e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            e.setDefaultBackground(false);
            e.setShadowed(true);
            e.setBrightness(new Display.Brightness(15, 15));
            e.setViewRange(0.5f);
            e.setTransformation(transform(0, 0));
            if (onlyPlayer) e.setVisibleByDefault(false);
        });
        if (onlyPlayer) player.showEntity(plugin, d);
        // 1: pop in slightly too big, 2: settle, 3: drift up, 4: shrink away
        step(plugin, d, 1, 4, transform(0, sc * 1.25f));
        step(plugin, d, 5, 3, transform((float) (rise * 0.15), sc));
        step(plugin, d, 8, life - 12, transform((float) (rise * 0.85), sc));
        step(plugin, d, life - 4, 4, transform((float) rise, 0));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (d.isValid()) d.remove();
        }, life + 1L);
    }

    private static void step(SilentUI plugin, TextDisplay d, long delay, int duration, Transformation t) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!d.isValid()) return;
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(Math.max(1, duration));
            d.setTransformation(t);
        }, Math.max(1, delay));
    }

    private static Transformation transform(float y, float scale) {
        return new Transformation(new Vector3f(0, y, 0), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf());
    }

    private static Location where(UiInstance ui, Player player, String at, String elementId) {
        if ("element".equals(at) && ui instanceof PlaneUi plane) {
            ElementState s = plane.state(elementId);
            if (s != null) {
                Vector v = plane.worldPoint(s.x + s.w / 2, s.y);
                Location loc = new Location(plane.origin.getWorld(), v.getX(), v.getY(), v.getZ());
                return loc.add(plane.normal.clone().multiply(0.05));
            }
        }
        if (player == null) return null;
        if ("above".equals(at)) return player.getLocation().add(0, player.getHeight() + 0.35, 0);
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().setY(0);
        if (dir.lengthSquared() < 1e-6) dir = new Vector(0, 0, 1);
        return eye.add(dir.normalize().multiply(1.6)).add(0, -0.25, 0);
    }
}
