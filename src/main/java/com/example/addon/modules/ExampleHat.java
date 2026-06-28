package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventWorldRender;
import dev.boze.api.option.ColorOption;
import dev.boze.api.render.ColorMaker;
import dev.boze.api.render.WorldDrawer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a cone hat above every player, including yourself when in third person.
 * Showcases the 3D triangle/line drawing API.
 */
public class ExampleHat extends AddonModule {
    public static final ExampleHat INSTANCE = new ExampleHat();

    private static final int SEGMENTS = 24;
    private static final double RADIUS = 0.55;
    private static final double HEIGHT = 0.35;

    public final ColorOption color = new ColorOption(this, "Color", "Hat color",
            ColorMaker.staticColor(220, 40, 40), 0.35f, 0.8f);

    private ExampleHat() {
        super("HatExample", "Renders a hat above players");
    }

    @EventHandler
    private void onWorldRender(EventWorldRender event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        WorldDrawer.start();
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == mc.player && firstPerson) continue;
            renderHat(player, event.tickDelta);
        }
        WorldDrawer.draw(event.matrices);
    }

    private void renderHat(AbstractClientPlayer player, float tickDelta) {
        double x = Mth.lerp(tickDelta, player.xOld, player.getX());
        double baseY = Mth.lerp(tickDelta, player.yOld, player.getY()) + player.getBbHeight();
        double z = Mth.lerp(tickDelta, player.zOld, player.getZ());

        Vec3 apex = new Vec3(x, baseY + HEIGHT, z);

        ColorOption.Value value = color.getValue();

        Vec3 prev = ringPoint(x, baseY, z, 0);
        for (int i = 1; i <= SEGMENTS; i++) {
            Vec3 next = ringPoint(x, baseY, z, i);

            // Filled cone face, drawn both windings so it shows from any angle
            WorldDrawer.triangle(value, apex, prev, next);
            WorldDrawer.triangle(value, apex, next, prev);

            // Outline: brim ring + a rib up to the tip
            WorldDrawer.line(value, prev, next);
            WorldDrawer.line(value, prev, apex);

            prev = next;
        }
    }

    private Vec3 ringPoint(double cx, double cy, double cz, int index) {
        double angle = (Math.PI * 2.0 * index) / SEGMENTS;
        return new Vec3(cx + Math.cos(angle) * RADIUS, cy, cz + Math.sin(angle) * RADIUS);
    }
}
