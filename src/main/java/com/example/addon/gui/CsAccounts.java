package com.example.addon.gui;

import dev.boze.api.gui.editor.EditorButton;
import dev.boze.api.gui.editor.EditorView;
import dev.boze.api.gui.editor.GuiEditor;
import dev.boze.api.gui.editor.ListEditor;
import dev.boze.api.gui.editor.ListEntry;
import dev.boze.api.gui.editor.RowAction;
import dev.boze.api.gui.input.GuiCursor;
import dev.boze.api.gui.input.GuiMouseEvent;
import dev.boze.api.gui.input.GuiScrollEvent;
import dev.boze.api.gui.render.Corners;
import dev.boze.api.gui.render.GuiIcon;
import dev.boze.api.gui.render.GuiPaint;
import dev.boze.api.gui.render.GuiPainter;
import dev.boze.api.gui.util.Rect;
import dev.boze.api.gui.util.Spring;
import dev.boze.api.gui.util.TextLayout;
import dev.boze.api.gui.util.Zones;
import dev.boze.api.gui.view.GuiFrame;

import java.util.List;
import java.util.function.Predicate;

import static com.example.addon.gui.CsView.SHADOW;

/**
 * The account manager laid out like a module's settings: a header with the editor's buttons as
 * chips, then the saved accounts with their faces, the one in use marked like the selected module.
 * Docked it fills the settings column; on screens without the window it floats as a small window
 * of its own.
 */
final class CsAccounts implements EditorView {
    static final String ID = "boze:accounts";
    private static final double WIDTH = 230, HEIGHT = 250, ROW = 24, GAP = 2, FACE = 14, STATUS = 14;
    private static final GuiPaint DESTRUCTIVE = GuiPaint.argb(0xFFE5484D);

    private final CsTheme theme;
    private final ListEditor editor;
    private final Spring springs = Spring.settling(0.3);
    private final Zones zones = new Zones();
    private double x, y, width, height;
    private boolean docked;
    private double scroll, maxScroll;
    private Rect list;

    // The frame being drawn
    private GuiFrame frame;
    private GuiPainter p;
    private CsColors c;
    private double u, ts, th, small, smallTh, title, titleTh, is, ih;
    private String status;

    CsAccounts(CsTheme theme, ListEditor editor) {
        this.theme = theme;
        this.editor = editor;
    }

    @Override
    public GuiEditor editor() {
        return editor;
    }

    @Override
    public void measure(GuiFrame frame, double maxWidth, double maxHeight, boolean fill) {
        docked = fill;
        double u = theme.unit();
        width = fill ? maxWidth : Math.min(WIDTH * u, maxWidth);
        height = fill ? maxHeight : Math.min(HEIGHT * u, maxHeight);
    }

    @Override
    public double width() {
        return width;
    }

