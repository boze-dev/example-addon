package com.example.addon.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.boze.api.gui.editor.GuiEditor;
import dev.boze.api.gui.hud.HudEditor;
import dev.boze.api.gui.hud.HudElement;
import dev.boze.api.gui.input.GuiCharEvent;
import dev.boze.api.gui.input.GuiCursor;
import dev.boze.api.gui.input.GuiDrag;
import dev.boze.api.gui.input.GuiKeyEvent;
import dev.boze.api.gui.input.GuiMouseEvent;
import dev.boze.api.gui.input.GuiScrollEvent;
import dev.boze.api.gui.model.GuiCategory;
import dev.boze.api.gui.model.GuiClientMode;
import dev.boze.api.gui.model.GuiModel;
import dev.boze.api.gui.model.GuiModule;
import dev.boze.api.gui.model.GuiSetting;
import dev.boze.api.gui.model.ModuleKind;
import dev.boze.api.gui.render.Corners;
import dev.boze.api.gui.render.GuiIcon;
import dev.boze.api.gui.render.GuiImage;
import dev.boze.api.gui.render.GuiPaint;
import dev.boze.api.gui.render.GuiPainter;
import dev.boze.api.gui.util.Rect;
import dev.boze.api.gui.util.Spring;
import dev.boze.api.gui.util.TextFieldState;
import dev.boze.api.gui.util.TextLayout;
import dev.boze.api.gui.util.Zones;
import dev.boze.api.gui.view.GuiFrame;
import dev.boze.api.gui.view.GuiSession;
import dev.boze.api.gui.view.GuiView;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * The window: categories on the left, the selected category's modules in the middle and the
 * selected module's settings on the right. Used for the module GUI and, with the HUD's categories,
 * for the HUD editor.
 */
final class CsView implements GuiView {
    private static final double WIDTH = 480, HEIGHT = 320, BAR = 22, SIDEBAR = 96, MODULES = 140;
    private static final double CATEGORY_ROW = 22, MODULE_ROW = 18;
    private static final double OPEN_SECONDS = 0.16;
    private static final int MAX_ID_LENGTH = 128;
    static final boolean SHADOW = true;
    static final int TEXT_GLOW_SPREAD = 2;
    static final float TEXT_GLOW_STRENGTH = 0.4F;
    static final float GLASS = 0.8F;
    // The Boze mark stands in for the logo's b, and the rest is text.
    private static final GuiImage MARK = GuiImage.svg(Identifier.fromNamespaceAndPath("addon-example", "textures/gui/boze.svg"));
    private static final String LOGO = "oze";
    // The mark's width over its height, then its height, the gap after it and where it sits on the
    // baseline, as parts of the line height: the size and place of the font's b.
    private static final double MARK_ASPECT = 160.19 / 281.19, MARK_HEIGHT = 0.6, MARK_GAP = 0.09, MARK_BASELINE = 0.8;
    // One wave of light through the logo every WAVE_SECONDS, letters WAVE_SPACING of a wave apart.
    private static final double WAVE_SECONDS = 1.6, WAVE_SPACING = 0.18;

    final CsTheme theme;
    final GuiSession session;
    private final HudEditor hud;
    private final CsSettings settings = new CsSettings(this);
    private final TextFieldState search = new TextFieldState(32, TextFieldState.PRINTABLE);
    final Spring springs = Spring.settling(0.35);
    final Zones zones = new Zones();

    private boolean searching;
    // Where the window sits in the screen space around it, 0 to 1 across and down, so it keeps its
    // place when the screen size or GUI scale changes. 0.5 centers it.
    private double anchorX = 0.5, anchorY = 0.5;
    private double freeX, freeY;
    private String categoryId, moduleId;
    // This frame's module list: searching reads every setting's name, and the list is asked for several times a frame.
    private List<GuiModule> visibleModules;
    private double categoryScroll, moduleScroll, categoryMax, moduleMax;
    private Rect categoryList, moduleList, editorArea, window;
    private double progress;
    private boolean closing;
    Object binding;
    String status;

    // The frame being drawn
    GuiFrame frame;
    GuiPainter p;
    CsColors c;
    double u, ts, th, small, smallTh, title, titleTh, is, ih;

