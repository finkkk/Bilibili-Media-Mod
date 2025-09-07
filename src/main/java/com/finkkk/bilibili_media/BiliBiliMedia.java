package com.finkkk.bilibili_media;

import com.finkkk.bilibili_media.config.BiliBiliMediaConfig;
import com.finkkk.bilibili_media.util.BilibiliMediaUtil;
import com.finkkk.bilibili_media.util.SimpleFileServer;
import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.util.UUID;

@Mod(BiliBiliMedia.MODID)
public class BiliBiliMedia
{

    public static final String MODID = "bilibili_media";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static BiliBiliMediaConfig config;
    public static HttpServer server;
    public static final UUID MY_UUID = UUID.fromString("99e73d9f-4a4c-447a-8d1a-9877b8dd3a3d");

    public BiliBiliMedia() {
        // 1. 注册配置 (AutoConfig + GsonConfigSerializer)
        AutoConfig.register(BiliBiliMediaConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(BiliBiliMediaConfig.class).getConfig();

        // 2. 注册 ConfigScreen (Forge 1.20.1 写法)
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> BiliBiliMediaConfig.getConfigScreen(parent))
        );

        // 3. 启动本地 SimpleFileServer
        server = SimpleFileServer.startServer();
        if (server != null) {
            LOGGER.info("BilibiliMedia SimpleFileServer 已启动");
        } else {
            LOGGER.warn("BilibiliMedia SimpleFileServer 启动失败，本地视频缓存功能已禁用");
        }

        // 4. 启动参数检查
        if (config.clearOnStart) {
            try {
                BilibiliMediaUtil.clearFile();
            } catch (Exception e) {
                LOGGER.error("清理缓存失败", e);
            }
        }

        // 5. 释放/测试依赖工具
        try {
            BilibiliMediaUtil.tryBBDown();
            BilibiliMediaUtil.tryFFmpeg();
        } catch (Exception e) {
            LOGGER.error("依赖工具检测失败", e);
        }

        // 6. 读取缓存 JSON
        try {
            BilibiliMediaUtil.loadJson();
        } catch (Exception e) {
            LOGGER.error("加载缓存 JSON 失败", e);
        }
    }
}
