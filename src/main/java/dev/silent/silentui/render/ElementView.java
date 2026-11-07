package dev.silent.silentui.render;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.util.FontWidths;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The display entities drawing one element. Only sends what actually changed, so idle UIs cost no packets.
 */
final class ElementView {

    private final PlaneUi ui;
    private final ElementState s;
    private final int layer;

    private TextDisplay background;   // rect / image / button / input / text bg / progress track
    private TextDisplay fillBar;      // progress fill
    private TextDisplay label;        // text
    private ItemDisplay item;         // item / head

    private final Cache bgCache = new Cache(), fillCache = new Cache(), labelCache = new Cache(), itemCache = new Cache();
    private String lastResolvedText;
    private String headOwnerApplied;

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
            case "item", "head" -> item = ui.spawnDisplay(ItemDisplay.class, d -> {
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
                d.setItemStack(buildItem());
            });
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
                if (!s.def.text.isEmpty()) label = ui.spawnDisplay(TextDisplay.class, d -> { });
            }
        }
        if (item != null && "head".equals(s.type())) resolveHead();
    }

    private boolean hasBackground() {
        return s.def.fillEnabled || !s.def.sprite.isEmpty();
    }

    void remove() {
        if (background != null) background.remove();
        if (fillBar != null) fillBar.remove();
        if (label != null) label.remove();
        if (item != null) item.remove();
    }

    // ------------------------------------------------------------------ updates

    void update(int interp) {
        if (background != null) updateGlyph(background, bgCache, s.effectiveSprite(), s.effectiveFill(),
                s.x, s.y, s.w, s.h, 0, interp, s.def.fillEnabled || !s.effectiveSprite().isEmpty());
        if (fillBar != null) {
            double f = progressFraction();
            String sprite = s.def.barSprite;
            updateGlyph(fillBar, fillCache, sprite, s.def.barColor, s.x, s.y, s.w * f, s.h, 1, interp, f > 0);
        }
        if (label != null) updateLabel(interp);
        if (item != null) {
            Transformation t = ui.itemTransform(s, layer);
            applyTransform(item, itemCache, t, interp);
        }
    }

    void refreshText() {
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
        lastResolvedText = plain;
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

    // ------------------------------------------------------------------ items

    private ItemStack buildItem() {
        if ("head".equals(s.type())) return new ItemStack(Material.PLAYER_HEAD);
        Material m = Material.matchMaterial(s.def.item == null ? "" : s.def.item);
        if (m == null || !m.isItem() || m.isAir()) m = Material.BARRIER;
        return new ItemStack(m);
    }

    private void resolveHead() {
        String owner = s.def.headOwner == null ? "" : s.def.headOwner.trim();
        Player viewer = ui.placeholderPlayer();
        if (owner.isEmpty() || owner.equalsIgnoreCase("{player}")) {
            if (viewer == null) return;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(viewer));
            item.setItemStack(head);
            headOwnerApplied = viewer.getName();
            return;
        }
        if (owner.equals(headOwnerApplied)) return;
        headOwnerApplied = owner;
        PlayerProfile profile = Bukkit.createProfile(owner);
        CompletableFuture.supplyAsync(() -> {
            profile.complete(true);
            return profile;
        }).thenAccept(done -> Bukkit.getScheduler().runTask(ui.plugin(), () -> {
            if (item == null || !item.isValid()) return;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(done));
            item.setItemStack(head);
        }));
    }

    String lastText() {
        return lastResolvedText;
    }
}
