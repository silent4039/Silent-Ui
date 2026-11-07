package dev.silent.silentui.render;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.util.FontWidths;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The entities drawing one element. Only sends what actually changed, so idle UIs cost no packets.
 */
final class ElementView {

    private final PlaneUi ui;
    private final ElementState s;
    private final int layer;

    private TextDisplay background;   // rect / image / button / input / text bg / progress track
    private TextDisplay fillBar;      // progress fill
    private TextDisplay label;        // text
    private ItemDisplay item;         // item / head
    private LivingEntity model;       // player model (mannequin)

    private final Cache bgCache = new Cache(), fillCache = new Cache(), labelCache = new Cache(), itemCache = new Cache();
    private String itemApplied;
    private ItemStack itemStackApplied;
    private String profileApplied;
    private Location modelLoc;
    private double modelScale = -1;
    private boolean modelHidden;

    private static final class Cache {
        Transformation transform;
        Component text;
        byte opacity = -1;
        boolean opacitySet;
        Boolean shadow;
        TextDisplay.TextAlignment align;
        int lineWidth = -1;
    }

    ElementView(PlaneUi ui, ElementState s, int layer) {
        this.ui = ui;
        this.s = s;
        this.layer = layer;
    }

    void spawn() {
        switch (s.type()) {
            case "item", "head" -> {
                if (s.def.fillEnabled) background = ui.spawnDisplay(TextDisplay.class, d -> { });
                item = ui.spawnDisplay(ItemDisplay.class, d -> {
                    d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
                    d.setItemStack(new ItemStack(Material.AIR));
                });
            }
            case "model" -> {
                if (s.def.fillEnabled) background = ui.spawnDisplay(TextDisplay.class, d -> { });
                model = ui.spawnModel();
                if (model == null) { // server without mannequins: fall back to a head
                    item = ui.spawnDisplay(ItemDisplay.class, d -> d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI));
                }
            }
            case "text" -> {
                if (hasBackground()) background = ui.spawnDisplay(TextDisplay.class, d -> { });
                label = ui.spawnDisplay(TextDisplay.class, d -> { });
            }
            case "progress" -> {
                if (s.def.fillEnabled || !s.def.sprite.isEmpty()) background = ui.spawnDisplay(TextDisplay.class, d -> { });
                fillBar = ui.spawnDisplay(TextDisplay.class, d -> { });
            }
            case "button", "input" -> {
                background = ui.spawnDisplay(TextDisplay.class, d -> { });
                label = ui.spawnDisplay(TextDisplay.class, d -> { });
            }
            default -> { // rect, image
                background = ui.spawnDisplay(TextDisplay.class, d -> { });
                // rects always get a label so "Set text" works later; images only when they start with text
                if (!"image".equals(s.type()) || !s.def.text.isEmpty()) label = ui.spawnDisplay(TextDisplay.class, d -> { });
            }
        }
    }

    private boolean hasBackground() {
        return s.def.fillEnabled || !s.def.sprite.isEmpty();
    }

    void remove() {
        for (Entity e : new Entity[]{background, fillBar, label, item, model}) if (e != null) e.remove();
    }

    boolean valid() {
        for (Entity e : new Entity[]{background, fillBar, label, item, model}) if (e != null && !e.isValid()) return false;
        return true;
    }

    // ------------------------------------------------------------------ updates

    void update(int interp) {
        if (background != null) {
            boolean show = s.def.fillEnabled || !s.effectiveSprite().isEmpty();
            updateGlyph(background, bgCache, s.effectiveSprite(), s.effectiveFill(), s.x, s.y, s.w, s.h, 0, interp, show);
        }
        if (fillBar != null) {
            double f = progressFraction();
            updateGlyph(fillBar, fillCache, s.def.barSprite, s.def.barColor, s.x, s.y, s.w * f, s.h, 1, interp, f > 0);
        }
        if (label != null) updateLabel(interp);
        if (item != null) {
            applyItem();
            applyTransform(item, itemCache, ui.itemTransform(s, layer), interp);
        }
        if (model != null) updateModel();
    }

    void refreshText() {
        if (item != null && s.item != null && s.item.indexOf('{') >= 0) applyItem();
        if (label == null && fillBar == null) return;
        String raw = labelSource();
        boolean dynamic = raw.indexOf('{') >= 0 || raw.indexOf('%') >= 0
                || (fillBar != null && (s.value.indexOf('{') >= 0 || s.value.indexOf('%') >= 0 || s.max.indexOf('%') >= 0));
        if (dynamic) update(1);
    }

    private double progressFraction() {
        Player p = ui.placeholderPlayer();
        Map<String, String> vars = p == null ? null : ui.plugin().vars(p);
        double v = Text.num(Text.apply(s.value, p, vars), 0);
        double m = Text.num(Text.apply(s.max, p, vars), 100);
        if (m <= 0) return 0;
        return Math.max(0, Math.min(1, v / m));
    }

    private void updateGlyph(TextDisplay d, Cache c, String sprite, String color, double rx, double ry, double rw, double rh,
                             double zExtra, int interp, boolean show) {
        GlyphRegistry glyphs = ui.plugin().glyphs();
        GlyphRegistry.UiGlyph g = glyphs.sprite(ui.def.id, sprite);
        boolean isSprite = g != null;
        if (g == null) g = glyphs.pixel();
        if (g == null) return;
        TextColor tint = isSprite ? TextColor.color(0xFFFFFF) : Text.color(color, 0xFFFFFF);
        Component comp = Component.text(String.valueOf(g.ch())).font(GlyphRegistry.FONT_UI).color(tint);
        if (!comp.equals(c.text)) {
            d.text(comp);
            c.text = comp;
        }
        applyOpacity(d, c, s.opacity);
        Transformation t = show ? ui.glyphTransform(s, rx, ry, rw, rh, g, layer, zExtra * 0.3) : PlaneUi.zero();
        applyTransform(d, c, t, interp);
    }

    private String labelSource() {
        if ("input".equals(s.type()) && (s.text == null || s.text.isEmpty())) return s.def.placeholder;
        return s.text == null ? "" : s.text;
    }

    private void updateLabel(int interp) {
        Player p = ui.placeholderPlayer();
        String raw = labelSource();
        String resolved = Text.apply(raw, p, p == null ? null : ui.plugin().vars(p));
        boolean placeholderStyle = "input".equals(s.type()) && (s.text == null || s.text.isEmpty());
        TextColor color = placeholderStyle ? TextColor.color(0x9a9a9a) : Text.color(s.effectiveTextColor(), 0xFFFFFF);
        Component comp = Text.mm(resolved).colorIfAbsent(color);
        if (!comp.equals(labelCache.text)) {
            label.text(comp);
            labelCache.text = comp;
        }
        if (labelCache.shadow == null || labelCache.shadow != s.def.shadow) {
            label.setShadowed(s.def.shadow);
            labelCache.shadow = s.def.shadow;
        }
        TextDisplay.TextAlignment align = switch (s.def.align) {
            case "left" -> TextDisplay.TextAlignment.LEFT;
            case "right" -> TextDisplay.TextAlignment.RIGHT;
            default -> TextDisplay.TextAlignment.CENTER;
        };
        if (labelCache.align != align) {
            label.setAlignment(align);
            labelCache.align = align;
        }
        int padding = "text".equals(s.type()) ? 0 : 6;
        int lineWidth = (int) Math.max(8, (s.w - padding * 2) / s.def.textSize);
        if (labelCache.lineWidth != lineWidth) {
            label.setLineWidth(lineWidth);
            labelCache.lineWidth = lineWidth;
        }
        String plain = Text.plain(comp);
        int lines = FontWidths.lines(plain, lineWidth);
        int tw = 0;
        for (String line : plain.split("\n")) tw = Math.max(tw, Math.min(lineWidth, FontWidths.width(line, false)));
        double twUi = tw * s.def.textSize;
        double cx = switch (s.def.align) {
            case "left" -> s.x + padding + twUi / 2;
            case "right" -> s.x + s.w - padding - twUi / 2;
            default -> s.x + s.w / 2;
        };
        double cy = s.y + s.h / 2;
        applyOpacity(label, labelCache, s.opacity);
        applyTransform(label, labelCache, ui.textTransform(s, cx, cy, lines, layer), interp);
    }

    private void applyOpacity(TextDisplay d, Cache c, double opacity) {
        byte b;
        if (opacity >= 0.995) b = (byte) 255;
        else b = (byte) Math.max(4, (int) Math.round(opacity * 255)); // 0..3 render as fully opaque in vanilla
        if (!c.opacitySet || c.opacity != b) {
            d.setTextOpacity(b);
            c.opacity = b;
            c.opacitySet = true;
        }
    }

    private static void applyTransform(org.bukkit.entity.Display d, Cache c, Transformation t, int interp) {
        if (t.equals(c.transform)) return;
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(0, interp));
        d.setTransformation(t);
        c.transform = t;
    }

    // ------------------------------------------------------------------ items & heads

    private void applyItem() {
        if ("head".equals(s.type()) || "model".equals(s.type())) {
            applyHead();
            return;
        }
        Player viewer = ui.placeholderPlayer();
        String raw = s.item == null ? "" : s.item;
        boolean dynamic = raw.indexOf('{') >= 0;
        if (!dynamic && raw.equals(itemApplied)) return;
        ItemStack stack = resolveItem(raw, viewer);
        if (s.def.itemModel != null && !s.def.itemModel.isEmpty() || s.def.glint) {
            stack = stack.clone();
            stack.editMeta(meta -> {
                if (!s.def.itemModel.isEmpty()) {
                    NamespacedKey key = NamespacedKey.fromString(s.def.itemModel.toLowerCase(Locale.ROOT));
                    if (key != null) meta.setItemModel(key);
                }
                if (s.def.glint) meta.setEnchantmentGlintOverride(true);
            });
        }
        itemApplied = raw;
        if (stack.equals(itemStackApplied)) return;
        itemStackApplied = stack;
        item.setItemStack(stack);
    }

    /** "minecraft:diamond", "diamond", or the viewer's gear: {hand} {offhand} {helmet} {chestplate} {leggings} {boots} */
    static ItemStack resolveItem(String raw, Player viewer) {
        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (viewer != null && id.startsWith("{") && id.endsWith("}")) {
            PlayerInventory inv = viewer.getInventory();
            ItemStack it = switch (id) {
                case "{hand}", "{mainhand}" -> inv.getItemInMainHand();
                case "{offhand}" -> inv.getItemInOffHand();
                case "{helmet}", "{head}" -> inv.getHelmet();
                case "{chestplate}", "{chest}" -> inv.getChestplate();
                case "{leggings}", "{legs}" -> inv.getLeggings();
                case "{boots}", "{feet}" -> inv.getBoots();
                default -> null;
            };
            return it == null ? new ItemStack(Material.AIR) : it.clone();
        }
        Material m = Material.matchMaterial(id);
        if (m == null || !m.isItem() || m.isAir()) m = Material.BARRIER;
        return new ItemStack(m);
    }

    private String ownerName() {
        Player viewer = ui.placeholderPlayer();
        String owner = s.headOwner == null ? "" : s.headOwner.trim();
        if (owner.isEmpty() || owner.equalsIgnoreCase("{player}")) return viewer == null ? "" : viewer.getName();
        return Text.apply(owner, viewer, viewer == null ? null : ui.plugin().vars(viewer));
    }

    private void applyHead() {
        String owner = ownerName();
        if (Objects.equals(owner, profileApplied)) return;
        profileApplied = owner;
        if (owner.isEmpty()) {
            if (item != null) item.setItemStack(new ItemStack(Material.PLAYER_HEAD));
            return;
        }
        ui.plugin().profiles().get(owner, profile -> {
            if (!owner.equals(profileApplied)) return;
            if (item != null && item.isValid()) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                head.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(profile));
                item.setItemStack(head);
            }
            if (model != null && model.isValid()) applyProfile(model, profile);
        });
    }

    // ------------------------------------------------------------------ player model (mannequin)

    private void updateModel() {
        applyHead();
        boolean hide = !s.visible || s.opacity < 0.05 || s.effectiveScale() <= 0.0001;
        if (hide != modelHidden) {
            model.setInvisible(hide);
            modelHidden = hide;
        }
        double sc = s.effectiveScale();
        double heightBlocks = s.h * sc * ui.unit;
        double scale = Math.max(0.0625, Math.min(16, heightBlocks / 1.8));
        if (Math.abs(scale - modelScale) > 1e-4) {
            AttributeInstance a = model.getAttribute(Attribute.SCALE);
            if (a != null) a.setBaseValue(scale);
            modelScale = scale;
        }
        double[] foot = ui.warp(s, s.x + s.w / 2, s.y + s.h);
        Vector p = ui.worldPoint(foot[0], foot[1]).add(ui.normal.clone().multiply(ui.layerZ(layer, 0.5) + 0.05 * scale));
        float yaw = (float) (ui.displayYaw + s.turn);
        Location loc = new Location(ui.origin.getWorld(), p.getX(), p.getY(), p.getZ(), yaw, 0);
        if (modelLoc == null || modelLoc.distanceSquared(loc) > 1e-8 || Math.abs(modelLoc.getYaw() - yaw) > 0.01) {
            model.teleport(loc);
            model.setBodyYaw(yaw);
            modelLoc = loc;
        }
    }

    /** Mannequin API differs between versions, so it is reached by reflection and failures are ignored. */
    static void applyProfile(Entity e, PlayerProfile profile) {
        try {
            Class<?> rp = Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile");
            Object resolvable = rp.getMethod("resolvableProfile", PlayerProfile.class).invoke(null, profile);
            for (Method m : e.getClass().getMethods()) {
                if (m.getName().equals("setProfile") && m.getParameterCount() == 1) {
                    Class<?> pt = m.getParameterTypes()[0];
                    if (pt.isInstance(resolvable)) {
                        m.invoke(e, resolvable);
                        return;
                    }
                    if (pt.isInstance(profile)) {
                        m.invoke(e, profile);
                        return;
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // old server
        }
    }
}