    CsView(CsTheme theme, GuiSession session, HudEditor hud) {
        this.theme = theme;
        this.session = session;
        this.hud = hud;
    }

    // State, saved per theme. It can come from another player's profile, so every value is checked.

    private String key(String name) {
        return (hud == null ? "gui." : "hud.") + name;
    }

    @Override
    public void opened() {
        JsonObject state = session.state().read();
        anchorX = fraction(state, key("anchorX"));
        anchorY = fraction(state, key("anchorY"));
        categoryId = string(state, key("category"));
        moduleId = string(state, key("module"));
    }

    private void save() {
        JsonObject state = session.state().read();
        state.addProperty(key("anchorX"), anchorX);
        state.addProperty(key("anchorY"), anchorY);
        if (categoryId != null) state.addProperty(key("category"), categoryId);
        if (moduleId != null) state.addProperty(key("module"), moduleId);
        session.state().write(state);
    }

    private static double fraction(JsonObject state, String key) {
        JsonElement value = state.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return 0.5;
        double number = value.getAsDouble();
        return Double.isFinite(number) ? Math.clamp(number, 0.0, 1.0) : 0.5;
    }

    private static String string(JsonObject state, String key) {
        JsonElement value = state.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
        String text = value.getAsString();
        return text.length() <= MAX_ID_LENGTH ? text : null;
    }

    @Override
    public void closed() {
        save();
        if (binding != null) session.input().cancelBindCapture();
        binding = null;
        springs.clear();
        ((CsKit) theme.kit()).forget();
    }

    @Override
    public boolean closeRequested() {
        closing = true;
        searching = false;
        return true;
    }

    @Override
    public float opacity() {
        return eased();
    }

    private float eased() {
        return (float) (progress * progress * (3 - 2 * progress));
    }

    @Override
    public boolean capturesKeyboard() {
        return searching;
    }

    @Override
    public Rect editorArea() {
        return editorArea;
    }

    // Drawing

    @Override
    public void render(GuiFrame frame) {
        this.frame = frame;
        p = frame.painter();
        c = theme.colors();
        double fit = Math.min((frame.width() - 16) / WIDTH, (frame.height() - 16) / HEIGHT);
        u = Math.clamp(Math.min(theme.preferredScale(), fit), 0.3, 3);
        theme.beginFrame(u);
        ts = u * 0.45;
        small = u * 0.38;
        title = u * 0.6;
        is = u * 0.45;
        th = p.textHeight(ts, SHADOW);
        smallTh = p.textHeight(small, SHADOW);
        titleTh = p.textHeight(title, SHADOW);
        ih = p.iconHeight(is, SHADOW);
        status = null;
        visibleModules = null;
        if (binding != null && !session.input().isCapturingBind()) binding = null;

        double step = frame.deltaSeconds() / OPEN_SECONDS;
        progress = closing ? Math.max(0, progress - step) : Math.min(1, progress + step);
        if (closing && progress <= 0) {
            session.finishClose();
            return;
        }

        double w = WIDTH * u, h = HEIGHT * u;
        freeX = Math.max(0, frame.width() - w);
        freeY = Math.max(0, frame.height() - h);
        window = new Rect(anchorX * freeX, anchorY * freeY, w, h);

        zones.begin();
        float eased = eased();
        // A layer fades the window as one picture, so its overlapping pieces do not show through.
        boolean layered = eased < 1F;
        if (layered) p.pushLayer(eased);
        try {
            if (hud == null && c.tint().alpha() > 0F) p.rect(0, 0, frame.width(), frame.height(), c.tint());
            if (hud != null) drawHudElements();
            double scale = 0.94 + 0.06 * eased;
            p.pushTransform(window.centerX() * (1 - scale), window.centerY() * (1 - scale), scale);
            boolean glowAll = theme.textGlow() == CsTheme.TextGlow.All;
            if (glowAll) p.pushTextGlow(TEXT_GLOW_SPREAD, TEXT_GLOW_STRENGTH);
            try {
                drawWindow();
            } finally {
                if (glowAll) p.popTextGlow();
                p.popTransform();
            }
        } finally {
            if (layered) p.popLayer();
        }
        zones.end();
        GuiCursor cursor = zones.cursorAt(frame.mouseX(), frame.mouseY());
        if (cursor != null) session.input().setCursor(cursor);
    }

