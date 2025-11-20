package com.example.addon.mixin;

import com.example.addon.ExampleAddon;
import dev.boze.api.BozeInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;instance:Lnet/minecraft/client/MinecraftClient;"))
    private void onInit$setInstance(RunArgs args, CallbackInfo ci) {
        BozeInstance.INSTANCE.registerAddon(new ExampleAddon());
    }
}
