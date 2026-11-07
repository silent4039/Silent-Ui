package dev.silent.silentui.render;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.model.UiRegistry;
import dev.silent.silentui.util.Dialogs;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * In-game editor (/sui edit &lt;ui&gt;). Draws the UI in front of the player with a toolbar underneath.
 * <ul>
 *   <li>Click an element to select it, click it again to pick it up; it follows your crosshair until you click.</li>
 *   <li>Right click opens its properties.</li>
 *   <li>WASD nudges 1px, scroll resizes, Space picks up / drops, Q deletes, F duplicates, Shift deselects / exits.</li>
 * </ul>
 * Scripts and animations never run while editing.
 */
public final class EditController {

    private enum Mode { NONE, GRAB, RESIZE }

    private static final String P = "__sui_";
    private static final int BAR_GAP = 6, BAR_H = 22, BTN_W = 36, BTN_H = 16, BTN_GAP = 2;
    private static final String[][] TOOLS = {
            {"move", "Move"}, {"size", "Size"}, {"up", "Up"}, {"down", "Down"}, {"copy", "Copy"}, {"del", "Delete"},
            {"edit", "Edit"}, {"add", "Add"}, {"page", "Page"}, {"undo", "Undo"}, {"save", "Save"}, {"exit", "Exit"}};

    private final SilentUI plugin;
    private final Player player;
    private UiDefinition working;
    private ScreenSession session;
    private Location origin;
    private Float displayYaw;

    private String selected;
    private Mode mode = Mode.NONE;
    private double grabDx, grabDy;
    private int offX;
    private final Deque<String> undo = new ArrayDeque<>();
    private final Map<String, String> pages = new LinkedHashMap<>();
    private boolean dirty;
    private long exitWarnTick = -100, lastNudgeTick = -100;
    private final Set<String> hiddenInWorking = new LinkedHashSet<>();

    public EditController(SilentUI plugin, Player player, UiDefinition def) {
        this.plugin = plugin;
        this.player = player;
        this.working = UiRegistry.copy(def);
        if (working.tabs != null) pages.putAll(working.tabs);
    }

    public UiDefinition working() {
        return working;
    }

    public Player player() {
        return player;
    }

    // ------------------------------------------------------------------ building the edit view

    public void open() {
        UiDefinition view = buildView();
        ScreenSession s;
        if (origin == null) {
            s = new ScreenSession(plugin, view, player, this, originFor(), player.getEyeLocation().getYaw() + 180f);
            origin = s.originLocation();
            displayYaw = s.displayYawValue();
        } else {
            s = new ScreenSession(plugin, view, player, this, origin, displayYaw).keepView(true);
        }
        session = s;
        plugin.manager().openSession(player, s);
        updateFrame();
        status();
    }

    private Location originFor() {
        Location eye = player.getEyeLocation();
        double yaw = Math.toRadians(eye.getYaw());
        return eye.clone().add(-Math.sin(yaw) * plugin.settings().screenDistance, 0, Math.cos(yaw) * plugin.settings().screenDistance);
    }