    private void drawWindow() {
        double r = radius(7);
        Rect sidebar = new Rect(window.x(), window.y(), SIDEBAR * u, window.height());
        Rect bar = new Rect(sidebar.right(), window.y(), window.width() - sidebar.width(), BAR * u);
        Rect modules = new Rect(bar.x(), bar.bottom(), MODULES * u, window.bottom() - bar.bottom());
        Rect settingsArea = new Rect(modules.right(), bar.bottom(), window.right() - modules.right(), modules.height());
        editorArea = new Rect(settingsArea.x(), settingsArea.y() + u * 18, settingsArea.width(), settingsArea.height() - u * 18);

        // With blur on, the fills are partly see-through so the blur shows; each part is filled once
        // so the left column is not darker for being drawn over the rest.
        float glass = theme.blur() ? GLASS : 1F;
        if (theme.blur()) p.backdropBlur(window.x(), window.y(), window.width(), window.height(), r, Corners.ALL, theme.blurStrength());
        if (theme.glow()) p.shadow(window.x(), window.y(), window.width(), window.height(), r, theme.glowSize() * 2 * u, c.accent());
        if (theme.outline()) {
            // Just outside the window, so editors filling the settings column do not cover it.
            double line = u;
            p.strokeRoundRect(window.x() - line, window.y() - line, window.width() + line * 2, window.height() + line * 2, r + line,
                    Corners.ALL, line, c.accent());
        }
        Rect rest = new Rect(sidebar.right(), window.y(), window.width() - sidebar.width(), window.height());
        p.roundRect(sidebar.x(), sidebar.y(), sidebar.width(), sidebar.height(), r, Corners.LEFT,
                c.sidebar().withAlpha(c.sidebar().alpha() * glass));
        p.roundRect(rest.x(), rest.y(), rest.width(), rest.height(), r, Corners.RIGHT, c.background().withAlpha(c.background().alpha() * glass));
        // Clicks anywhere on the window stay on it.
        zones.add(window, event -> true, null);

        drawSidebar(sidebar);
        drawBar(bar);
        p.rect(modules.right(), modules.y(), Math.max(1, u * 0.5), modules.height(), c.line());
        drawModules(modules);
        settings.draw(settingsArea);
    }

    double radius(double units) {
        return u * units;
    }

    // Sidebar

    private List<GuiCategory> categories() {
        GuiModel model = session.model();
        List<GuiCategory> shown = new ArrayList<>();
        for (GuiCategory category : hud == null ? model.categories() : model.hudCategories()) {
            if (category.isShown()) shown.add(category);
        }
        return shown;
    }

    private GuiCategory selectedCategory(List<GuiCategory> categories) {
        for (GuiCategory category : categories) {
            if (category.id().equals(categoryId)) return category;
        }
        return categories.isEmpty() ? null : categories.getFirst();
    }

