package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventHudRender;
import dev.boze.api.event.EventWorldRender;
import dev.boze.api.option.ColorOption;
import dev.boze.api.option.ModeOption;
import dev.boze.api.option.RangeSliderOption;
import dev.boze.api.option.SliderOption;
import dev.boze.api.option.ToggleOption;
import dev.boze.api.render.Billboard;
import dev.boze.api.render.BloomMode;
import dev.boze.api.render.ColorMaker;
import dev.boze.api.render.TextDrawer;
import dev.boze.api.render.TextType;
import dev.boze.api.render.WorldDrawer;
import dev.boze.api.utility.EntityHelper;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Example module pairing a range slider with the EntityHelper API: draws a box around every entity
 * whose distance from you falls inside the selected low-high band.
 */
public class ExampleRange extends AddonModule {
    public static final ExampleRange INSTANCE = new ExampleRange();

    public final RangeSliderOption distance = new RangeSliderOption(this, "Distance",
            "Only highlight entities this many blocks away", 4.0, 16.0, 0.0, 64.0, 1.0);
    public final ColorOption color = new ColorOption(this, "Color", "Box color",
            ColorMaker.staticColor(0, 255, 0), 1.0f, distance);

    public final ToggleOption text = new ToggleOption(this, "Text",
            "Show each entity's distance as billboard text above it", true);
    public final SliderOption bloom = new SliderOption(this, "Bloom",
            "Text bloom blur passes (0 = off)", 0, 0, 6, 1, text);
    // Kept flat under Text (gated on bloom > 0) rather than nested under Bloom, since deep nesting
    // doesn't render well in the GUI.
    public final ModeOption<BloomMode> original = new ModeOption<>(this, "Original",
            "How the original text shows under the bloom", BloomMode.Keep, () -> bloom.getValue() > 0, text);
    public final SliderOption opacity = new SliderOption(this, "Opacity",
            "Bloom opacity", 1.0, 0.0, 2.0, 0.05, () -> bloom.getValue() > 0, text);

    private ExampleRange() {
        super("RangeExample", "Highlights entities within a distance range");
    }

    @EventHandler
    private void onWorldRender(EventWorldRender event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        double low = distance.getMin();
        double high = distance.getMax();
        ColorOption.Value value = color.getValue();

        WorldDrawer.start();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !EntityHelper.isAlive(entity)) continue;

            double dist = EntityHelper.getDistance(mc.player, entity);
            if (dist < low || dist > high) continue;

            Vec3 pos = EntityHelper.getInterpolatedPos(entity, event.tickDelta);
            double halfWidth = entity.getBbWidth() / 2.0;
            AABB box = new AABB(
                    pos.x - halfWidth, pos.y, pos.z - halfWidth,
                    pos.x + halfWidth, pos.y + entity.getBbHeight(), pos.z + halfWidth);

            WorldDrawer.boxLines(value, box);
        }
        WorldDrawer.draw(event.matrices);
    }

    @EventHandler
    private void onHudRender(EventHudRender event) {
        if (!text.getValue()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        double low = distance.getMin();
        double high = distance.getMax();
        int passes = bloom.getValue().intValue();
        boolean blooming = passes > 0;

        // One bloom region wraps every label, so all of them cost a single batched blur pass.
        if (blooming) TextDrawer.bloomStart(passes, opacity.getValue().floatValue(), original.getValue());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !EntityHelper.isAlive(entity)) continue;

            double dist = EntityHelper.getDistance(mc.player, entity);
            if (dist < low || dist > high) continue;

            Vec3 pos = EntityHelper.getInterpolatedPos(entity, event.tickDelta)
                    .add(0.0, entity.getBbHeight() + 0.4, 0.0);
            if (!Billboard.start(pos, event.context, 1.0)) continue;

            String label = String.format("%.1f", dist);
            TextDrawer.start(TextType.HUD, 1.0);
            double w = TextDrawer.getWidth(label, false);
            double h = TextDrawer.getHeight(false);
            TextDrawer.render(label, -w / 2.0, -h / 2.0, color.getValue().color, 1.0f, false);
            TextDrawer.draw(event.context);

            Billboard.stop(event.context);
        }

        if (blooming) TextDrawer.bloomStop();
    }
}
