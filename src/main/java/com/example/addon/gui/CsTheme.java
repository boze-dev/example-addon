package com.example.addon.gui;

import dev.boze.api.gui.editor.EditorLayout;
import dev.boze.api.gui.editor.EditorView;
import dev.boze.api.gui.editor.GuiEditor;
import dev.boze.api.gui.editor.ListEditor;
import dev.boze.api.gui.hud.HudEditor;
import dev.boze.api.gui.kit.WidgetKit;
import dev.boze.api.gui.theme.GuiTheme;
import dev.boze.api.gui.theme.ThemeOptions;
import dev.boze.api.gui.view.GuiSession;
import dev.boze.api.gui.view.GuiView;
import dev.boze.api.option.ColorOption;
import dev.boze.api.option.ModeOption;
import dev.boze.api.option.SliderOption;
import dev.boze.api.option.ToggleOption;
import dev.boze.api.render.ColorMaker;

/**
 * A single window with a column of categories, a column of modules and a column of the selected
 * module's settings. Editors open in place of the settings.
 */
public final class CsTheme implements GuiTheme {
    public enum TextGlow {
        Off, Accent, All
    }

    private final CsKit kit = new CsKit(this);

    private ColorOption accent, background, text;
    private SliderOption scale;
    private ToggleOption glow;
    private SliderOption glowSize;
    private ToggleOption outline;
    private ModeOption<TextGlow> textGlow;
    private ToggleOption logoAnimation;
    private ToggleOption blur;
    private SliderOption blurStrength;

    // The unit of the frame being drawn, which the kit sizes pieces with.
    private double unit = 1;

    @Override
    public String id() {
        return "example-addon:cs";
    }

    @Override
    public String name() {
        return "CS";
    }

    @Override
    public String description() {
        return "Categories, modules and settings side by side in one window";
    }

    @Override
    public void declareOptions(ThemeOptions options) {
        accent = options.add(new ColorOption(options.owner(), "Accent", "Highlight color",
                ColorMaker.staticColor(124, 92, 255), 1F));
        background = options.add(new ColorOption(options.owner(), "Background", "Window color",
                ColorMaker.staticColor(16, 16, 20), 0.96F));
        text = options.add(new ColorOption(options.owner(), "Text", "Text color",
                ColorMaker.staticColor(236, 236, 242), 1F));
        scale = options.add(new SliderOption(options.owner(), "Scale", "Size of the window", 1, 0.6, 1.6, 0.05));
        glow = options.add(new ToggleOption(options.owner(), "Glow", "Glow in the accent color around the window", true));
        glowSize = options.add(new SliderOption(options.owner(), "GlowSize", "How far the glow around the window reaches", 5, 1, 10, 1,
                () -> glow.getValue()));
        outline = options.add(new ToggleOption(options.owner(), "Outline", "Draw a thin line in the accent color around the window", true));
        textGlow = options.add(new ModeOption<>(options.owner(), "TextGlow", """
                Which text glows
                 - Accent: Highlighted text such as enabled modules
                 - All: Every piece of text in the window
                """, TextGlow.Accent));
        logoAnimation = options.add(new ToggleOption(options.owner(), "LogoAnimation",
                "Roll a wave of light through the logo, lifting each letter as it passes", true));
        blur = options.add(new ToggleOption(options.owner(), "Blur", "Blur what is behind the window", true));
        blurStrength = options.add(new SliderOption(options.owner(), "BlurStrength", "How blurry what is behind the window is", 4, 1, 8, 1,
                () -> blur.getValue()));
    }

    CsColors colors() {
        return CsColors.of(accent.getValue(), background.getValue(), text.getValue());
    }

    double preferredScale() {
        double value = scale.getValue();
        return Double.isFinite(value) ? Math.clamp(value, 0.6, 1.6) : 1;
    }

    boolean glow() {
        return glow.getValue();
    }

    double glowSize() {
        double value = glowSize.getValue();
        return Double.isFinite(value) ? Math.clamp(value, 1, 10) : 5;
    }

    boolean outline() {
        return outline.getValue();
    }

    TextGlow textGlow() {
        return textGlow.getValue();
    }

    boolean logoAnimation() {
        return logoAnimation.getValue();
    }

    boolean blur() {
        return blur.getValue();
    }

    int blurStrength() {
        double value = blurStrength.getValue();
        return Double.isFinite(value) ? Math.clamp(Math.round(value), 1, 8) : 4;
    }

    void beginFrame(double unit) {
        this.unit = unit;
    }

    double unit() {
        return unit;
    }

    @Override
    public GuiView createClickGui(GuiSession session) {
        return new CsView(this, session, null);
    }

    @Override
    public GuiView createHudEditor(GuiSession session, HudEditor hudEditor) {
        return new CsView(this, session, hudEditor);
    }

    @Override
    public WidgetKit kit() {
        return kit;
    }

    /**
     * The account manager gets a layout of its own; every other editor is drawn with the kit.
     */
    @Override
    public EditorView createEditorView(GuiEditor editor) {
        return editor instanceof ListEditor list && CsAccounts.ID.equals(editor.id()) ? new CsAccounts(this, list) : null;
    }

    /**
     * Editors fill the settings column; on screens without the window, such as the account
     * manager, they float in the middle at their own size.
     */
    @Override
    public EditorLayout editorLayout() {
        EditorLayout docked = EditorLayout.stack(), floating = EditorLayout.row(() -> 12 * unit);
        return (frame, views, area) -> {
            boolean screen = area.width() >= frame.width() && area.height() >= frame.height();
            return (screen ? floating : docked).place(frame, views, area);
        };
    }

    @Override
    public double editorTransition() {
        return 0.16;
    }
}
