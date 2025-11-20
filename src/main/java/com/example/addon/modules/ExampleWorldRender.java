package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventShader;
import dev.boze.api.event.EventWorldRender;
import dev.boze.api.option.*;
import dev.boze.api.render.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * Example module showcasing 3D world rendering capabilities
 * Demonstrates WorldDrawer API with various box rendering options
 */
public class ExampleWorldRender extends AddonModule {
    public static final ExampleWorldRender INSTANCE = new ExampleWorldRender();

    // Color options
    public final ColorOption boxColor = new ColorOption(this, "BoxColor", "Color for rendered boxes",
        ColorMaker.staticColor(255, 0, 0), 0.1F, 1.0F);

    // Rendering modes
    public final ToggleOption filledBoxes = new ToggleOption(this, "FilledBoxes", "Render filled box sides", true);
    public final ToggleOption outlineBoxes = new ToggleOption(this, "OutlineBoxes", "Render box outlines", true);
    public final ToggleOption shaderMode = new ToggleOption(this, "ShaderMode", "Use shader rendering", false);

    // Size and range settings
    public final SliderOption boxSize = new SliderOption(this, "BoxSize", "Size of rendered boxes", 1.0, 0.1, 3.0, 0.1);

    // Animation settings
    public final ToggleOption animate = new ToggleOption(this, "Animate", "Enable size animation", false);
    public final SliderOption animationSpeed = new SliderOption(this, "AnimationSpeed", "Animation speed multiplier", 1.0, 0.1, 5.0, 0.1, animate::getValue);

    private ExampleWorldRender() {
        super("WorldRenderExample", "Showcase 3D world rendering with WorldDrawer API");
    }

    @EventHandler
    public void onShader(EventShader event) {
        if (shaderMode.getValue()) event.prepare(boxColor.getValue().color);
    }

    @EventHandler
    public void onWorldRender(EventWorldRender event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getBlockPos() != null) {
            BlockPos pos = result.getBlockPos();

            // Calculate animated size if enabled
            double currentSize = boxSize.getValue();
            if (animate.getValue()) {
                long time = System.currentTimeMillis();
                double animation = Math.sin(time * 0.001 * animationSpeed.getValue()) * 0.2;
                currentSize += animation;
                currentSize = MathHelper.clamp(currentSize, 0.1, 3.0);
            }

            // Create box around the target block
            Box box = new Box(pos).expand(currentSize - 1.0);

            WorldDrawer.start();

            if (filledBoxes.getValue() && outlineBoxes.getValue()) {
                // Render both fill and outline
                WorldDrawer.dynamicBox(boxColor.getValue(), shaderMode.getValue(),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            } else if (filledBoxes.getValue() || shaderMode.getValue()) {
                // Render only filled sides
                WorldDrawer.dynamicBoxSides(boxColor.getValue(), shaderMode.getValue(),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            } else if (outlineBoxes.getValue()) {
                // Render only outlines
                WorldDrawer.boxLines(boxColor.getValue(),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            }

            WorldDrawer.draw(event.matrices);
        }
    }
}
