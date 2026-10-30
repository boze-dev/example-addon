package com.example.addon.modules;

import dev.boze.api.addon.AddonModule;
import dev.boze.api.event.EventTick;
import dev.boze.api.option.SliderOption;
import dev.boze.api.utility.ChatHelper;
import dev.boze.api.utility.NewChunksHelper;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Example module counting the NewChunks marks around the player and printing them to chat. Needs
 * the NewChunks module on.
 */
public class ExampleNewChunks extends AddonModule {
    public static final ExampleNewChunks INSTANCE = new ExampleNewChunks();

    public final SliderOption radius = new SliderOption(this, "Radius",
            "Chunks around you to read", 16, 1, 64, 1);
    public final SliderOption interval = new SliderOption(this, "Interval",
            "Ticks between chat reports", 100, 1, 200, 1);

    private int newCount;
    private int oldCount;
    private int legacyCount;
    private int updatingCount;

    // A field rather than a lambda at the call site, so reading every tick allocates nothing.
    private final NewChunksHelper.MarkedChunkConsumer counter = (chunkX, chunkZ, age, detail) -> {
        if (age == NewChunksHelper.ChunkAge.NEW) newCount++;
        else oldCount++;

        if (detail == NewChunksHelper.ChunkDetail.LEGACY) legacyCount++;
        else if (detail == NewChunksHelper.ChunkDetail.UPDATING) updatingCount++;
    };

    private ExampleNewChunks() {
        super("NewChunksExample", "Counts the NewChunks marks around you");
    }

    @EventHandler
    private void onTick(EventTick.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int chunkX = player.getBlockX() >> 4;
        int chunkZ = player.getBlockZ() >> 4;

        newCount = 0;
        oldCount = 0;
        legacyCount = 0;
        updatingCount = 0;
        NewChunksHelper.forEachMarked(chunkX, chunkZ, radius.getValue().intValue(), counter);

        if (player.tickCount % interval.getValue().intValue() == 0) {
            ChatHelper.sendMsg(getTitle(), newCount + " new, " + oldCount + " old ("
                    + legacyCount + " legacy, " + updatingCount + " updating) in "
                    + player.level().dimension().identifier().getPath() + ", this chunk is "
                    + NewChunksHelper.getAge(chunkX, chunkZ) + "/" + NewChunksHelper.getDetail(chunkX, chunkZ));
        }
    }
}
