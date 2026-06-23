package com.example.addon.mixin;

import com.example.addon.ExampleAddon;
import dev.boze.api.BozeInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {
    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;instance:Lnet/minecraft/client/Minecraft;"))
    private void onInit$setInstance(GameConfig args, CallbackInfo ci) {
        BozeInstance.INSTANCE.registerAddon(new ExampleAddon());
    }
}
