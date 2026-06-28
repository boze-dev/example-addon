package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.client.module.helper.AutoMineHelper;
import dev.boze.api.event.EventTick;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Example module showcasing the AutoMineHelper queue/double-mine API.
 * <br>
 * Feeds the block you are looking at to AutoMine: it fills an active mining slot if one is free
 * (a second one too with DoubleMine), otherwise it is added to the queue. Aim across several blocks
 * to see them mine simultaneously and queue up. Requires AutoMine to be enabled.
 */
public class ExampleMiner extends AddonModule {
    public static final ExampleMiner INSTANCE = new ExampleMiner();

    private ExampleMiner() {
        super("MinerExample", "Mines the block you look at via AutoMine's active slots + queue");
    }

    @EventHandler
    private void onTick(EventTick.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;

        BlockPos pos = hit.getBlockPos();

        // Start mining it in an active slot if one is free, otherwise queue it for later.
        if (AutoMineHelper.canAddTask()) {
            AutoMineHelper.addActiveTask(pos);
        } else {
            AutoMineHelper.addToQueue(pos);
        }
    }
}