    private UiDefinition buildView() {
        UiDefinition v = UiRegistry.copy(working);
        int w = working.canvas.width, h = working.canvas.height;
        int barW = TOOLS.length * (BTN_W + BTN_GAP) + 6;
        int vw = Math.max(w, barW);
        offX = (vw - w) / 2;
        v.type = "screen";
        v.animations = new ArrayList<>();
        v.script = new UiDefinition.ScriptDef();
        v.commands = new ArrayList<>();
        v.settings.freeze = true;
        v.settings.closeOnSneak = false;
        v.settings.clampCursor = true;
        v.settings.cursorSprite = "";
        v.settings.dim = Math.max(0.35, working.settings.dim);
        v.settings.screenSize = Math.min(1.0, working.settings.screenSize);
        v.canvas.width = vw;
        v.canvas.height = h + BAR_GAP + BAR_H + 2;
        v.tabs = new LinkedHashMap<>(pages);
        hiddenInWorking.clear();
        for (ElementDef e : v.elements) {
            e.x += offX;
            e.blocked = false;
            e.hoverFill = "";
            e.hoverSprite = "";
            e.hoverTextColor = "";
            e.hoverScale = 1;
            e.spin = 0;
            if (!e.visible) {
                hiddenInWorking.add(e.id);
                e.visible = true;
                e.opacity = Math.max(0.15, e.opacity * 0.35);
            }
        }
        // canvas outline under everything
        ElementDef bounds = rect(P + "canvas", offX, 0, w, h, "#ffffff");
        bounds.opacity = 0.05;
        bounds.blocked = true;
        v.elements.add(0, bounds);
        // selection frame
        for (String side : new String[]{"t", "b", "l", "r"}) {
            ElementDef f = rect(P + "sel_" + side, 0, 0, 1, 1, "#ffffff");
            f.visible = false;
            f.blocked = true;
            v.elements.add(f);
        }
        // toolbar
        int by = h + BAR_GAP;
        ElementDef bar = rect(P + "bar", 0, by, vw, BAR_H, "#0e0e0e");
        bar.opacity = 0.92;
        bar.blocked = true;
        v.elements.add(bar);
        int x = (vw - (TOOLS.length * (BTN_W + BTN_GAP) - BTN_GAP)) / 2;
        for (String[] t : TOOLS) {
            ElementDef b = new ElementDef();
            b.id = P + "t_" + t[0];
            b.name = t[1];
            b.type = "button";
            b.x = x;
            b.y = by + (BAR_H - BTN_H) / 2.0;
            b.w = BTN_W;
            b.h = BTN_H;
            b.fill = t[0].equals("save") ? "#e8e8e8" : "#262626";
            b.textColor = t[0].equals("save") ? "#000000" : "#f2f2f2";
            b.hoverFill = "#ffffff";
            b.hoverTextColor = "#000000";
            b.text = t[1];
            b.textSize = 0.7;
            b.shadow = false;
            v.elements.add(b);
            x += BTN_W + BTN_GAP;
        }
        return v;
    }

    private static ElementDef rect(String id, double x, double y, double w, double h, String fill) {
        ElementDef e = new ElementDef();
        e.id = id;
        e.name = id;
        e.type = "rect";
        e.x = x;
        e.y = y;
        e.w = w;
        e.h = h;
        e.fill = fill;
        return e;
    }

    private ElementDef sel() {
        return selected == null ? null : working.element(selected);
    }

    // ------------------------------------------------------------------ per tick

    void tick(double[] cursor) {
        ElementDef e = sel();
        if (e != null && cursor != null && mode != Mode.NONE) {
            ElementState st = session.state(e.id);
            if (mode == Mode.GRAB) {
                e.x = Math.round(cursor[0] - grabDx - offX);
                e.y = Math.round(cursor[1] - grabDy);
            } else {
                e.w = Math.max(1, Math.round(cursor[0] - offX - e.x));
                e.h = Math.max(1, Math.round(cursor[1] - e.y));
            }
            if (st != null) {
                st.x = e.x + offX;
                st.y = e.y;
                st.w = e.w;
                st.h = e.h;
                session.refresh(e.id, 1);
            }
            updateFrame();
        }
        if (plugin.currentTick() % 10 == 0) status();
    }

    private void status() {
        ElementDef e = sel();
        String what = e == null ? "<gray>Click an element to select it" :
                "<white>" + esc(e.name) + "</white> <gray>" + (int) e.x + ", " + (int) e.y + " · " + (int) e.w + "×" + (int) e.h + "</gray>";
        String m = switch (mode) {
            case GRAB -> " <white>· moving, click to drop</white>";
            case RESIZE -> " <white>· resizing, click to finish</white>";
            default -> "";
        };
        player.sendActionBar(Text.mm("<gray>Editing <white>" + working.id + "</white>" + (dirty ? " *" : "") + " ·</gray> " + what + m));
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("<", "\\<");
    }

    private void updateFrame() {
        if (session == null) return;
        ElementDef e = sel();
        String[] sides = {"t", "b", "l", "r"};
        for (String side : sides) {
            ElementState f = session.state(P + "sel_" + side);
            if (f == null) continue;
            if (e == null) {
                f.visible = false;
            } else {
                double x = e.x + offX, y = e.y, w = e.w, h = e.h;
                f.visible = true;
                switch (side) {
                    case "t" -> { f.x = x - 1; f.y = y - 1; f.w = w + 2; f.h = 1; }
                    case "b" -> { f.x = x - 1; f.y = y + h; f.w = w + 2; f.h = 1; }
                    case "l" -> { f.x = x - 1; f.y = y; f.w = 1; f.h = h; }
                    default -> { f.x = x + w; f.y = y; f.w = 1; f.h = h; }
                }
            }
            session.refresh(f.def.id, 1);
        }
    }

