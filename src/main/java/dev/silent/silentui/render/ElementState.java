package dev.silent.silentui.render;

import dev.silent.silentui.model.UiDefinition.ElementDef;

/** Mutable runtime copy of an element (scripts and animations change this, renderers draw it). */
public final class ElementState {

    public final ElementDef def;
    public boolean visible;
    public double x, y, w, h, scale, rotation, opacity;
    public String fill, sprite, text, textColor, value, max;
    public boolean hovered;

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
        hovered = false;
    }

    public String type() {
        return def.type;
    }

    public boolean interactive() {
        return visible && !def.blocked && opacity > 0.01;
    }

    public double effectiveScale() {
        return scale * (hovered ? def.hoverScale : 1.0);
    }

    public String effectiveFill() {
        return hovered && def.hoverFill != null && !def.hoverFill.isEmpty() ? def.hoverFill : fill;
    }

    public String effectiveSprite() {
        return hovered && def.hoverSprite != null && !def.hoverSprite.isEmpty() ? def.hoverSprite : sprite;
    }

    public String effectiveTextColor() {
        return hovered && def.hoverTextColor != null && !def.hoverTextColor.isEmpty() ? def.hoverTextColor : textColor;
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
