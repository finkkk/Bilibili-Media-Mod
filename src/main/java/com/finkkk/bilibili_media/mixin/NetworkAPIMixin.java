package com.finkkk.bilibili_media.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.watermedia.api.network.NetworkAPI;
import org.watermedia.api.network.patchs.AbstractPatch;
import org.watermedia.loaders.ILoader;
import com.finkkk.bilibili_media.util.BilibiliPatch;

import java.util.List;

@Mixin(NetworkAPI.class)
public abstract class NetworkAPIMixin {
    @Shadow(remap = false) @Final private static List<AbstractPatch> FIXERS;

    @Inject(method = "start", at = @At("RETURN"), remap = false)
    private void appendBilibili(ILoader bootCore, CallbackInfo ci){
        FIXERS.add(new BilibiliPatch());
    }
}
