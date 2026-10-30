package com.example.addon.gui;

import dev.boze.api.gui.editor.EditorButton;
import dev.boze.api.gui.kit.KitMetrics;
import dev.boze.api.gui.kit.Surface;
import dev.boze.api.gui.kit.TextRole;
import dev.boze.api.gui.kit.WidgetKit;
import dev.boze.api.gui.kit.WidgetState;
import dev.boze.api.gui.render.Corners;
import dev.boze.api.gui.render.GuiPaint;
import dev.boze.api.gui.render.GuiPainter;
import dev.boze.api.gui.util.Rect;
import dev.boze.api.gui.util.Spring;
import dev.boze.api.gui.util.TextFieldState;

/**
 * Every editor's pieces in the window's colors. Hovered pieces brighten, and switches slide.
 */
final class CsKit implements WidgetKit {
    private static final GuiPaint DESTRUCTIVE = GuiPaint.argb(0xFFE5484D);
    private static final GuiPaint BACKDROP = GuiPaint.argb(0x99000000);

    private final CsTheme theme;
    private final Spring springs = Spring.settling(0.3);

    CsKit(CsTheme theme) {
        this.theme = theme;
    }

    void forget() {
        springs.clear();
    }

    private CsColors colors() {
        return theme.colors();
    }

    private double u() {
        return theme.unit();
    }

    /**
     * @return 0 to 1, easing toward 1 while {@code on}, for a piece identified by {@code state}
     */
    private float ease(WidgetState state, String part, boolean on) {
        if (state.key() == null) return on ? 1F : 0F;
        return springs.update(new PartKey(state.key(), part), on);
    }

    private record PartKey(Object key, String part) {
    }

    private double radius(double units) {
        return u() * units;
    }

    @Override
    public KitMetrics metrics() {
        double u = u();
        return new KitMetrics(u, u * 0.45, u * 0.42, u * 16, CsView.SHADOW);
    }

    @Override
    public GuiPaint textPaint(TextRole role) {
        CsColors c = colors();
        return switch (role) {
            case NORMAL -> c.text();
            case MUTED -> c.muted();
            case ACCENT -> c.accent();
            case ON_ACCENT -> c.onAccent();
        };
    }