    // ------------------------------------------------------------------ input

    public void click(boolean left) {
        double[] c = session.cursor();
        if (mode != Mode.NONE) {
            mode = Mode.NONE;
            dirty = true;
            click("block.wooden_button.click_off");
            status();
            return;
        }
        ElementState hit = c == null ? null : session.hit(c[0], c[1]);
        String id = hit == null ? null : hit.def.id;
        if (id != null && id.startsWith(P + "t_")) {
            tool(id.substring((P + "t_").length()));
            return;
        }
        if (id == null || id.startsWith(P)) {
            selected = null;
            updateFrame();
            status();
            return;
        }
        if (!left) {
            selected = id;
            updateFrame();
            properties();
            return;
        }
        if (id.equals(selected)) {
            beginGrab(c);
        } else {
            selected = id;
            click("ui.button.click");
            updateFrame();
            status();
        }
    }

    public void key(String key) {
        ElementDef e = sel();
        switch (key) {
            case "W", "A", "S", "D" -> {
                if (e == null) return;
                if (plugin.currentTick() - lastNudgeTick > 20) snapshot();
                lastNudgeTick = plugin.currentTick();
                if (key.equals("W")) e.y -= 1;
                if (key.equals("S")) e.y += 1;
                if (key.equals("A")) e.x -= 1;
                if (key.equals("D")) e.x += 1;
                syncState(e, 1);
            }
            case "SCROLL_UP", "SCROLL_DOWN" -> {
                if (e == null) return;
                if (plugin.currentTick() - lastNudgeTick > 20) snapshot();
                lastNudgeTick = plugin.currentTick();
                double d = key.equals("SCROLL_UP") ? 2 : -2;
                double ratio = e.h / Math.max(1, e.w);
                e.w = Math.max(1, e.w + d);
                e.h = Math.max(1, Math.round(e.w * ratio));
                syncState(e, 2);
            }
            case "JUMP" -> {
                if (e == null) return;
                if (mode == Mode.NONE) beginGrab(session.cursor());
                else click(true);
            }
            case "DROP" -> tool("del");
            case "SWAP" -> tool("copy");
            case "SNEAK" -> {
                if (mode != Mode.NONE) {
                    undo(); // cancel the move
                    mode = Mode.NONE;
                } else if (selected != null) {
                    selected = null;
                    updateFrame();
                } else {
                    tool("exit");
                }
            }
            default -> { }
        }
        status();
    }

    private void beginGrab(double[] c) {
        ElementDef e = sel();
        if (e == null || c == null) return;
        snapshot();
        grabDx = c[0] - (e.x + offX);
        grabDy = c[1] - e.y;
        mode = Mode.GRAB;
        click("block.wooden_button.click_on");
    }

    private void syncState(ElementDef e, int interp) {
        ElementState st = session.state(e.id);
        if (st != null) {
            st.x = e.x + offX;
            st.y = e.y;
            st.w = e.w;
            st.h = e.h;
            session.refresh(e.id, interp);
        }
        dirty = true;
        updateFrame();
    }

    private void click(String sound) {
        player.playSound(Sound.sound(net.kyori.adventure.key.Key.key(sound), Sound.Source.MASTER, 0.4f, 1.4f));
    }

    // ------------------------------------------------------------------ tools

