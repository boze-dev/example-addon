package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventHudRender;
import dev.boze.api.option.ColorOption;
import dev.boze.api.render.Billboard;
import dev.boze.api.render.ColorMaker;
import dev.boze.api.render.HudDrawer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Example module showcasing flat 2D block highlighting using Billboard and HudDrawer
 * Renders a simple quad at the targeted block position
 */
public class ExampleBlockHighlight extends AddonModule {
    public static final ExampleBlockHighlight INSTANCE = new ExampleBlockHighlight();

    public final ColorOption color = new ColorOption(this, "Color", "Color for block highlight", ColorMaker.staticColor(0, 255, 0), 0.5f);

    private ExampleBlockHighlight() {
        super("BlockHighlightExample", "Flat 2D block highlight example");
    }

    @EventHandler
    public void onHudRender(EventHudRender event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getBlockPos() != null) {
            Vec3d center = new Vec3d(
                result.getBlockPos().getX() + 0.5,
                result.getBlockPos().getY() + 0.5,
                result.getBlockPos().getZ() + 0.5
            );

            Billboard.start(center, event.context, 1.0);
            HudDrawer.start();
            HudDrawer.quad(color.getValue().color, color.getValue().fillOpacity, -50, -50, 100, 100);
            HudDrawer.draw();
            Billboard.stop(event.context);
        }
    }
}
