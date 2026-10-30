package com.example.addon.gui;

import dev.boze.api.gui.editor.EditorButton;
import dev.boze.api.gui.editor.FormField;
import dev.boze.api.gui.editor.GuiEditor;
import dev.boze.api.gui.editor.GuiEditors;
import dev.boze.api.gui.input.GuiCursor;
import dev.boze.api.gui.input.GuiDrag;
import dev.boze.api.gui.input.GuiMouseEvent;
import dev.boze.api.gui.model.ActionSetting;
import dev.boze.api.gui.model.BindSetting;
import dev.boze.api.gui.model.ColorSetting;
import dev.boze.api.gui.model.EditorSetting;
import dev.boze.api.gui.model.GroupSetting;
import dev.boze.api.gui.model.GuiModule;
import dev.boze.api.gui.model.GuiSetting;
import dev.boze.api.gui.model.ModeSetting;
import dev.boze.api.gui.model.ModuleKind;
import dev.boze.api.gui.model.NumberSetting;
import dev.boze.api.gui.model.PageSetting;
import dev.boze.api.gui.model.RangeSetting;
import dev.boze.api.gui.model.TextSetting;
import dev.boze.api.gui.model.ToggleSetting;
import dev.boze.api.gui.render.Corners;
import dev.boze.api.gui.render.GuiIcon;
import dev.boze.api.gui.render.GuiPaint;
import dev.boze.api.gui.util.Rect;
import dev.boze.api.gui.util.TextFieldState;
import dev.boze.api.gui.util.TextLayout;
import dev.boze.api.utility.input.Bind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The settings column: the selected module's header and its settings, or a strip to go back while
 * an editor fills the column.
 */
final class CsSettings {
    private static final double ROW = 18, SLIDER_ROW = 26, STATUS = 14, DOTS = 9;

    private final CsView view;
    private Rect list;
    private double scroll, maxScroll;
    private GuiSetting revealing;

    CsSettings(CsView view) {
        this.view = view;
    }

    void resetScroll() {
        scroll = 0;
    }

    /**
     * Opens the page and parents of the module's first setting the search found, and scrolls to it.
     */
    void reveal(GuiModule module) {
        revealing = null;
        for (GuiSetting setting : module.settings()) {
            if (!setting.isVisible() || !view.matchesSearch(setting)) continue;
            PageSetting page = setting instanceof PageSetting own ? own : setting.page();
            if (page != null) {
                if (!page.isOpen()) page.open();
            } else if (module.openPage() != null) {
                module.openPage().close();
            }
            for (GuiSetting parent = setting.parent(); parent != null; parent = parent.parent()) parent.setExpanded(true);
            revealing = setting;
            return;
        }
    }

    boolean scroll(double x, double y, double amount) {
        if (list == null || !list.contains(x, y)) return false;
        scroll = Math.clamp(scroll - amount, 0, maxScroll);
        return true;
    }

    void draw(Rect area) {
        list = null;
        if (!view.session.editors().isEmpty()) {
            drawBackStrip(area);
            return;
        }
        GuiModule module = view.selectedModule();
        if (module == null) {
            String empty = "Select a module";
            view.p.text(empty, area.centerX() - view.p.textWidth(empty, view.ts, CsView.SHADOW) / 2, area.centerY() - view.th / 2, view.ts,
                    view.c.muted(), CsView.SHADOW);
            drawStatus(area);
            return;
        }
        double u = view.u;
        double y = drawHeader(module, area);
        view.p.rect(area.x() + u * 10, y, area.width() - u * 20, Math.max(1, u * 0.5), view.c.line());
        list = new Rect(area.x(), y + u * 2, area.width(), area.bottom() - y - u * 2 - STATUS * u);
        drawSettings(module);
        drawStatus(area);
    }

    // Editors

