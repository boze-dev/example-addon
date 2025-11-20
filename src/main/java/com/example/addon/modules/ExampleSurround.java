package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventInteract;
import dev.boze.api.render.ColorMaker;
import dev.boze.api.render.PlaceRenderer;
import dev.boze.api.utility.MathHelper;
import dev.boze.api.utility.interaction.Interaction;
import dev.boze.api.utility.interaction.InteractionMode;
import dev.boze.api.utility.interaction.InvHelper;
import dev.boze.api.utility.interaction.PlaceHelper;
import dev.boze.api.utility.interaction.SwapType;
import dev.boze.api.option.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


/**
 * Example module showcasing inventory, swap, and place helpers
 */
public class ExampleSurround extends AddonModule {
    public static final ExampleSurround INSTANCE = new ExampleSurround();

    public final ToggleOption rotate = new ToggleOption(this, "Rotate", "Rotate to place blocks", true);
    public final ToggleOption airPlace = new ToggleOption(this, "AirPlace", "Place blocks in air", false);
    public final ToggleOption render = new ToggleOption(this, "Render", "Render placement visualizations", true);
    public final ColorOption renderColor = new ColorOption(this, "RenderColor", "Color for placement visualizations",
        ColorMaker.staticColor(255, 0, 0), 0.1F, 1.0F, render);
    public final ToggleOption useShader = new ToggleOption(this, "UseShader", "Use shader rendering for placements", false, render);

    private ExampleSurround() {
        super("SurroundExample", "Basic surround example");
    }

    private final Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    @EventHandler
    private void onInteract(EventInteract event) {
        if (event.getMode() != InteractionMode.NCP) return;

        // Find obsidian in hotbar
        int obsidianSlot = InvHelper.findInHotbar(Blocks.OBSIDIAN);
        if (obsidianSlot == -1) return; // No obsidian in hotbar, don't try to place

        // Get player position as center
        BlockPos center = MinecraftClient.getInstance().player.getBlockPos();

        // Try to place blocks in all 4 directions
        for (Direction dir : directions) {
            BlockPos targetPos = center.offset(dir);

            if (!PlaceHelper.isEmpty(targetPos)) continue;

            // Cast to try to find a valid placement position
            BlockHitResult hitResult = PlaceHelper.cast(targetPos, airPlace.getValue(), InteractionMode.NCP);
            if (hitResult != null) {
                Runnable place = () -> {
                    InvHelper.swapToSlot(obsidianSlot, SwapType.Silent);
                    PlaceHelper.place(InteractionMode.NCP, hitResult, Hand.MAIN_HAND);
                    InvHelper.swapBack();

                    // Add placement visualization if render is enabled
                    if (render.getValue()) {
                        ColorOption.Value colorValue = renderColor.getValue();
                        PlaceRenderer.addPlacement(new PlaceRenderer.PlacementRecord(
                                PlaceRenderer.getRenderPos(hitResult),
                                System.currentTimeMillis(),
                                1000L, // duration: 1000ms
                                colorValue.color,
                                colorValue.fillOpacity,
                                colorValue.outlineOpacity,
                                0.1f, // animOpacity
                                0.1f, // animGrow
                                0.1f, // animShrink
                                useShader.getValue()
                        ));
                    }
                };

                float[] rotation = MathHelper.calculateRotation(hitResult.getPos(), rotate.getValue());
                event.addInteraction(new Interaction(place, rotate.getValue(), rotation[0], rotation[1]));
            }
        }
    }
}