    /**
     * The logo, centered over the column and as large as the bar and the column allow. With Logo
     * Animation on, a wave of light rolls through it, each letter brightening and rising as it passes.
     */
    private void drawLogo(Rect sidebar) {
        // Line height and the mark's size, gap and baseline are all in proportion to the scale.
        double line = p.textHeight(1, false);
        double unitWidth = line * (MARK_HEIGHT * MARK_ASPECT + MARK_GAP) + p.textWidth(LOGO, 1, SHADOW);
        double scale = Math.min(u * 0.95, Math.min((BAR * u - u * 4) / p.textHeight(1, SHADOW), (sidebar.width() - u * 20) / unitWidth));
        double markHeight = line * scale * MARK_HEIGHT, markWidth = markHeight * MARK_ASPECT;
        double x = sidebar.centerX() - unitWidth * scale / 2;
        double y = sidebar.y() + (BAR * u - p.textHeight(scale, SHADOW)) / 2;
        double markY = y + line * scale * MARK_BASELINE - markHeight;
        double textX = x + markWidth + line * scale * MARK_GAP;
        // Eases in and out as the option is switched, instead of the letters jumping.
        float motion = fade("logo", theme.logoAnimation());
        glowing(true, () -> {
            if (motion <= 0F) {
                p.image(MARK, x, markY, markWidth, markHeight, c.accent(), SHADOW);
                p.text(LOGO, textX, y, scale, c.accent(), SHADOW);
                return;
            }
            double time = frame.nanoTime() / 1e9;
            // A sine wave rolling left to right, the mark first and each letter a little behind.
            for (int i = 0; i <= LOGO.length(); i++) {
                double wave = 0.5 + 0.5 * Math.sin((time / WAVE_SECONDS - i * WAVE_SPACING) * Math.PI * 2);
                float lit = (float) (wave * wave) * motion;
                double lift = lit * u * 1.5;
                GuiPaint paint = c.lift(c.accent(), 0.5F * lit);
                if (i == 0) {
                    p.image(MARK, x, markY - lift, markWidth, markHeight, paint, SHADOW);
                } else {
                    double letterX = textX + p.textWidth(LOGO.substring(0, i - 1), scale, SHADOW);
                    p.text(LOGO.substring(i - 1, i), letterX, y - lift, scale, paint, SHADOW);
                }
            }
        });
    }

    private void drawSidebar(Rect sidebar) {
        drawLogo(sidebar);
        zones.add(new Rect(sidebar.x(), sidebar.y(), sidebar.width(), BAR * u), this::dragWindow, GuiCursor.RESIZE_ALL);

        double footer = hud == null ? u * 30 : u * 6;
        categoryList = new Rect(sidebar.x(), sidebar.y() + BAR * u, sidebar.width(), sidebar.height() - BAR * u - footer);
        List<GuiCategory> categories = categories();
        GuiCategory selected = selectedCategory(categories);
        double rowHeight = CATEGORY_ROW * u;
        categoryMax = Math.max(0, categories.size() * rowHeight - categoryList.height());
        categoryScroll = Math.clamp(categoryScroll, 0, categoryMax);
        double offset = smooth("scroll.categories", categoryScroll);

        p.pushClip(categoryList.x(), categoryList.y(), categoryList.width(), categoryList.height());
        try {
            int selectedIndex = categories.indexOf(selected);
            if (selectedIndex >= 0) {
                // The row index is what eases, so moving or resizing the window does not animate it.
                double indicatorY = categoryList.y() + smooth("indicator", selectedIndex) * rowHeight - offset;
                p.roundRect(sidebar.x() + u * 3, indicatorY + u * 6, u * 2, rowHeight - u * 12, u, Corners.ALL, c.accent());
            }
            for (int i = 0; i < categories.size(); i++) {
                GuiCategory category = categories.get(i);
                Rect row = new Rect(sidebar.x() + u * 7, categoryList.y() + i * rowHeight - offset + u * 2, sidebar.width() - u * 13,
                        rowHeight - u * 4);
                boolean active = category == selected && !hasSearch();
                boolean hovered = hovered(row, categoryList);
                float highlight = fade("category." + category.id(), active || hovered);
                if (highlight > 0.01F) {
                    p.roundRect(row.x(), row.y(), row.width(), row.height(), radius(4), Corners.ALL,
                            c.raised().withAlpha(c.raised().alpha() * highlight));
                }
                GuiIcon icon = category.icon() != null ? category.icon() : GuiIcon.VIEW_LIST;
                GuiPaint paint = active ? c.accent() : hovered ? c.text() : c.muted();
                glowing(active, () -> p.icon(icon, row.x() + u * 6, row.centerY() - ih / 2, is, paint, SHADOW));
                double textX = row.x() + u * 8 + p.iconWidth(icon, is);
                p.text(fit(category.label(), row.right() - u * 4 - textX, ts), textX, row.centerY() - th / 2, ts,
                        active ? c.text() : c.muted(), SHADOW);
                add(row, categoryList, event -> {
                    if (!event.isLeft()) return false;
                    selectCategory(category);
                    return true;
                }, GuiCursor.POINTER);
            }
        } finally {
            p.popClip();
        }

        if (hud == null) drawClientMode(new Rect(sidebar.x() + u * 8, sidebar.bottom() - u * 24, sidebar.width() - u * 16, u * 16));
    }