    private void drawBackStrip(Rect area) {
        double u = view.u;
        String label = "< Back";
        Rect back = new Rect(area.x() + u * 6, area.y() + u * 3, view.p.textWidth(label, view.small, CsView.SHADOW) + u * 12, u * 12);
        float hover = view.fade("back", back.contains(view.frame.mouseX(), view.frame.mouseY()));
        view.p.roundRect(back.x(), back.y(), back.width(), back.height(), back.height() / 2, Corners.ALL,
                view.c.lift(view.c.inset(), 0.1F * hover));
        view.p.text(label, back.x() + u * 6, back.centerY() - view.smallTh / 2, view.small, view.c.text(), CsView.SHADOW);
        view.zones.add(back, event -> {
            if (!event.isLeft()) return false;
            view.session.editors().pop();
            view.session.input().clickSound();
            return true;
        }, GuiCursor.POINTER);
        GuiModule module = view.selectedModule();
        if (module != null) {
            String title = view.fit(module.label(), area.right() - back.right() - u * 16, view.small);
            view.p.text(title, area.right() - u * 8 - view.p.textWidth(title, view.small, CsView.SHADOW), back.centerY() - view.smallTh / 2,
                    view.small, view.c.muted(), CsView.SHADOW);
        }
    }

    // Header

    private double drawHeader(GuiModule module, Rect area) {
        double u = view.u, x = area.x() + u * 10, width = area.width() - u * 20;
        double y = area.y() + u * 7;
        view.p.text(view.fit(module.label(), width, view.title), x, y, view.title, view.c.text(), CsView.SHADOW);
        y += view.titleTh + u * 2;

        List<String> lines = TextLayout.wrap(module.description(), width, view.p.font(), view.small, CsView.SHADOW);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            view.p.text(lines.get(i), x, y, view.small, view.c.muted(), CsView.SHADOW);
            y += view.smallTh + u;
        }
        y += u * 4;