    private void tool(String t) {
        ElementDef e = sel();
        switch (t) {
            case "move" -> {
                if (e == null) hint("Select an element first.");
                else beginGrab(session.cursor());
            }
            case "size" -> {
                if (e == null) {
                    hint("Select an element first.");
                    return;
                }
                snapshot();
                mode = Mode.RESIZE;
            }
            case "up", "down" -> {
                if (e == null) return;
                snapshot();
                List<ElementDef> list = working.elements;
                int i = list.indexOf(e), j = t.equals("up") ? i + 1 : i - 1;
                if (j < 0 || j >= list.size()) return;
                list.set(i, list.get(j));
                list.set(j, e);
                changed();
            }
            case "copy" -> {
                if (e == null) return;
                snapshot();
                ElementDef c = UiRegistry.GSON.fromJson(UiRegistry.GSON.toJson(e), ElementDef.class);
                c.id = uniqueId(e.id);
                c.name = e.name + " copy";
                c.x += 6;
                c.y += 6;
                working.elements.add(working.elements.indexOf(e) + 1, c);
                selected = c.id;
                changed();
            }
            case "del" -> {
                if (e == null) return;
                snapshot();
                working.elements.remove(e);
                selected = null;
                changed();
            }
            case "edit" -> {
                if (e == null) hint("Select an element first, or right click one.");
                else properties();
            }
            case "add" -> addDialog();
            case "page" -> cyclePage();
            case "undo" -> {
                if (undo.isEmpty()) hint("Nothing to undo.");
                else {
                    undo();
                    rebuild();
                }
            }
            case "save" -> save();
            case "exit" -> {
                if (dirty && plugin.currentTick() - exitWarnTick > 60) {
                    exitWarnTick = plugin.currentTick();
                    player.sendMessage(Text.mm("<gray>Unsaved changes. Press <white>Exit</white> (or Shift) again to discard, or <white>Save</white>."));
                    return;
                }
                plugin.manager().stopEditing(player);
            }
            default -> { }
        }
    }

    private void hint(String msg) {
        player.sendActionBar(Text.mm("<gray>" + msg));
    }

    private String uniqueId(String base) {
        String b = base.replaceAll("_\\d+$", "");
        int n = 2;
        String id = b + "_" + n;
        while (working.element(id) != null) id = b + "_" + (++n);
        return id;
    }

    private void cyclePage() {
        // tab sets come from the default tabs and from "set/page" groups
        Map<String, List<String>> sets = new LinkedHashMap<>();
        for (ElementDef el : working.elements) {
            String g = el.group;
            int slash = g == null ? -1 : g.indexOf('/');
            if (slash <= 0) continue;
            String set = g.substring(0, slash), page = g.substring(slash + 1);
            List<String> l = sets.computeIfAbsent(set, k -> new ArrayList<>());
            if (!l.contains(page)) l.add(page);
        }
        if (sets.isEmpty()) {
            hint("This UI has no tab pages. Give elements a group like main/shop.");
            return;
        }
        String set = sets.keySet().iterator().next();
        ElementDef e = sel();
        if (e != null && e.group != null && e.group.contains("/")) set = e.group.substring(0, e.group.indexOf('/'));
        List<String> list = sets.get(set);
        String cur = pages.getOrDefault(set, list.get(0));
        String next = list.get((list.indexOf(cur) + 1) % list.size());
        pages.put(set, next);
        selected = null;
        rebuild();
        hint("Showing page " + set + "/" + next);
    }

    private void snapshot() {
        undo.push(UiRegistry.GSON.toJson(working));
        while (undo.size() > 60) undo.removeLast();
    }

    private void undo() {
        if (undo.isEmpty()) return;
        working = UiRegistry.GSON.fromJson(undo.pop(), UiDefinition.class);
        if (selected != null && working.element(selected) == null) selected = null;
        dirty = true;
    }

    private void changed() {
        dirty = true;
        rebuild();
    }

    private void rebuild() {
        mode = Mode.NONE;
        open();
    }

    private void save() {
        String err = UiRegistry.check(working);
        if (err != null) {
            player.sendMessage(Text.mm("<red>Can't save: " + esc(err)));
            return;
        }
        try {
            plugin.registry().save(working);
        } catch (java.io.IOException ex) {
            player.sendMessage(Text.mm("<red>Save failed: " + esc(ex.getMessage())));
            return;
        }
        dirty = false;
        plugin.manager().reloadAfterEditorSave(this);
        player.sendMessage(Text.mm("<white>Saved</white> <gray>" + working.id + ".json and reloaded."));
        click("entity.experience_orb.pickup");
    }

    // ------------------------------------------------------------------ dialogs

