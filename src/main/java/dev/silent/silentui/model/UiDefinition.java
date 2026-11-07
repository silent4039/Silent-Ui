package dev.silent.silentui.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The exact JSON shape the web editor exports (*.json in plugins/SilentUI/uis/).
 * Public fields + defaults so Gson can fill it directly and missing keys stay sane.
 */
public final class UiDefinition {

    public int format = 1;
    public String id = "ui";
    public String name = "UI";
    /** screen | hud | world */
    public String type = "screen";
    public Canvas canvas = new Canvas();
    public Settings settings = new Settings();
    public List<SpriteDef> sprites = new ArrayList<>();
    /** Index 0 is the bottom layer, last is the top layer. */
    public List<ElementDef> elements = new ArrayList<>();
    public List<AnimationDef> animations = new ArrayList<>();
    public ScriptDef script = new ScriptDef();
    /** Tab sets: tabset name -> page shown when the UI opens. Elements join a page with group "tabset/page". */
    public Map<String, String> tabs = new java.util.LinkedHashMap<>();
    /** Custom commands with typed arguments. */
    public List<CommandDef> commands = new ArrayList<>();

    public boolean isHud() { return "hud".equalsIgnoreCase(type); }
    public boolean isWorld() { return "world".equalsIgnoreCase(type); }
    /** Logic-only file: commands and functions, nothing drawn. */
    public boolean isLogic() { return "logic".equalsIgnoreCase(type); }
    public boolean isScreen() { return !isHud() && !isWorld() && !isLogic(); }

    public ElementDef element(String elementId) {
        if (elementId == null) return null;
        for (ElementDef e : elements) if (elementId.equals(e.id)) return e;
        return null;
    }

    public SpriteDef sprite(String spriteId) {
        if (spriteId == null || spriteId.isEmpty()) return null;
        for (SpriteDef s : sprites) if (spriteId.equals(s.id)) return s;
        return null;
    }

    public AnimationDef animation(String idOrName) {
        if (idOrName == null) return null;
        for (AnimationDef a : animations) if (idOrName.equals(a.id) || idOrName.equalsIgnoreCase(a.name)) return a;
        return null;
    }

    public static final class Canvas {
        public int width = 480;
        public int height = 270;
    }

    public static final class Settings {
        /** Optional command that opens this UI directly, e.g. "menu". */
        public String command = "";
        public String permission = "";
        /** Screen UI: lock movement while open. */
        public boolean freeze = true;
        /** Screen UI: sneaking closes the UI. */
        public boolean closeOnSneak = true;
        /** Screen UI: keep the crosshair inside the canvas. */
        public boolean clampCursor = true;
        /** Screen UI: darken the world behind the canvas (0..1). */
        public double dim = 0.0;
        /** Optional sprite drawn under the crosshair. */
        public String cursorSprite = "";
        /** Screen UI: multiplier on the configured canvas size. */
        public double screenSize = 1.0;
        /** HUD: show automatically on join. */
        public boolean autoShow = false;
        /** World UI: blocks per 100 canvas pixels. */
        public double worldScale = 1.0;
    }

    public static final class SpriteDef {
        public String id = "";
        /** data:image/png;base64,... */
        public String data = "";
        public int width = 16;
        public int height = 16;
    }

    public static final class ElementDef {
        public String id = "";
        public String name = "Element";
        /** rect | image | text | button | input | item | head | progress */
        public String type = "rect";
        public double x = 0, y = 0, w = 64, h = 32;
        public double rotation = 0;
        public double opacity = 1;
        public double scale = 1;
        public boolean visible = true;
        /** Ignores the cursor (no hover / click). */
        public boolean blocked = false;
        /** Pivot for scale + rotation: center, top-left, top, top-right, left, right, bottom-left, bottom, bottom-right */
        public String anchor = "center";

        public String fill = "#ffffff";
        public boolean fillEnabled = true;
        public String sprite = "";

        public String text = "";
        public String textColor = "#ffffff";
        public double textSize = 1;
        /** left | center | right */
        public String align = "center";
        public boolean shadow = true;

        public String hoverFill = "";
        public String hoverSprite = "";
        public String hoverTextColor = "";
        public double hoverScale = 1;

        /** item / head */
        public String item = "minecraft:diamond";
        /** head: blank = viewer's own head, or a player name */
        public String headOwner = "";

        /** progress: value and max may contain placeholders */
        public String value = "50";
        public String max = "100";
        public String barColor = "#ff8fd1";
        public String barSprite = "";

        /** input: shown while empty */
        public String placeholder = "Type...";

        /** Group name. "tabset/page" makes the element part of a tab page. */
        public String group = "";
        /** Marks a tab button: "tabset/page". Clicking it switches to that page; it uses the active style while selected. */
        public String tabFor = "";
        public String activeFill = "";
        public String activeSprite = "";
        public String activeTextColor = "";

        /** item: 1.21.4+ item model key (e.g. "minecraft:diamond" or "myitems:ruby") */
        public String itemModel = "";
        public boolean glint = false;
        /** item/head/model: degrees per second of continuous spin */
        public double spin = 0;
        /** item/head/model: scrolling over it rotates it */
        public boolean rotatable = false;
    }

    public static final class CommandDef {
        public String name = "";
        public List<String> aliases = new ArrayList<>();
        public String permission = "";
        public String description = "";
        public List<ArgDef> args = new ArrayList<>();
        /** Function (on_function node name) to run. Optional. */
        public String function = "";
        /** Open/show this UI before running. */
        public boolean open = true;
    }

    public static final class ArgDef {
        public String name = "arg";
        /** word | text (rest of the line) | number | integer | player | choice | boolean */
        public String type = "word";
        public List<String> choices = new ArrayList<>();
        public boolean optional = false;
        public String def = "";
    }

    public static final class AnimationDef {
        public String id = "";
        public String name = "Animation";
        /** Length in ticks (20 = 1s). */
        public int length = 20;
        public boolean loop = false;
        public boolean autoplay = false;
        public List<TrackDef> tracks = new ArrayList<>();
    }

    public static final class TrackDef {
        public String element = "";
        /** x | y | w | h | scale | rotation | opacity */
        public String property = "x";
        public List<KeyDef> keys = new ArrayList<>();
    }

    public static final class KeyDef {
        public int t = 0;
        public double v = 0;
        /** Easing used from this key to the next: linear | ease-in | ease-out | ease-in-out | step */
        public String ease = "linear";
    }

    public static final class ScriptDef {
        public List<NodeDef> nodes = new ArrayList<>();
    }

    public static final class NodeDef {
        public String id = "";
        public String type = "";
        public double x = 0, y = 0;
        public Map<String, String> props = new HashMap<>();
        /** output port -> connected node ids. Ports: then, true, false */
        public Map<String, List<String>> next = new HashMap<>();

        public String prop(String key, String def) {
            String v = props == null ? null : props.get(key);
            return v == null || v.isEmpty() ? def : v;
        }

        public List<String> out(String port) {
            if (next == null) return List.of();
            List<String> l = next.get(port);
            return l == null ? List.of() : l;
        }
    }
}