    private void drawClientMode(Rect chip) {
        GuiModel model = session.model();
        boolean hovered = chip.contains(frame.mouseX(), frame.mouseY());
        p.roundRect(chip.x(), chip.y(), chip.width(), chip.height(), chip.height() / 2, Corners.ALL,
                c.lift(c.inset(), 0.08F * fade("mode", hovered)));
        p.text("Mode", chip.x() + u * 7, chip.centerY() - smallTh / 2, small, c.muted(), SHADOW);
        String mode = model.clientMode().label();
        glowing(true, () -> p.text(mode, chip.right() - u * 7 - p.textWidth(mode, small, SHADOW), chip.centerY() - smallTh / 2, small,
                c.accent(), SHADOW));
        zones.add(chip, event -> {
            GuiClientMode[] modes = GuiClientMode.values();
            int direction = event.isRight() ? -1 : event.isLeft() ? 1 : 0;
            if (direction == 0) return false;
            model.setClientMode(modes[Math.floorMod(model.clientMode().ordinal() + direction, modes.length)]);
            session.input().clickSound();
            return true;
        }, GuiCursor.POINTER);
        if (hovered) status = "Client mode: left click for the next, right click for the previous";
    }

    private void selectCategory(GuiCategory category) {
        categoryId = category.id();
        moduleId = null;
        moduleScroll = 0;
        search.clear();
        searching = false;
        settings.resetScroll();
        session.editors().closeAll();
        session.input().clickSound();
        save();
    }

    // Top bar

    private void drawBar(Rect bar) {
        zones.add(bar, this::dragWindow, GuiCursor.RESIZE_ALL);
        GuiCategory category = selectedCategory(categories());
        String heading = hasSearch() ? "Search" : category == null ? "" : category.label();
        p.text(heading, bar.x() + u * 10, bar.centerY() - titleTh / 2, title, c.text(), SHADOW);
        if (hud != null) {
            String hint = "Drag elements on screen to move them";
            p.text(hint, bar.x() + u * 16 + p.textWidth(heading, title, SHADOW), bar.centerY() - smallTh / 2, small, c.muted(), SHADOW);
        }

        double size = u * 14;
        Rect close = new Rect(bar.right() - size - u * 5, bar.centerY() - size / 2, size, size);
        float hover = fade("close", close.contains(frame.mouseX(), frame.mouseY()));
        if (hover > 0.01F) p.circle(close.x(), close.y(), size, c.raised().withAlpha(c.raised().alpha() * hover));
        p.text("x", close.centerX() - p.textWidth("x", ts, SHADOW) / 2, close.centerY() - th / 2, ts, hover > 0.5F ? c.text() : c.muted(), SHADOW);
        zones.add(close, event -> {
            if (!event.isLeft()) return false;
            session.close();
            return true;
        }, GuiCursor.POINTER);
        // The HUD editor needs a world to edit.
        if (hud != null || Minecraft.getInstance().player != null) drawSurfaceSwitch(bar, close);

        p.rect(bar.x(), bar.bottom() - Math.max(1, u * 0.75), bar.width(), Math.max(1, u * 0.75),
                GuiPaint.horizontal(c.accent(), c.accent().withAlpha(0F)));
    }

    /**
     * A chip beside the close button that goes between the module GUI and the HUD editor.
     */
    private void drawSurfaceSwitch(Rect bar, Rect close) {
        String label = hud == null ? "HUD" : "Modules";
        double width = p.textWidth(label, small, SHADOW) + u * 12;
        Rect chip = new Rect(close.x() - u * 5 - width, bar.centerY() - u * 6, width, u * 12);
        boolean hovered = chip.contains(frame.mouseX(), frame.mouseY());
        p.roundRect(chip.x(), chip.y(), chip.width(), chip.height(), chip.height() / 2, Corners.ALL,
                c.lift(c.inset(), 0.1F * fade("surface", hovered)));
        p.text(label, chip.x() + u * 6, chip.centerY() - smallTh / 2, small, c.text(), SHADOW);
        zones.add(chip, event -> {
            if (!event.isLeft()) return false;
            session.editors().closeAll();
            if (hud == null) session.showHudEditor();
            else session.showClickGui();
            session.input().clickSound();
            return true;
        }, GuiCursor.POINTER);
        if (hovered) status = hud == null ? "Open the HUD editor" : "Back to the modules";
    }

