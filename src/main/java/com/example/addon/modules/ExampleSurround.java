package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventInteract;
import dev.boze.api.utility.MathHelper;
import dev.boze.api.utility.interaction.Interaction;
import dev.boze.api.utility.interaction.InteractionMode;
import dev.boze.api.utility.interaction.InvHelper;
import dev.boze.api.utility.interaction.PlaceHelper;
import dev.boze.api.utility.interaction.SwapType;
import dev.boze.api.option.ToggleOption;
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
            BlockHitResult hitResult = PlaceHelper.cast(targetPos, InteractionMode.NCP);
            if (hitResult != null) {
                Runnable place = () -> {
                    InvHelper.swapToSlot(obsidianSlot, SwapType.Silent);
                    PlaceHelper.place(InteractionMode.NCP, hitResult, Hand.MAIN_HAND);
                    InvHelper.swapBack();
                };

                if (rotate.getValue()) {
                    float[] rotation = MathHelper.calculateRotation(MinecraftClient.getInstance().player.getEyePos(), hitResult.getPos());
                    event.addInteraction(new Interaction(place, rotation[0], rotation[1]));
                } else {
                    event.addInteraction(new Interaction(place));
                }
            }
        }
    }
}