    private void properties() {
        ElementDef e = sel();
        if (e == null) return;
        List<String> sprites = new ArrayList<>();
        sprites.add("");
        for (UiDefinition.SpriteDef s : working.sprites) sprites.add(s.id);
        Dialogs.Form f = new Dialogs.Form("<white>" + esc(e.name) + " <gray>(" + e.type + ")")
                .text("name", "Name", e.name, 64)
                .text("x", "X", fmt(e.x), 12).text("y", "Y", fmt(e.y), 12)
                .text("w", "Width", fmt(e.w), 12).text("h", "Height", fmt(e.h), 12)
                .text("rotation", "Rotation", fmt(e.rotation), 12)
                .text("opacity", "Opacity (0-1)", fmt(e.opacity), 12)
                .toggle("visible", "Visible", e.visible)
                .toggle("blocked", "Ignore the cursor", e.blocked);
        boolean visual = !List.of("item", "head", "model").contains(e.type);
        if (visual) {
            f.toggle("fillEnabled", "Fill colour on", e.fillEnabled)
                    .text("fill", "Fill colour", e.fill, 16)
                    .choice("sprite", "Sprite", sprites, e.sprite);
        }
        if (List.of("text", "button", "input", "rect").contains(e.type)) {
            f.area("text", "Text (MiniMessage, {player}, %papi%)", e.text)
                    .text("textColor", "Text colour", e.textColor, 16)
                    .text("textSize", "Text size", fmt(e.textSize), 8)
                    .choice("align", "Align", List.of("left", "center", "right"), e.align);
        }
        if (e.type.equals("input")) f.text("placeholder", "Placeholder", e.placeholder, 64);
        if (e.type.equals("item")) {
            f.text("item", "Item id or {hand} {helmet}...", e.item, 96).text("itemModel", "Item model (1.21.4+)", e.itemModel, 96)
                    .toggle("glint", "Enchant glint", e.glint);
        }
        if (e.type.equals("head") || e.type.equals("model")) f.text("headOwner", "Player (blank = viewer)", e.headOwner, 32);
        if (List.of("item", "head", "model").contains(e.type)) {
            f.text("spin", "Spin (degrees/second)", fmt(e.spin), 8).toggle("rotatable", "Scroll to rotate", e.rotatable);
        }
        if (e.type.equals("progress")) {
            f.text("value", "Value", e.value, 64).text("max", "Max", e.max, 64).text("barColor", "Bar colour", e.barColor, 16);
        }
        f.text("hoverFill", "Hover fill", e.hoverFill, 16).text("hoverScale", "Hover scale", fmt(e.hoverScale), 8)
                .text("group", "Group (tabset/page)", e.group, 64).text("tabFor", "Tab button for (tabset/page)", e.tabFor, 64)
                .text("activeFill", "Active tab fill", e.activeFill, 16);
        boolean ok = Dialogs.show(plugin, player, f, values -> {
            snapshot();
            apply(e, values);
            changed();
        });
        if (!ok) chatProperties(e);
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(Math.round(d * 1000) / 1000.0);
    }

    /** Applies "key -> value" edits. Unknown keys are ignored; bad numbers keep the old value. */
    public static void apply(ElementDef e, Map<String, String> v) {
        for (Map.Entry<String, String> en : v.entrySet()) set(e, en.getKey(), en.getValue());
    }

    public static boolean set(ElementDef e, String key, String val) {
        String s = val == null ? "" : val;
        switch (key) {
            case "name" -> e.name = s.isBlank() ? e.name : s;
            case "x" -> e.x = Text.num(s, e.x);
            case "y" -> e.y = Text.num(s, e.y);
            case "w" -> e.w = Math.max(1, Text.num(s, e.w));
            case "h" -> e.h = Math.max(1, Text.num(s, e.h));
            case "rotation" -> e.rotation = Text.num(s, e.rotation);
            case "opacity" -> e.opacity = Math.max(0, Math.min(1, Text.num(s, e.opacity)));
            case "scale" -> e.scale = Math.max(0.01, Text.num(s, e.scale));
            case "visible" -> e.visible = Boolean.parseBoolean(s);
            case "blocked" -> e.blocked = Boolean.parseBoolean(s);
            case "fillEnabled" -> e.fillEnabled = Boolean.parseBoolean(s);
            case "fill" -> e.fill = s;
            case "sprite" -> e.sprite = s;
            case "text" -> e.text = s;
            case "textColor" -> e.textColor = s;
            case "textSize" -> e.textSize = Math.max(0.1, Text.num(s, e.textSize));
            case "align" -> e.align = s.isEmpty() ? "center" : s;
            case "placeholder" -> e.placeholder = s;
            case "item" -> e.item = s.isEmpty() ? e.item : s;
            case "itemModel" -> e.itemModel = s;
            case "glint" -> e.glint = Boolean.parseBoolean(s);
            case "headOwner" -> e.headOwner = s;
            case "spin" -> e.spin = Text.num(s, e.spin);
            case "rotatable" -> e.rotatable = Boolean.parseBoolean(s);
            case "value" -> e.value = s;
            case "max" -> e.max = s;
            case "barColor" -> e.barColor = s;
            case "hoverFill" -> e.hoverFill = s;
            case "hoverScale" -> e.hoverScale = Math.max(0.1, Text.num(s, e.hoverScale));
            case "group" -> e.group = s.trim();
            case "tabFor" -> e.tabFor = s.trim();
            case "activeFill" -> e.activeFill = s;
            default -> {
                return false;
            }
        }
        return true;
    }

