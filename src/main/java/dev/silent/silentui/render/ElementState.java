package dev.silent.silentui.render;

import dev.silent.silentui.model.UiDefinition.ElementDef;

/** Mutable runtime copy of an element (scripts and animations change this, renderers draw it). */
public final class ElementState {

    public final ElementDef def;
    public boolean visible;
    public double x, y, w, h, scale, rotation, opacity;
    public String fill, sprite, text, textColor, value, max, item, headOwner;
    public boolean hovered;
    /** Tab button whose page is showing. */
    public boolean active;
    /** Extra yaw (degrees) for item / head / model elements: spin + scroll rotation. */
    public double turn;

    public ElementState(ElementDef def) {
        this.def = def;
        reset();
    }

    public void reset() {
        visible = def.visible;
        x = def.x;
        y = def.y;
        w = def.w;
        h = def.h;
        scale = def.scale;
        rotation = def.rotation;
        opacity = def.opacity;
        fill = def.fill;
        sprite = def.sprite;
        text = def.text;
        textColor = def.textColor;
        value = def.value;
        max = def.max;
        item = def.item;
        headOwner = def.headOwner;
        hovered = false;
        active = false;
        turn = 0;
    }

    public String type() {
        return def.type;
    }

    public boolean interactive() {
        return visible && !def.blocked && opacity > 0.01;
    }

    /** True if this element belongs to the group (exact name or a "group/page" below it). */
    public boolean inGroup(String group) {
        if (group == null || group.isEmpty() || def.group == null || def.group.isEmpty()) return false;
        return def.group.equals(group) || def.group.startsWith(group + "/");
    }

    public double effectiveScale() {
        return scale * (hovered ? def.hoverScale : 1.0);
    }

    private static boolean has(String s) {
        return s != null && !s.isEmpty();
    }

    public String effectiveFill() {
        if (hovered && has(def.hoverFill)) return def.hoverFill;
        if (active && has(def.activeFill)) return def.activeFill;
        return fill;
    }

    public String effectiveSprite() {
        if (hovered && has(def.hoverSprite)) return def.hoverSprite;
        if (active && has(def.activeSprite)) return def.activeSprite;
        return sprite;
    }

    public String effectiveTextColor() {
        if (hovered && has(def.hoverTextColor)) return def.hoverTextColor;
        if (active && has(def.activeTextColor)) return def.activeTextColor;
        return textColor;
    }

    /** Pivot (anchor) point in canvas pixels. */
    public double[] pivot() {
        String a = def.anchor == null ? "center" : def.anchor;
        double px = x + w / 2, py = y + h / 2;
        if (a.contains("left")) px = x;
        if (a.contains("right")) px = x + w;
        if (a.startsWith("top")) py = y;
        if (a.startsWith("bottom")) py = y + h;
        return new double[]{px, py};
    }

    /** Is the canvas point inside this element (respecting scale + rotation)? */
    public boolean contains(double ux, double uy) {
        double[] p = pivot();
        double s = effectiveScale();
        if (s <= 0.0001) return false;
        double dx = ux - p[0], dy = uy - p[1];
        if (rotation != 0) {
            double r = Math.toRadians(-rotation);
            double c = Math.cos(r), sn = Math.sin(r);
            double nx = dx * c - dy * sn, ny = dx * sn + dy * c;
            dx = nx;
            dy = ny;
        }
        double lx = p[0] + dx / s, ly = p[1] + dy / s;
        return lx >= x && lx <= x + w && ly >= y && ly <= y + h;
    }
}