    private boolean dragWindow(GuiMouseEvent event) {
        if (!event.isLeft()) return false;
        double grabX = event.x() - window.x(), grabY = event.y() - window.y();
        session.input().beginDrag(new GuiDrag() {
            @Override
            public void moved(double x, double y) {
                if (freeX > 0) anchorX = Math.clamp((x - grabX) / freeX, 0.0, 1.0);
                if (freeY > 0) anchorY = Math.clamp((y - grabY) / freeY, 0.0, 1.0);
            }

            @Override
            public void released() {
                save();
            }
        });
        return true;
    }

    // Modules

    private boolean hasSearch() {
        return !search.isEmpty();
    }

    private String searchQuery() {
        return search.text().trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @return Whether a search is on and has found this setting by its name
     */
    boolean matchesSearch(GuiSetting setting) {
        return hasSearch() && setting.label().toLowerCase(Locale.ROOT).contains(searchQuery());
    }

    private boolean matchesSearch(GuiModule module) {
        String query = searchQuery();
        if (module.label().toLowerCase(Locale.ROOT).contains(query) || module.id().toLowerCase(Locale.ROOT).contains(query)) return true;
        for (GuiSetting setting : module.settings()) {
            if (setting.isVisible() && matchesSearch(setting)) return true;
        }
        return false;
    }

    private List<GuiModule> visibleModules() {
        if (visibleModules == null) visibleModules = findVisibleModules();
        return visibleModules;
    }

    private List<GuiModule> findVisibleModules() {
        List<GuiModule> modules = new ArrayList<>();
        if (hasSearch()) {
            for (GuiCategory category : categories()) {
                for (GuiModule module : category.modules()) {
                    if (module.isShown() && matchesSearch(module)) modules.add(module);
                }
            }
            return modules;
        }
        GuiCategory category = selectedCategory(categories());
        if (category == null) return modules;
        for (GuiModule module : category.modules()) {
            if (module.isShown()) modules.add(module);
        }
        return modules;
    }

    GuiModule selectedModule() {
        List<GuiModule> modules = visibleModules();
        for (GuiModule module : modules) {
            if (module.id().equals(moduleId)) return module;
        }
        if (moduleId == null && !modules.isEmpty()) {
            moduleId = modules.getFirst().id();
            return modules.getFirst();
        }
        return null;
    }

    private void drawModules(Rect area) {
        Rect field = new Rect(area.x() + u * 6, area.y() + u * 6, area.width() - u * 12, u * 16);
        drawSearch(field);

        moduleList = new Rect(area.x(), field.bottom() + u * 4, area.width(), area.bottom() - field.bottom() - u * 8);
        List<GuiModule> modules = visibleModules();
        GuiModule selected = selectedModule();
        double rowHeight = MODULE_ROW * u;
        moduleMax = Math.max(0, modules.size() * rowHeight - moduleList.height());
        moduleScroll = Math.clamp(moduleScroll, 0, moduleMax);
        double offset = smooth("scroll.modules", moduleScroll);

        if (modules.isEmpty()) {
            String empty = hasSearch() ? "No matches" : "No modules";
            p.text(empty, area.centerX() - p.textWidth(empty, ts, SHADOW) / 2, moduleList.y() + u * 10, ts, c.muted(), SHADOW);
            return;
        }
        p.pushClip(moduleList.x(), moduleList.y(), moduleList.width(), moduleList.height());
        try {
            for (int i = 0; i < modules.size(); i++) {
                double rowY = moduleList.y() + i * rowHeight - offset;
                if (rowY + rowHeight < moduleList.y() || rowY > moduleList.bottom()) continue;
                drawModule(modules.get(i), modules.get(i) == selected, new Rect(area.x() + u * 6, rowY + u, area.width() - u * 12,
                        rowHeight - u * 2));
            }
        } finally {
            p.popClip();
        }
    }

    private void drawModule(GuiModule module, boolean selected, Rect row) {
        boolean hovered = hovered(row, moduleList);
        float highlight = fade("module." + module.id(), selected || hovered);
        if (highlight > 0.01F) {
            GuiPaint fill = selected ? c.raised() : c.raised().withAlpha(c.raised().alpha() * highlight * 0.6F);
            p.roundRect(row.x(), row.y(), row.width(), row.height(), radius(4), Corners.ALL, fill);
        }
        if (selected) p.roundRect(row.x(), row.y() + u * 4, u * 2, row.height() - u * 8, u, Corners.ALL, c.accent());

        boolean normal = module.kind() == ModuleKind.NORMAL;
        GuiPaint paint = module.isEnabled() ? c.accent() : selected || hovered ? c.text() : c.muted();
        double textRoom = row.width() - u * (normal || module.kind() == ModuleKind.EDITOR ? 30 : 12);
        String label = fit(module.label(), textRoom, ts);
        glowing(module.isEnabled(), () -> p.text(label, row.x() + u * 7, row.centerY() - th / 2, ts, paint, SHADOW));

        // The row goes in first: click areas added later sit on top of it, so the switch gets its own clicks.
        add(row, moduleList, event -> {
            if (event.isLeft()) {
                selectModule(module);
                if (module.kind() == ModuleKind.EDITOR) open(module.editor());
                return true;
            }
            if (event.isRight() && normal) {
                module.toggle();
                session.input().clickSound();
                return true;
            }
            return false;
        }, GuiCursor.POINTER);

        if (normal) {
            Rect toggle = new Rect(row.right() - u * 21, row.centerY() - u * 4.5, u * 16, u * 9);
            CsKit.drawSwitch(p, toggle.x(), toggle.y(), u, module.isEnabled(), fade("switch." + module.id(), module.isEnabled()), c);
            add(toggle, moduleList, event -> {
                if (!event.isLeft()) return false;
                module.toggle();
                session.input().clickSound();
                return true;
            }, GuiCursor.POINTER);
        } else if (module.kind() == ModuleKind.EDITOR) {
            p.icon(GuiIcon.POPUP, row.right() - u * 6 - p.iconWidth(GuiIcon.POPUP, is), row.centerY() - ih / 2, is, c.muted(), SHADOW);
        }
        if (hovered) status = module.description();
    }

    private void selectModule(GuiModule module) {
        if (!module.id().equals(moduleId)) settings.resetScroll();
        moduleId = module.id();
        if (hasSearch()) settings.reveal(module);
        session.editors().closeAll();
        session.input().clickSound();
        save();
    }

    private void drawSearch(Rect field) {
        p.roundRect(field.x(), field.y(), field.width(), field.height(), radius(4), Corners.ALL, c.inset());
        float focus = fade("search", searching);
        if (focus > 0.01F) {
            p.strokeRoundRect(field.x(), field.y(), field.width(), field.height(), radius(4), Corners.ALL, u,
                    c.accent().withAlpha(c.accent().alpha() * focus));
        }
        double textX = field.x() + u * 6, textY = field.centerY() - th / 2;
        p.pushClip(field.x() + u * 2, field.y(), field.width() - u * 4, field.height());
        try {
            if (search.isEmpty()) p.text(searching ? "" : "Search  (Ctrl+F)", textX, textY, ts, c.muted(), SHADOW);
            double before = p.textWidth(search.beforeCaret(), ts, SHADOW);
            double shift = Math.max(0, before + u * 2 - (field.width() - u * 12));
            p.text(search.text(), textX - shift, textY, ts, c.text(), SHADOW);
            if (searching && System.currentTimeMillis() % 1000 < 500) p.rect(textX - shift + before, textY, Math.max(1, u * 0.6), th, c.accent());
        } finally {
            p.popClip();
        }
        zones.add(field, event -> {
            if (!event.isLeft()) return false;
            startSearch();
            return true;
        }, GuiCursor.TEXT);
    }

    private void startSearch() {
        searching = true;
        session.editors().closeAll();
    }

    // HUD editor

    private void drawHudElements() {
        boolean dragging = false;
        for (HudElement element : hud.elements()) {
            if (!element.isEnabled()) continue;
            dragging |= element.isDragging();
            GuiPaint paint = element.isDragging() ? c.accent() : c.accent().withAlpha(0.45F);
            p.strokeRect(element.x() - 1, element.y() - 1, element.width() + 2, element.height() + 2, 0.5, paint);
        }
        if (dragging) {
            GuiPaint guide = c.muted().withAlpha(0.4F);
            p.line(frame.width() / 2, hud.margin(), frame.width() / 2, frame.height() - hud.margin(), guide);
            p.line(hud.margin(), frame.height() / 2, frame.width() - hud.margin(), frame.height() / 2, guide);
        }
    }

    // Shared helpers for the settings column

    /**
     * Draws highlighted text, glowing when the theme's Text Glow is Accent.
     */
    void glowing(boolean highlighted, Runnable draw) {
        boolean glow = highlighted && theme.textGlow() == CsTheme.TextGlow.Accent;
        if (glow) p.pushTextGlow(TEXT_GLOW_SPREAD, TEXT_GLOW_STRENGTH);
        try {
            draw.run();
        } finally {
            if (glow) p.popTextGlow();
        }
    }

    void open(GuiEditor editor) {
        if (editor == null) return;
        session.editors().open(editor);
        session.input().clickSound();
    }

    String fit(String text, double maxWidth, double scale) {
        return TextLayout.fit(text == null ? "" : text, Math.max(0, maxWidth), p.font(), scale, SHADOW);
    }

    boolean hovered(Rect rect, Rect clip) {
        double x = frame.mouseX(), y = frame.mouseY();
        return rect.contains(x, y) && (clip == null || clip.contains(x, y));
    }

    /**
     * Adds a click area, cut to the visible part of a scrolled list.
     */
    void add(Rect rect, Rect clip, Predicate<GuiMouseEvent> click, GuiCursor cursor) {
        double x0 = Math.max(rect.x(), clip.x()), y0 = Math.max(rect.y(), clip.y());
        double x1 = Math.min(rect.right(), clip.right()), y1 = Math.min(rect.bottom(), clip.bottom());
        if (x1 > x0 && y1 > y0) zones.add(new Rect(x0, y0, x1 - x0, y1 - y0), click, cursor);
    }

    /**
     * @return 0 to 1, easing toward 1 while {@code on}
     */
    float fade(Object key, boolean on) {
        return springs.update(key, on);
    }

    double smooth(Object key, double target) {
        return springs.update(key, (float) target);
    }

    // Input

    @Override
    public boolean mouseClicked(GuiMouseEvent event) {
        if (closing) return true;
        boolean inSearch = false;
        if (searching) {
            searching = false;
            inSearch = true;
        }
        if (zones.click(event)) return true;
        if (window != null && window.contains(event.x(), event.y())) return true;
        if (inSearch) return true;
        return hud != null && hud.mouseClicked(event);
    }

    @Override
    public boolean mouseReleased(GuiMouseEvent event) {
        if (hud != null) hud.mouseReleased(event);
        return false;
    }

    @Override
    public boolean mouseScrolled(GuiScrollEvent event) {
        double amount = event.vertical() * u * 24;
        if (categoryList != null && categoryList.contains(event.x(), event.y())) {
            categoryScroll = Math.clamp(categoryScroll - amount, 0, categoryMax);
            return true;
        }
        if (moduleList != null && moduleList.contains(event.x(), event.y())) {
            moduleScroll = Math.clamp(moduleScroll - amount, 0, moduleMax);
            return true;
        }
        if (settings.scroll(event.x(), event.y(), amount)) return true;
        return window != null && window.contains(event.x(), event.y());
    }

    @Override
    public boolean keyPressed(GuiKeyEvent event) {
        if (searching) {
            if (event.isEscape() || event.isEnter()) {
                searching = false;
                return true;
            }
            search.keyPressed(event, session.input()::clipboard);
            moduleScroll = 0;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_F && event.hasActionModifier()) {
            startSearch();
            return true;
        }
        if (event.isPaste() && session.editors().isEmpty()) {
            session.pasteConfigCodes();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(GuiCharEvent event) {
        if (!searching) return false;
        if (search.insert(event.codepoint())) moduleScroll = 0;
        return true;
    }
}