    /** /sui ed set <prop> <value> */
    public void setFromCommand(String key, String value) {
        ElementDef e = sel();
        if (e == null) {
            player.sendMessage(Text.mm("<gray>Select an element first."));
            return;
        }
        snapshot();
        if (!set(e, key, value)) {
            undo.pop();
            player.sendMessage(Text.mm("<red>Unknown property: " + esc(key)));
            return;
        }
        changed();
    }

    private void chatProperties(ElementDef e) {
        player.sendMessage(Text.mm("<white><bold>" + esc(e.name) + "</bold></white> <gray>click a value to change it:"));
        String[][] rows = {{"name", e.name}, {"x", fmt(e.x)}, {"y", fmt(e.y)}, {"w", fmt(e.w)}, {"h", fmt(e.h)},
                {"text", e.text}, {"textColor", e.textColor}, {"fill", e.fill}, {"sprite", e.sprite}, {"item", e.item},
                {"headOwner", e.headOwner}, {"visible", String.valueOf(e.visible)}, {"group", e.group}, {"tabFor", e.tabFor}};
        for (String[] r : rows) {
            String cmd = "/sui ed set " + r[0] + " " + (r[1] == null ? "" : r[1]);
            player.sendMessage(Component.text("  " + r[0] + ": ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .append(Component.text(r[1] == null || r[1].isEmpty() ? "(empty)" : r[1], net.kyori.adventure.text.format.NamedTextColor.WHITE)
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(cmd))));
        }
    }

    private void addDialog() {
        Dialogs.Form f = new Dialogs.Form("<white>Add element")
                .choice("type", "Type", List.of("rect", "text", "button", "image", "input", "progress", "item", "head", "model"), "button")
                .text("text", "Text", "New button", 128)
                .text("item", "Item (for item elements)", "minecraft:diamond", 64);
        boolean ok = Dialogs.show(plugin, player, f, v -> addElement(v.getOrDefault("type", "rect"), v.get("text"), v.get("item")));
        if (!ok) addElement("button", "New button", "minecraft:diamond");
    }

    private void addElement(String type, String text, String item) {
        snapshot();
        ElementDef e = new ElementDef();
        e.type = type.toLowerCase(Locale.ROOT);
        e.id = uniqueId(e.type);
        e.name = Character.toUpperCase(e.type.charAt(0)) + e.type.substring(1);
        e.w = switch (e.type) {
            case "item", "head" -> 24;
            case "model" -> 40;
            case "text" -> 120;
            default -> 80;
        };
        e.h = switch (e.type) {
            case "item", "head" -> 24;
            case "model" -> 80;
            case "text" -> 12;
            case "progress" -> 6;
            default -> 22;
        };
        e.x = Math.round(working.canvas.width / 2.0 - e.w / 2);
        e.y = Math.round(working.canvas.height / 2.0 - e.h / 2);
        e.fill = "#2a2a2a";
        if (List.of("text", "button", "input", "rect").contains(e.type) && text != null) e.text = text;
        if (e.type.equals("text") || e.type.equals("item") || e.type.equals("head") || e.type.equals("model")) e.fillEnabled = false;
        if (e.type.equals("item") && item != null && !item.isBlank()) e.item = item;
        // put new elements on the page being viewed
        if (!pages.isEmpty()) {
            Map.Entry<String, String> first = pages.entrySet().iterator().next();
            ElementDef s = sel();
            if (s != null && s.group != null && s.group.contains("/")) e.group = s.group;
            else if (working.elements.stream().anyMatch(x -> x.group.startsWith(first.getKey() + "/"))) e.group = "";
        }
        working.elements.add(e);
        selected = e.id;
        changed();
    }

    public String selectedId() {
        return selected;
    }

    boolean isHiddenInWorking(String id) {
        return hiddenInWorking.contains(id);
    }
}
