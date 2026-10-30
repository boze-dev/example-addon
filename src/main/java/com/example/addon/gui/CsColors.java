package com.example.addon.gui;

import dev.boze.api.gui.render.GuiPaint;
import dev.boze.api.gui.util.Argb;
import dev.boze.api.option.ColorOption;

/**
 * The colors one frame is drawn with, worked out from the theme's Accent, Background and Text.
 *
 * @param sidebar The darker column on the left
 * @param raised  Rows and buttons
 * @param inset   Fields and slider tracks
 */
record CsColors(GuiPaint background, GuiPaint sidebar, GuiPaint raised, GuiPaint inset, GuiPaint accent, GuiPaint onAccent,
                GuiPaint text, GuiPaint muted, GuiPaint line, GuiPaint tint) {
    static CsColors of(ColorOption.Value accent, ColorOption.Value background, ColorOption.Value text) {
        int back = GuiPaint.of(background).argb(), fore = GuiPaint.of(text).argb();
        int opaque = Argb.withAlpha(back, 255);
        int alpha = Argb.alpha(back);
        GuiPaint sidebar = GuiPaint.argb(Argb.withAlpha(Argb.blend(opaque, 0xFF000000, 0.35F), alpha));
        GuiPaint raised = GuiPaint.argb(Argb.withAlpha(Argb.blend(opaque, fore, 0.08F), alpha));
        GuiPaint inset = GuiPaint.argb(Argb.withAlpha(Argb.blend(opaque, 0xFF000000, 0.45F), alpha));
        GuiPaint line = GuiPaint.argb(Argb.withAlpha(Argb.blend(opaque, fore, 0.12F), alpha));
        return new CsColors(GuiPaint.of(background), sidebar, raised, inset, GuiPaint.of(accent), GuiPaint.argb(0xFFFFFFFF),
                GuiPaint.of(text), GuiPaint.argb(Argb.fade(fore, 0.55F)), line, GuiPaint.argb(0x66000000));
    }

    /**
     * @return {@code paint} mixed toward the text color, for hovered pieces
     */
    GuiPaint lift(GuiPaint paint, float amount) {
        return mix(paint, text.argb(), amount);
    }

    /**
     * @return {@code paint} mixed toward black, for pressed pieces
     */
    GuiPaint press(GuiPaint paint, float amount) {
        return mix(paint, 0xFF000000, amount);
    }

    private static GuiPaint mix(GuiPaint paint, int toward, float amount) {
        if (amount <= 0F) return paint;
        int argb = paint.argb();
        return GuiPaint.argb(Argb.withAlpha(Argb.blend(argb, toward, amount), Argb.alpha(argb)));
    }
}