    @Override
    public void surface(GuiPainter painter, Surface surface, Rect b, WidgetState state) {
        CsColors c = colors();
        double u = u();
        switch (surface) {
            case PANEL -> painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(5), Corners.ALL, c.background());
            case LIST_AREA, BOX -> painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(4), Corners.ALL, c.inset());
            case ROW -> {
                float hover = ease(state, "hover", state.hovered());
                // A selected row is a dark shade of the accent with a bright accent bar at its left edge.
                GuiPaint fill = c.lift(state.selected() ? c.press(c.accent(), 0.7F) : c.raised(), 0.06F * hover);
                painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(3), Corners.ALL, fill);
                if (state.selected()) {
                    painter.roundRect(b.x() + u * 2, b.y() + u * 3, u * 1.5, b.height() - u * 6, u * 0.75, Corners.ALL, c.accent());
                }
                if (state.focused()) painter.strokeRoundRect(b.x(), b.y(), b.width(), b.height(), radius(3), Corners.ALL, u, c.accent());
            }
            case FIELD, AMOUNT -> {
                painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(4), Corners.ALL, c.inset());
                float focus = ease(state, "focus", state.focused());
                if (focus > 0.01F) {
                    painter.strokeRoundRect(b.x(), b.y(), b.width(), b.height(), radius(4), Corners.ALL, u,
                            c.accent().withAlpha(c.accent().alpha() * focus));
                }
            }
            case BUTTON -> painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(4), Corners.ALL,
                    c.lift(c.raised(), 0.1F * ease(state, "hover", state.hovered())));
            case TAB -> {
                float hover = ease(state, "hover", state.hovered());
                GuiPaint fill = state.selected() ? c.accent() : c.lift(c.raised(), 0.08F * hover);
                painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(3), Corners.ALL, fill);
            }
            case CHIP -> painter.roundRect(b.x(), b.y(), b.width(), b.height(), Math.min(b.width(), b.height()) / 2, Corners.ALL,
                    c.lift(c.inset(), 0.08F * ease(state, "hover", state.hovered())));
            case SCROLL_THUMB -> painter.roundRect(b.x(), b.y(), b.width(), b.height(), b.width() / 2, Corners.ALL,
                    c.accent().withAlpha(0.6F));
            case CARD -> {
                painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(6), Corners.ALL, c.background());
                painter.strokeRoundRect(b.x(), b.y(), b.width(), b.height(), radius(6), Corners.ALL, u, c.accent().withAlpha(0.5F));
            }
            case CARD_HEADER -> painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(6), Corners.TOP,
                    GuiPaint.horizontal(c.accent(), c.accent().withAlpha(0.35F)));
            case CARD_BUTTON -> {
                float hover = ease(state, "hover", state.hovered());
                if (hover > 0.01F) painter.circle(b.x(), b.y(), Math.min(b.width(), b.height()), c.onAccent().withAlpha(0.25F * hover));
            }
            case BACKDROP -> painter.rect(b.x(), b.y(), b.width(), b.height(), BACKDROP);
        }
    }

    @Override
    public void button(GuiPainter painter, Rect b, String label, EditorButton.Role role, WidgetState state) {
        CsColors c = colors();
        float hover = state.disabled() ? 0F : ease(state, "hover", state.hovered());
        GuiPaint fill = switch (role) {
            case PRIMARY -> c.accent();
            case DESTRUCTIVE -> DESTRUCTIVE;
            case SECONDARY -> c.raised();
        };
        fill = c.lift(fill, 0.12F * hover);
        if (state.pressed()) fill = c.press(fill, 0.15F);
        painter.pushAlpha(state.disabled() ? 0.45F : 1F);
        painter.roundRect(b.x(), b.y(), b.width(), b.height(), radius(4), Corners.ALL, fill);
        GuiPaint text = role == EditorButton.Role.SECONDARY ? c.text() : c.onAccent();
        double scale = metrics().textScale();
        double width = painter.textWidth(label, scale, CsView.SHADOW), height = painter.textHeight(scale, CsView.SHADOW);
        painter.text(label, b.centerX() - width / 2, b.centerY() - height / 2, scale, text, CsView.SHADOW);
        painter.popAlpha();
    }

    // Sliders: the label and value on top, the track below them between a minus and a plus.

    private Rect track(Rect b) {
        double u = u();
        return new Rect(b.x() + u * 14, b.y() + b.height() * 0.72 - u * 1.5, b.width() - u * 28, u * 3);
    }

    private void sliderText(GuiPainter painter, Rect b, String label, String value) {
        CsColors c = colors();
        double u = u(), scale = metrics().textScale();
        double th = painter.textHeight(scale, CsView.SHADOW);
        double textY = b.y() + b.height() * 0.32 - th / 2;
        painter.text(label, b.x() + u * 6, textY, scale, c.text(), CsView.SHADOW);
        painter.text(value, b.right() - u * 6 - painter.textWidth(value, scale, CsView.SHADOW), textY, scale, c.muted(), CsView.SHADOW);
        double signY = b.y() + b.height() * 0.72 - th / 2;
        painter.text("-", b.x() + u * 6, signY, scale, c.muted(), CsView.SHADOW);
        painter.text("+", b.right() - u * 6 - painter.textWidth("+", scale, CsView.SHADOW), signY, scale, c.muted(), CsView.SHADOW);
    }

    @Override
    public void slider(GuiPainter painter, Rect b, String label, String value, double fraction, WidgetState state) {
        CsColors c = colors();
        double u = u();
        sliderText(painter, b, label, value);
        Rect track = track(b);
        double f = Math.clamp(fraction, 0.0, 1.0);
        painter.roundRect(track.x(), track.y(), track.width(), track.height(), track.height() / 2, Corners.ALL, c.inset());
        painter.roundRect(track.x(), track.y(), track.width() * f, track.height(), track.height() / 2, Corners.ALL, c.accent());
        double knob = u * (5 + 1.5 * ease(state, "hover", state.hovered()));
        painter.circle(track.x() + track.width() * f - knob / 2, track.centerY() - knob / 2, knob, c.text());
    }

    @Override
    public void hueSlider(GuiPainter painter, Rect b, String label, String value, double hue, WidgetState state) {
        double u = u();
        sliderText(painter, b, label, value);
        Rect track = track(b);
        // Between two HSB colors the hue itself is blended, so one gradient runs through the rainbow.
        GuiPaint rainbow = GuiPaint.horizontal(GuiPaint.hsb(0F, 1F, 1F, 1F), GuiPaint.hsb(1F, 1F, 1F, 1F));
        painter.roundRect(track.x(), track.y(), track.width(), track.height(), track.height() / 2, Corners.ALL, rainbow);
        double knob = u * (5 + 1.5 * ease(state, "hover", state.hovered()));
        double knobX = track.x() + track.width() * Math.clamp(hue, 0.0, 1.0) - knob / 2;
        painter.circle(knobX, track.centerY() - knob / 2, knob, GuiPaint.hsb((float) hue, 1F, 1F, 1F));
        painter.strokeCircle(knobX, track.centerY() - knob / 2, knob, u, colors().text());
    }

    @Override
    public Rect sliderTrack(Rect b) {
        double u = u();
        Rect track = track(b);
        return new Rect(track.x(), track.centerY() - u * 4, track.width(), u * 8);
    }

    @Override
    public Rect sliderStep(GuiPainter painter, Rect b, int direction) {
        double u = u(), scale = metrics().textScale();
        double th = painter.textHeight(scale, CsView.SHADOW);
        double y = b.y() + b.height() * 0.72 - th / 2 - u;
        return direction < 0 ? new Rect(b.x() + u * 4, y, u * 8, th + u * 2) : new Rect(b.right() - u * 12, y, u * 8, th + u * 2);
    }

    @Override
    public void toggle(GuiPainter painter, Rect b, String label, boolean on, WidgetState state) {
        CsColors c = colors();
        double u = u(), scale = metrics().textScale();
        painter.text(label, b.x() + u * 6, b.centerY() - painter.textHeight(scale, CsView.SHADOW) / 2, scale, c.text(), CsView.SHADOW);
        drawSwitch(painter, b.right() - u * 6 - u * 16, b.centerY() - u * 4.5, u, on, ease(state, "on", on), c);
    }

    /**
     * A switch 16 by 9 units at (x, y), its knob at {@code position} from off to on.
     */
    static void drawSwitch(GuiPainter painter, double x, double y, double u, boolean on, float position, CsColors c) {
        painter.roundRect(x, y, u * 16, u * 9, u * 4.5, Corners.ALL, c.inset());
        painter.roundRect(x, y, u * 16, u * 9, u * 4.5, Corners.ALL, c.accent().withAlpha(c.accent().alpha() * position));
        double knob = u * 7;
        painter.circle(x + u + (u * 16 - knob - u * 2) * position, y + u, knob, on ? c.onAccent() : c.text());
    }

    @Override
    public void saturationBrightness(GuiPainter painter, Rect b, float hue, float saturation, float brightness, WidgetState state) {
        double u = u();
        painter.gradient(b.x(), b.y(), b.width(), b.height(), GuiPaint.hsb(hue, 0F, 1F, 1F), GuiPaint.hsb(hue, 1F, 1F, 1F),
                GuiPaint.hsb(hue, 1F, 0F, 1F), GuiPaint.hsb(hue, 0F, 0F, 1F));
        double size = u * 6;
        double markerX = b.x() + saturation * b.width() - size / 2, markerY = b.y() + (1 - brightness) * b.height() - size / 2;
        painter.strokeCircle(markerX, markerY, size, u * 1.5, GuiPaint.argb(0xFFFFFFFF));
        painter.strokeCircle(markerX + u * 1.5, markerY + u * 1.5, size - u * 3, u * 0.75, GuiPaint.argb(0xFF000000));
    }

    @Override
    public Rect saturationBrightnessArea(Rect b) {
        return b;
    }

    @Override
    public void textField(GuiPainter painter, Rect b, TextFieldState field, String placeholder, boolean masked, double textScale,
                          WidgetState state) {
        CsColors c = colors();
        double u = u();
        surface(painter, Surface.FIELD, b, state);
        double th = painter.textHeight(textScale, CsView.SHADOW);
        double textX = b.x() + u * 5, textY = b.centerY() - th / 2;
        painter.pushClip(b.x() + u * 2, b.y(), b.width() - u * 4, b.height());
        try {
            if (field.isEmpty() && placeholder != null) painter.text(placeholder, textX, textY, textScale, c.muted(), CsView.SHADOW);
            String before = masked ? "*".repeat(field.beforeCaret().length()) : field.beforeCaret();
            String after = masked ? "*".repeat(field.afterCaret().length()) : field.afterCaret();
            double beforeWidth = painter.textWidth(before, textScale, CsView.SHADOW);
            double shift = Math.max(0, beforeWidth + u * 2 - (b.width() - u * 10));
            painter.text(before + after, textX - shift, textY, textScale, c.text(), CsView.SHADOW);
            if (state.focused() && System.currentTimeMillis() % 1000 < 500) {
                painter.rect(textX - shift + beforeWidth, textY, Math.max(1, u * 0.6), th, c.accent());
            }
        } finally {
            painter.popClip();
        }
    }
}
