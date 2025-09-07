package com.finkkk.bilibili_media.client;

import com.finkkk.bilibili_media.BiliBiliMedia;
import com.finkkk.bilibili_media.util.SimpleFileServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BiliBiliMedia.MODID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        SimpleFileServer.flushPendingMessage();
    }
}