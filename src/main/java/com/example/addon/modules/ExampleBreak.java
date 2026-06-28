package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventInteract;
import dev.boze.api.utility.interaction.BreakHelper;
import dev.boze.api.utility.interaction.Interaction;
import dev.boze.api.utility.interaction.InteractionMode;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Example module showcasing BreakHelper by breaking the block under the crosshair with rotations
 */
public class ExampleBreak extends AddonModule {
    public static final ExampleBreak INSTANCE = new ExampleBreak();

    private ExampleBreak() {
        super("BreakExample", "Breaks the block you are looking at using BreakHelper");
    }

    @EventHandler
    private void onInteract(EventInteract event) {
        if (event.getMode() != InteractionMode.NCP) return;
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)) return;

        BlockPos pos = hit.getBlockPos();
        if (!BreakHelper.canBreak(pos)) return;

        Interaction interaction = BreakHelper.interaction(pos, true);
        if (interaction != null) event.addInteraction(interaction);
    }
}