        Chips chips = new Chips(x, y, x + width);
        if (module.kind() == ModuleKind.NORMAL) {
            String bind = view.binding == module ? "..." : view.session.input().bindName(module.bind());
            Rect chip = chips.next("Bind " + (bind.isEmpty() ? "None" : bind), false);
            view.zones.add(chip, event -> {
                if (event.isRight()) {
                    module.setBind(new Bind(false, -1));
                    return true;
                }
                if (!event.isLeft()) return false;
                captureBind(module, chip, module::setBind);
                return true;
            }, GuiCursor.POINTER);
            hover(chip, "Click, then press a key or mouse button to bind it. Right click clears the bind");

            option(chips.next("Hold", module.isHoldBind()), "Only on while the bind is held",
                    () -> module.setHoldBind(!module.isHoldBind()));
            option(chips.next("List", module.isListed()), "Show in the ArrayList", () -> module.setListed(!module.isListed()));
            option(chips.next("Notify", module.isNotify()), "Show a notification when toggled",
                    () -> module.setNotify(!module.isNotify()));
        }
        if (module.hasPresets()) view.zones.add(chips.next("Presets", false), open(module::presetsEditor), GuiCursor.POINTER);
        if (module.kind() == ModuleKind.EDITOR) view.zones.add(chips.next("Open", true), open(module::editor), GuiCursor.POINTER);
        return chips.bottom() + u * 6;
    }

    /**
     * A chip that flips an option when clicked.
     */
    private void option(Rect chip, String description, Runnable flip) {
        view.zones.add(chip, event -> left(event, flip), GuiCursor.POINTER);
        hover(chip, description);
    }

    /**
     * Lays chips out left to right, starting a new line when one does not fit.
     */
    private final class Chips {
        private final double left, right;
        private double x, y;

        Chips(double left, double y, double right) {
            this.left = left;
            this.right = right;
            this.x = left;
            this.y = y;
        }

        Rect next(String label, boolean on) {
            double u = view.u;
            double width = view.p.textWidth(label, view.small, CsView.SHADOW) + u * 12;
            if (x > left && x + width > right) {
                x = left;
                y += u * 15;
            }
            Rect chip = chip(label, x, y, on);
            x = chip.right() + u * 4;
            return chip;
        }

        double bottom() {
            return y + view.u * 12;
        }
    }

    private Rect chip(String label, double x, double y, boolean on) {
        double u = view.u;
        Rect chip = new Rect(x, y, view.p.textWidth(label, view.small, CsView.SHADOW) + u * 12, u * 12);
        float hover = view.fade("chip." + label, chip.contains(view.frame.mouseX(), view.frame.mouseY()));
        GuiPaint fill = on ? view.c.accent() : view.c.lift(view.c.inset(), 0.1F * hover);
        view.p.roundRect(chip.x(), chip.y(), chip.width(), chip.height(), chip.height() / 2, Corners.ALL, fill);
        view.p.text(label, chip.x() + u * 6, chip.centerY() - view.smallTh / 2, view.small, on ? view.c.onAccent() : view.c.text(), CsView.SHADOW);
        return chip;
    }

    private Predicate<GuiMouseEvent> open(Supplier<GuiEditor> editor) {
        return event -> {
            if (!event.isLeft()) return false;
            view.open(editor.get());
            return true;
        };
    }

    private void captureBind(Object key, Rect area, Consumer<Bind> onBind) {
        view.binding = key;
        view.session.input().captureBind(bind -> {
            view.binding = null;
            onBind.accept(bind);
        }, () -> area);
    }

    private void hover(Rect rect, String description) {
        if (rect.contains(view.frame.mouseX(), view.frame.mouseY())) view.status = description;
    }

    // Settings

    private List<GuiSetting> rows(GuiModule module) {
        List<GuiSetting> rows = new ArrayList<>();
        PageSetting page = module.openPage();
        if (page != null) rows.add(page);
        for (GuiSetting setting : module.settings()) {
            if (setting == page || setting.page() != page || !setting.isShown()) continue;
            rows.add(setting);
        }
        return rows;
    }

    private double height(GuiSetting setting) {
        return (setting instanceof NumberSetting || setting instanceof RangeSetting ? SLIDER_ROW : ROW) * view.u;
    }

    private void drawSettings(GuiModule module) {
        double u = view.u;
        List<GuiSetting> rows = rows(module);
        double content = u * 4;
        for (GuiSetting setting : rows) {
            if (setting == revealing) scroll = content - (list.height() - height(setting)) / 2;
            content += height(setting) + u * 2;
        }
        revealing = null;
        maxScroll = Math.max(0, content - list.height());
        scroll = Math.clamp(scroll, 0, maxScroll);
        double offset = view.smooth("scroll.settings." + module.id(), scroll);

        if (rows.isEmpty()) {
            String empty = "No settings";
            view.p.text(empty, list.centerX() - view.p.textWidth(empty, view.ts, CsView.SHADOW) / 2, list.y() + u * 10, view.ts, view.c.muted(), CsView.SHADOW);
            return;
        }
        view.p.pushClip(list.x(), list.y(), list.width(), list.height());
        try {
            double y = list.y() + u * 4 - offset;
            for (GuiSetting setting : rows) {
                double height = height(setting);
                if (y + height >= list.y() && y <= list.bottom()) {
                    double indent = Math.min(setting.depth(), 4) * u * 8;
                    Rect row = new Rect(list.x() + u * 8 + indent, y, list.width() - u * 16 - indent, height);
                    drawRow(module, setting, row);
                }
                y += height + u * 2;
            }
        } finally {
            view.p.popClip();
        }
    }

    private void drawRow(GuiModule module, GuiSetting setting, Rect row) {
        boolean hovered = view.hovered(row, list);
        float hover = view.fade("setting." + module.id() + "." + setting.id(), hovered);
        if (hover > 0.01F) {
            view.p.roundRect(row.x(), row.y(), row.width(), row.height(), view.radius(4), Corners.ALL,
                    view.c.raised().withAlpha(view.c.raised().alpha() * hover * 0.7F));
        }
        if (view.matchesSearch(setting)) {
            view.p.roundRect(row.x(), row.y(), row.width(), row.height(), view.radius(4), Corners.ALL,
                    view.c.accent().withAlpha(view.c.accent().alpha() * 0.18F));
        }
        if (hovered) view.status = setting.description();
        // Groups open from their own row. Right clicking any other setting that holds others opens
        // them, unless its own row uses right click, so the dots are always there too. Added first,
        // so the row's own areas take clicks before it.
        boolean parent = setting != module.openPage() && !(setting instanceof GroupSetting) && setting.hasVisibleChildren();
        if (parent) {
            view.add(row, list, event -> {
                if (!event.isRight()) return false;
                expand(setting);
                return true;
            }, null);
        }
        Rect content = parent ? new Rect(row.x(), row.y(), row.width() - view.u * DOTS, row.height()) : row;
        switch (setting) {
            case ToggleSetting toggle -> toggleRow(toggle, content);
            case NumberSetting number -> numberRow(number, content);
            case RangeSetting range -> rangeRow(range, content);
            case ModeSetting mode -> modeRow(mode, content);
            case BindSetting bind -> bindRow(bind, content);
            case ColorSetting color -> colorRow(color, content);
            case GroupSetting group -> groupRow(group, content);
            case PageSetting page -> pageRow(page, content);
            case TextSetting text -> textRow(text, content);
            case ActionSetting action -> actionRow(action, content);
            case EditorSetting editor -> editorRow(editor, content);
        }
        if (parent) dots(setting, row);
    }

    private void expand(GuiSetting setting) {
        setting.setExpanded(!setting.isExpanded());
        view.session.input().clickSound();
    }

    /**
     * Three dots at the end of a row, which run together into a bar while its settings are open.
     */
    private void dots(GuiSetting setting, Rect row) {
        double u = view.u, size = u * 2, x = row.right() - u * 5 - size / 2;
        double middle = setting instanceof NumberSetting || setting instanceof RangeSetting ? row.y() + u * 4 + view.th / 2 : row.centerY();
        float open = view.fade("children." + setting.module().id() + "." + setting.id(), setting.isExpanded());
        Rect hit = new Rect(row.right() - u * DOTS, row.y(), u * DOTS, row.height());
        boolean hovered = view.hovered(hit, list);
        GuiPaint paint = hovered || setting.isExpanded() ? view.c.text() : view.c.muted();
        if (open < 1F) {
            GuiPaint dot = paint.withAlpha(paint.alpha() * (1 - open));
            for (int i = -1; i <= 1; i++) view.p.circle(x, middle + i * u * 3.5 - size / 2, size, dot);
        }
        if (open > 0F) {
            view.p.roundRect(x, middle - u * 3.5 - size / 2, size, u * 7 + size, size / 2, Corners.ALL, paint.withAlpha(paint.alpha() * open));
        }
        if (hovered) view.status = setting.isExpanded() ? "Hide the settings under this one" : "Show the settings under this one";
        view.add(hit, list, event -> left(event, () -> setting.setExpanded(!setting.isExpanded())), GuiCursor.POINTER);
    }

    private void label(String text, Rect row, double room) {
        view.p.text(view.fit(text, room, view.ts), row.x() + view.u * 6, row.centerY() - view.th / 2, view.ts, view.c.text(), CsView.SHADOW);
    }

    /**
     * Right-aligned text at the end of a row.
     *
     * @return Its left edge
     */
    private double value(String text, Rect row, GuiPaint paint) {
        double width = view.p.textWidth(text, view.ts, CsView.SHADOW);
        double x = row.right() - view.u * 6 - width;
        view.p.text(text, x, row.centerY() - view.th / 2, view.ts, paint, CsView.SHADOW);
        return x;
    }

    private void click(Rect row, Predicate<GuiMouseEvent> click) {
        view.add(row, list, click, GuiCursor.POINTER);
    }

    private boolean left(GuiMouseEvent event, Runnable action) {
        if (!event.isLeft()) return false;
        action.run();
        view.session.input().clickSound();
        return true;
    }

    private void toggleRow(ToggleSetting toggle, Rect row) {
        double u = view.u;
        label(toggle.label(), row, row.width() - u * 30);
        CsKit.drawSwitch(view.p, row.right() - u * 22, row.centerY() - u * 4.5, u, toggle.get(),
                view.fade("toggle." + toggle.module().id() + "." + toggle.id(), toggle.get()), view.c);
        click(row, event -> left(event, () -> toggle.set(!toggle.get())));
    }

    private Rect track(Rect row) {
        double u = view.u;
        return new Rect(row.x() + u * 6, row.y() + row.height() - u * 8, row.width() - u * 12, u * 3);
    }

    private void sliderLabel(GuiSetting setting, String formatted, Rect row, Runnable typeValue) {
        double u = view.u, textY = row.y() + u * 4;
        double valueWidth = view.p.textWidth(formatted, view.ts, CsView.SHADOW);
        Rect valueRect = new Rect(row.right() - u * 6 - valueWidth, textY - u, valueWidth, view.th + u * 2);
        boolean overValue = view.hovered(valueRect, list);
        view.p.text(view.fit(setting.label(), row.width() - valueWidth - u * 18, view.ts), row.x() + u * 6, textY, view.ts,
                view.c.text(), CsView.SHADOW);
        view.p.text(formatted, valueRect.x(), textY, view.ts, overValue ? view.c.accent() : view.c.muted(), CsView.SHADOW);
        view.add(valueRect, list, event -> left(event, typeValue), GuiCursor.TEXT);
        if (overValue) view.status = "Click to type a value";
    }

    private void numberRow(NumberSetting number, Rect row) {
        double u = view.u;
        sliderLabel(number, number.formatted(), row, () -> view.open(number.entryEditor()));
        Rect track = track(row);
        double fraction = Math.clamp(number.fraction(), 0.0, 1.0);
        view.p.roundRect(track.x(), track.y(), track.width(), track.height(), track.height() / 2, Corners.ALL, view.c.inset());
        view.p.roundRect(track.x(), track.y(), track.width() * fraction, track.height(), track.height() / 2, Corners.ALL, view.c.accent());
        double knob = u * 7;
        view.p.circle(track.x() + track.width() * fraction - knob / 2, track.centerY() - knob / 2, knob, view.c.text());
        Rect grab = new Rect(track.x() - u * 3, track.centerY() - u * 5, track.width() + u * 6, u * 10);
        view.add(grab, list, event -> {
            if (event.isRight()) {
                number.reset();
                return true;
            }
            if (!event.isLeft()) return false;
            slide(track, event.x(), value -> number.slideTo(number.min() + value * (number.max() - number.min())));
            return true;
        }, GuiCursor.RESIZE_HORIZONTAL);
    }

    private void rangeRow(RangeSetting range, Rect row) {
        double u = view.u;
        sliderLabel(range, range.formatted(), row, () -> view.open(range.entryEditor()));
        Rect track = track(row);
        double span = range.max() - range.min();
        double low = span <= 0 ? 0 : Math.clamp((range.value(range.lowIndex()) - range.min()) / span, 0.0, 1.0);
        double high = span <= 0 ? 0 : Math.clamp((range.value(range.highIndex()) - range.min()) / span, 0.0, 1.0);
        view.p.roundRect(track.x(), track.y(), track.width(), track.height(), track.height() / 2, Corners.ALL, view.c.inset());
        view.p.roundRect(track.x() + track.width() * low, track.y(), track.width() * (high - low), track.height(), track.height() / 2,
                Corners.ALL, view.c.accent());
        double knob = u * 7;
        for (double fraction : new double[]{low, high}) {
            view.p.circle(track.x() + track.width() * fraction - knob / 2, track.centerY() - knob / 2, knob, view.c.text());
        }
        Rect grab = new Rect(track.x() - u * 3, track.centerY() - u * 5, track.width() + u * 6, u * 10);
        view.add(grab, list, event -> {
            if (event.isRight()) {
                range.reset();
                return true;
            }
            if (!event.isLeft() || span <= 0) return false;
            int lowEnd = range.lowIndex(), highEnd = range.highIndex();
            double at = Math.clamp((event.x() - track.x()) / track.width(), 0.0, 1.0);
            double lowAt = (range.value(lowEnd) - range.min()) / span, highAt = (range.value(highEnd) - range.min()) / span;
            double grip = (highAt - lowAt) * 0.15;
            int handle = lowAt == highAt || at <= lowAt + grip && at < highAt - grip ? lowEnd : at >= highAt - grip && at > lowAt + grip ? highEnd : -1;
            if (handle >= 0) slide(track, event.x(), value -> range.set(handle, range.round(range.min() + value * span)));
            else moveBoth(range, track, event.x());
            return true;
        }, GuiCursor.RESIZE_HORIZONTAL);
    }

    /**
     * Follows the mouse with both ends of a range, keeping the distance between them at the track's ends.
     */
    private void moveBoth(RangeSetting range, Rect track, double start) {
        int lowEnd = range.lowIndex(), highEnd = range.highIndex();
        double from = range.value(lowEnd), to = range.value(highEnd), span = range.max() - range.min();
        double least = Math.min(0, range.min() - from), most = Math.max(0, range.max() - to);
        view.session.input().beginDrag(new GuiDrag() {
            @Override
            public void moved(double mouseX, double mouseY) {
                double shift = Math.clamp((mouseX - start) / track.width() * span, least, most);
                range.set(lowEnd, range.round(from + shift));
                range.set(highEnd, range.round(to + shift));
            }
        });
    }

    /**
     * Applies the press at {@code x} along {@code track}, then follows the mouse until release.
     */
    private void slide(Rect track, double x, DoubleConsumer fraction) {
        fraction.accept(Math.clamp((x - track.x()) / track.width(), 0.0, 1.0));
        view.session.input().beginDrag(new GuiDrag() {
            @Override
            public void moved(double mouseX, double mouseY) {
                fraction.accept(Math.clamp((mouseX - track.x()) / track.width(), 0.0, 1.0));
            }
        });
    }

    private void modeRow(ModeSetting mode, Rect row) {
        double u = view.u;
        String shown = "< " + mode.selected() + " >";
        double left = row.right() - u * 6 - view.p.textWidth(shown, view.ts, CsView.SHADOW);
        view.glowing(true, () -> value(shown, row, view.c.accent()));
        label(mode.label(), row, left - row.x() - u * 12);
        click(row, event -> {
            int count = mode.choices().size();
            int direction = event.isLeft() ? 1 : event.isRight() ? -1 : 0;
            if (direction == 0 || count == 0) return false;
            mode.select(Math.floorMod(mode.selectedIndex() + direction, count));
            view.session.input().clickSound();
            return true;
        });
    }

    private void bindRow(BindSetting bind, Rect row) {
        double u = view.u;
        String name = view.binding == bind ? "..." : view.session.input().bindName(bind.get());
        String shown = name.isEmpty() ? "None" : name;
        double width = view.p.textWidth(shown, view.small, CsView.SHADOW) + u * 12;
        Rect chip = new Rect(row.right() - u * 4 - width, row.centerY() - u * 6, width, u * 12);
        view.p.roundRect(chip.x(), chip.y(), chip.width(), chip.height(), chip.height() / 2, Corners.ALL, view.c.inset());
        view.p.text(shown, chip.x() + u * 6, chip.centerY() - view.smallTh / 2, view.small, view.c.text(), CsView.SHADOW);
        label(bind.label(), row, chip.x() - row.x() - u * 10);
        click(row, event -> {
            if (event.isRight()) {
                bind.set(new Bind(false, -1));
                return true;
            }
            if (!event.isLeft()) return false;
            captureBind(bind, row, bind::set);
            return true;
        });
    }

    private void colorRow(ColorSetting color, Rect row) {
        double u = view.u, size = u * 10;
        Rect swatch = new Rect(row.right() - u * 6 - size, row.centerY() - size / 2, size, size);
        view.p.roundRect(swatch.x(), swatch.y(), size, size, view.radius(3), Corners.ALL, color.paint());
        view.p.strokeRoundRect(swatch.x(), swatch.y(), size, size, view.radius(3), Corners.ALL, Math.max(0.5, u * 0.5), view.c.line());
        String name = color.colorName();
        double nameWidth = view.p.textWidth(name, view.small, CsView.SHADOW);
        view.p.text(name, swatch.x() - u * 5 - nameWidth, row.centerY() - view.smallTh / 2, view.small, view.c.muted(), CsView.SHADOW);
        label(color.label(), row, swatch.x() - nameWidth - row.x() - u * 16);
        click(row, event -> {
            if (!event.isLeft()) return false;
            view.open(color.editor());
            return true;
        });
    }

    private void groupRow(GroupSetting group, Rect row) {
        double u = view.u;
        GuiIcon icon = group.isExpanded() ? GuiIcon.EXPAND_LESS : GuiIcon.EXPAND_MORE;
        double iconWidth = view.p.iconWidth(icon, view.is);
        view.p.icon(icon, row.right() - u * 6 - iconWidth, row.centerY() - view.ih / 2, view.is, view.c.muted(), CsView.SHADOW);
        label(group.label(), row, row.width() - iconWidth - u * 16);
        click(row, event -> group.hasVisibleChildren() && left(event, () -> group.setExpanded(!group.isExpanded())));
    }

    private void pageRow(PageSetting page, Rect row) {
        double u = view.u;
        boolean open = page.isOpen();
        GuiIcon icon = open ? GuiIcon.EXPAND_LESS : GuiIcon.VIEW_LIST;
        double iconWidth = view.p.iconWidth(icon, view.is);
        view.p.icon(icon, row.right() - u * 6 - iconWidth, row.centerY() - view.ih / 2, view.is, view.c.accent(), CsView.SHADOW);
        label(open ? "< " + page.label() : page.label(), row, row.width() - iconWidth - u * 16);
        click(row, event -> left(event, () -> {
            resetScroll();
            if (page.isOpen()) page.close();
            else page.open();
        }));
    }

    private void textRow(TextSetting text, Rect row) {
        double u = view.u;
        String current = view.fit(text.get(), row.width() * 0.5, view.ts);
        double left = value(current, row, view.c.muted());
        label(text.label(), row, left - row.x() - u * 12);
        click(row, event -> left(event, () -> editText(text)));
    }

    /**
     * Edits a text setting in a form from the public editor API, docked like any other editor.
     */
    private void editText(TextSetting text) {
        TextFieldState field = new TextFieldState(256, TextFieldState.PRINTABLE);
        field.setText(text.get());
        GuiEditor form = GuiEditors.form("example-addon:text", text.label(), "Set " + text.label())
                .field(new FormField("", field, text.label()))
                .button("Cancel", EditorButton.Role.SECONDARY, GuiEditor::close)
                .button("Save", EditorButton.Role.PRIMARY, editor -> {
                    text.set(field.text());
                    editor.close();
                })
                .onSubmit(editor -> {
                    text.set(field.text());
                    editor.close();
                })
                .build();
        view.session.editors().open(form);
    }

    private void actionRow(ActionSetting action, Rect row) {
        double u = view.u;
        Rect button = new Rect(row.x() + u * 2, row.y() + u * 2, row.width() - u * 4, row.height() - u * 4);
        float hover = view.fade("action." + action.module().id() + "." + action.id(), view.hovered(button, list));
        view.p.roundRect(button.x(), button.y(), button.width(), button.height(), view.radius(4), Corners.ALL,
                view.c.lift(view.c.inset(), 0.1F * hover));
        String label = view.fit(action.label(), button.width() - u * 8, view.ts);
        view.p.text(label, button.centerX() - view.p.textWidth(label, view.ts, CsView.SHADOW) / 2, button.centerY() - view.th / 2, view.ts,
                view.c.text(), CsView.SHADOW);
        click(button, event -> left(event, action::run));
    }

    private void editorRow(EditorSetting editor, Rect row) {
        double u = view.u;
        double iconWidth = view.p.iconWidth(editor.icon(), view.is);
        view.p.icon(editor.icon(), row.right() - u * 6 - iconWidth, row.centerY() - view.ih / 2, view.is, view.c.accent(), CsView.SHADOW);
        String summary = view.fit(editor.summary(), row.width() * 0.4, view.small);
        double summaryWidth = view.p.textWidth(summary, view.small, CsView.SHADOW);
        view.p.text(summary, row.right() - u * 10 - iconWidth - summaryWidth, row.centerY() - view.smallTh / 2, view.small,
                view.c.muted(), CsView.SHADOW);
        label(editor.rowLabel(), row, row.width() - iconWidth - summaryWidth - u * 22);
        click(row, event -> {
            if (!event.isLeft()) return false;
            view.open(editor.editor());
            return true;
        });
    }

    // Status line

    /**
     * One line fits the strip under the settings; a longer description rises from it over them.
     */
    private void drawStatus(Rect area) {
        double u = view.u;
        String text = view.status;
        if (text == null || text.isBlank()) return;
        double width = area.width() - u * 20, pitch = view.smallTh + u;
        List<String> lines = new ArrayList<>();
        for (String line : text.strip().split("\n")) lines.addAll(TextLayout.wrap(line, width, view.p.font(), view.small, CsView.SHADOW));
        if (lines.isEmpty()) return;
        double firstY = area.bottom() - STATUS * u * 0.5 - view.smallTh / 2 - (lines.size() - 1) * pitch;
        if (lines.size() > 1) {
            view.p.roundRect(area.x() + u * 4, firstY - u * 3, area.width() - u * 8, area.bottom() - firstY + u, view.radius(4), Corners.ALL, view.c.raised());
        }
        for (int i = 0; i < lines.size(); i++) {
            view.p.text(lines.get(i), area.x() + u * 10, firstY + i * pitch, view.small, lines.size() > 1 ? view.c.text() : view.c.muted(), CsView.SHADOW);
        }
    }
}