    @Override
    public double height() {
        return height;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Drawing

    @Override
    public void render(GuiFrame frame) {
        this.frame = frame;
        p = frame.painter();
        c = theme.colors();
        u = theme.unit();
        ts = u * 0.45;
        small = u * 0.38;
        title = u * 0.6;
        is = u * 0.45;
        th = p.textHeight(ts, SHADOW);
        smallTh = p.textHeight(small, SHADOW);
        titleTh = p.textHeight(title, SHADOW);
        ih = p.iconHeight(is, SHADOW);
        status = null;

        zones.begin();
        Rect area = new Rect(x, y, width, height);
        // Clicks anywhere on it stay on it; added first so everything drawn sits above.
        zones.add(area, event -> true, null);
        boolean glowAll = theme.textGlow() == CsTheme.TextGlow.All;
        if (glowAll) p.pushTextGlow(CsView.TEXT_GLOW_SPREAD, CsView.TEXT_GLOW_STRENGTH);
        try {
            if (!docked) drawWindow(area);
            drawContent(area);
        } finally {
            if (glowAll) p.popTextGlow();
        }
        zones.end();
        GuiCursor cursor = zones.cursorAt(frame.mouseX(), frame.mouseY());
        if (cursor != null) frame.session().input().setCursor(cursor);
    }

    /**
     * The window's background, blur, glow and outline, and a close button, for when there is no
     * window to dock in.
     */
    private void drawWindow(Rect window) {
        double r = u * 7;
        float glass = theme.blur() ? CsView.GLASS : 1F;
        if (theme.blur()) p.backdropBlur(window.x(), window.y(), window.width(), window.height(), r, Corners.ALL, theme.blurStrength());
        if (theme.glow()) p.shadow(window.x(), window.y(), window.width(), window.height(), r, theme.glowSize() * 2 * u, c.accent());
        if (theme.outline()) {
            p.strokeRoundRect(window.x() - u, window.y() - u, window.width() + u * 2, window.height() + u * 2, r + u, Corners.ALL, u, c.accent());
        }
        p.roundRect(window.x(), window.y(), window.width(), window.height(), r, Corners.ALL, c.background().withAlpha(c.background().alpha() * glass));

        double size = u * 14;
        Rect close = new Rect(window.right() - size - u * 5, window.y() + u * 5, size, size);
        float hover = fade("close", hovered(close, null));
        if (hover > 0.01F) p.circle(close.x(), close.y(), size, c.raised().withAlpha(c.raised().alpha() * hover));
        p.text("x", close.centerX() - p.textWidth("x", ts, SHADOW) / 2, close.centerY() - th / 2, ts, hover > 0.5F ? c.text() : c.muted(), SHADOW);
        zones.add(close, event -> {
            if (!event.isLeft()) return false;
            frame.session().input().clickSound();
            editor.close();
            return true;
        }, GuiCursor.POINTER);
    }

    private void drawContent(Rect area) {
        List<ListEntry> entries = editor.entries();
        double left = area.x() + u * 10, right = area.right() - u * (docked ? 10 : 24);
        double top = area.y() + u * (docked ? 4 : 8);

        String heading = "Accounts", count = entries.size() == 1 ? "1 saved" : entries.size() + " saved";
        double headingWidth = p.textWidth(heading, title, SHADOW);
        p.text(heading, left, top, title, c.text(), SHADOW);
        p.text(fit(count, right - left - headingWidth - u * 6, small), left + headingWidth + u * 6, top + (titleTh - smallTh) / 2, small,
                c.muted(), SHADOW);
        double y = top + titleTh + u * 2;
        p.text(fit("Click an account to log in with it", area.right() - u * 10 - left, small), left, y, small, c.muted(), SHADOW);
        y += smallTh + u * 5;

        // Close is the back strip when docked and the window's own button when floating.
        double chipX = left;
        for (EditorButton button : editor.buttons()) {
            if (button.role() == EditorButton.Role.SECONDARY) continue;
            chipX = chip(button, chipX, y).right() + u * 4;
        }
        y += u * 18;

        p.rect(area.x() + u * 10, y, area.width() - u * 20, Math.max(1, u * 0.5), c.line());
        list = new Rect(area.x(), y + u * 2, area.width(), area.bottom() - y - u * 2 - STATUS * u);
        drawRows(entries);
        drawStatus(area);
    }

    private Rect chip(EditorButton button, double x, double y) {
        String label = button.label();
        Rect chip = new Rect(x, y, p.textWidth(label, small, SHADOW) + u * 12, u * 12);
        boolean enabled = button.isEnabled(), primary = button.role() == EditorButton.Role.PRIMARY;
        boolean over = enabled && hovered(chip, null);
        float hover = fade("chip." + label, over);
        GuiPaint fill = switch (button.role()) {
            case PRIMARY -> c.lift(c.accent(), 0.12F * hover);
            case DESTRUCTIVE -> c.lift(DESTRUCTIVE, 0.12F * hover);
            case SECONDARY -> c.lift(c.inset(), 0.1F * hover);
        };
        p.pushAlpha(enabled ? 1F : 0.45F);
        try {
            p.roundRect(chip.x(), chip.y(), chip.width(), chip.height(), chip.height() / 2, Corners.ALL, fill);
            p.text(label, chip.x() + u * 6, chip.centerY() - smallTh / 2, small, primary ? c.onAccent() : c.text(), SHADOW);
        } finally {
            p.popAlpha();
        }
        if (enabled) zones.add(chip, event -> left(event, button::press), GuiCursor.POINTER);
        if (over && primary) status = "Add a Microsoft, Altening or cracked account";
        return chip;
    }

    /**
     * The rows, scrolling when they do not fit. Rows glide up into the place of one removed above them.
     */
    private void drawRows(List<ListEntry> entries) {
        double pitch = (ROW + GAP) * u;
        maxScroll = Math.max(0, u * 4 + entries.size() * pitch - list.height());
        scroll = Math.clamp(scroll, 0, maxScroll);
        double offset = springs.update("scroll", (float) scroll);

        if (entries.isEmpty()) {
            drawEmpty();
            return;
        }
        p.pushClip(list.x(), list.y(), list.width(), list.height());
        try {
            for (int i = 0; i < entries.size(); i++) {
                ListEntry entry = entries.get(i);
                double rowY = list.y() + u * 4 + springs.update("slot." + entry.key(), i) * pitch - offset;
                if (rowY + ROW * u < list.y() || rowY > list.bottom()) continue;
                drawRow(entry, new Rect(list.x() + u * 8, rowY, list.width() - u * 16, ROW * u));
            }
        } finally {
            p.popClip();
        }
    }

    private void drawRow(ListEntry entry, Rect row) {
        boolean hovered = hovered(row, list), active = entry.selected();
        float hover = fade("row." + entry.key(), hovered), lit = fade("active." + entry.key(), active);
        float fill = Math.max(lit, hover * 0.7F);
        if (fill > 0.01F) {
            p.roundRect(row.x(), row.y(), row.width(), row.height(), u * 4, Corners.ALL, c.raised().withAlpha(c.raised().alpha() * fill));
        }
        if (lit > 0.01F) {
            p.roundRect(row.x(), row.y() + u * 5, u * 2, row.height() - u * 10, u, Corners.ALL, c.accent().withAlpha(c.accent().alpha() * lit));
        }

        double face = FACE * u, faceX = row.x() + u * 8, faceY = row.centerY() - face / 2;
        p.roundRect(faceX - u, faceY - u, face + u * 2, face + u * 2, u * 3, Corners.ALL, c.inset());
        if (entry.image() != null) p.image(entry.image(), faceX, faceY, face);

        // Actions sit at the right, showing while the row is hovered; their room is kept either way
        // so the name does not shift.
        double iconRight = row.right() - u * 7, actionsLeft = row.right() - u * 3;
        boolean overAction = false;
        for (RowAction action : entry.actions()) {
            double iconWidth = p.iconWidth(action.icon(), is);
            Rect hit = new Rect(iconRight - iconWidth - u * 3, row.y(), iconWidth + u * 6, row.height());
            boolean over = hovered(hit, list);
            overAction |= over;
            if (hover > 0.01F) {
                GuiPaint paint = over ? action.icon() == GuiIcon.DELETE ? DESTRUCTIVE : c.text() : action.paint() != null ? action.paint() : c.muted();
                p.pushAlpha(hover);
                try {
                    p.icon(action.icon(), iconRight - iconWidth, row.centerY() - ih / 2, is, paint, SHADOW);
                } finally {
                    p.popAlpha();
                }
            }
            if (over) status = action.icon() == GuiIcon.DELETE ? "Remove this account" : null;
            actionsLeft = hit.x();
            iconRight -= iconWidth + u * 8;
        }

        double textX = faceX + face + u * 8, room = actionsLeft - textX - u * 2;
        String name = fit(entry.label(), room, ts);
        GuiPaint paint = active ? c.accent() : c.text();
        if (entry.detail() != null) {
            double nameY = row.centerY() - (th + u * 1.5 + smallTh) / 2;
            glowing(active, () -> p.text(name, textX, nameY, ts, paint, SHADOW));
            String detail = active ? entry.detail() + "  ·  In use" : entry.detail();
            p.text(fit(detail, room, small), textX, nameY + th + u * 1.5, small, entry.detailPaint() != null ? entry.detailPaint() : c.muted(),
                    SHADOW);
        } else {
            glowing(active, () -> p.text(name, textX, row.centerY() - th / 2, ts, paint, SHADOW));
        }
        if (hovered && !overAction) status = active ? "Logged in with this account" : "Click to log in as " + entry.label();

        // The row goes in first: the actions added after it sit on top and get their own clicks.
        if (entry.onClick() != null) add(row, event -> left(event, entry::click));
        iconRight = row.right() - u * 7;
        for (RowAction action : entry.actions()) {
            double iconWidth = p.iconWidth(action.icon(), is);
            add(new Rect(iconRight - iconWidth - u * 3, row.y(), iconWidth + u * 6, row.height()), event -> left(event, action::run));
            iconRight -= iconWidth + u * 8;
        }
    }

    private void drawEmpty() {
        String empty = "No saved accounts", hint = "Add one with the chip above";
        double y = list.y() + u * 14;
        p.text(empty, list.centerX() - p.textWidth(empty, ts, SHADOW) / 2, y, ts, c.muted(), SHADOW);
        p.text(hint, list.centerX() - p.textWidth(hint, small, SHADOW) / 2, y + th + u * 3, small, c.muted(), SHADOW);
    }

    private void drawStatus(Rect area) {
        if (status == null) return;
        p.text(fit(status, area.width() - u * 20, small), area.x() + u * 10, area.bottom() - STATUS * u * 0.5 - smallTh / 2, small,
                c.muted(), SHADOW);
    }

    // Helpers

    private void glowing(boolean highlighted, Runnable draw) {
        boolean glow = highlighted && theme.textGlow() == CsTheme.TextGlow.Accent;
        if (glow) p.pushTextGlow(CsView.TEXT_GLOW_SPREAD, CsView.TEXT_GLOW_STRENGTH);
        try {
            draw.run();
        } finally {
            if (glow) p.popTextGlow();
        }
    }

    private String fit(String text, double maxWidth, double scale) {
        return TextLayout.fit(text == null ? "" : text, Math.max(0, maxWidth), p.font(), scale, SHADOW);
    }

    private float fade(Object key, boolean on) {
        return springs.update(key, on);
    }

    private boolean hovered(Rect rect, Rect clip) {
        double mouseX = frame.mouseX(), mouseY = frame.mouseY();
        return rect.contains(mouseX, mouseY) && (clip == null || clip.contains(mouseX, mouseY));
    }

    /**
     * Adds a click area, cut to the visible part of the list.
     */
    private void add(Rect rect, Predicate<GuiMouseEvent> click) {
        double x0 = Math.max(rect.x(), list.x()), y0 = Math.max(rect.y(), list.y());
        double x1 = Math.min(rect.right(), list.right()), y1 = Math.min(rect.bottom(), list.bottom());
        if (x1 > x0 && y1 > y0) zones.add(new Rect(x0, y0, x1 - x0, y1 - y0), click, GuiCursor.POINTER);
    }

    private static boolean left(GuiMouseEvent event, Runnable action) {
        if (!event.isLeft()) return false;
        action.run();
        return true;
    }

    // Input

    @Override
    public boolean mouseClicked(GuiMouseEvent event) {
        if (!contains(event.x(), event.y())) return false;
        zones.click(event);
        return true;
    }

    @Override
    public boolean mouseScrolled(GuiScrollEvent event) {
        if (!contains(event.x(), event.y())) return false;
        if (list != null && list.contains(event.x(), event.y())) scroll = Math.clamp(scroll - event.vertical() * u * 24, 0, maxScroll);
        return true;
    }
}
