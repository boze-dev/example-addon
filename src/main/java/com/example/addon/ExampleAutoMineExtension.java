package com.example.addon;

import dev.boze.api.client.ModuleManager;
import dev.boze.api.client.module.ClientModuleExtension;
import dev.boze.api.client.module.helper.AutoMineHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Example extension for the AutoMine module that lets it mine bedrock.
 * <br>
 * While AutoMine is enabled, this installs a custom canBreak predicate that additionally allows
 * bedrock; it is removed when AutoMine is disabled.
 */
public class ExampleAutoMineExtension extends ClientModuleExtension {

    public ExampleAutoMineExtension() {
        super(ModuleManager.getClientModule("AutoMine"));
    }

    @Override
    public void onEnable() {
        AutoMineHelper.setCanBreak((pos, state) -> state.getBlock() == Blocks.BEDROCK);
    }

    @Override
    public void onDisable() {
        AutoMineHelper.resetCanBreak();
    }
}
