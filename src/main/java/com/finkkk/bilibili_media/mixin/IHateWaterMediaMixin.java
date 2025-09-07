package com.finkkk.bilibili_media.mixin;

import com.finkkk.bilibili_media.BiliBiliMedia;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.watermedia.WaterMedia;

@Mixin(WaterMedia.class)
public class IHateWaterMediaMixin {
    @Inject(method = "start", remap = false, at = @At("RETURN"))
    private void log(CallbackInfo ci){
        // 你知道吗：
        // 我首先看到water media支持YouTube后高兴的打开了代理
        // 然后water media一个无效链接甩我脸上
        // 看源码发现它根本没有兼容代理
        // 我写了个模组修复后又不行
        // 以为是版本的问题换到neoforge又不行
        // 最后一行一行调试发现YouTube把我当人机了
        // 然后我写了个本地服务器下载YouTube视频
        // 结果老是登录失败，就换成哔哩哔哩了
        // 最后是用cct电脑模组加上一个python服务器解决的哔哩哔哩视频解析
        // 觉得太麻烦了就有了这个模组
        // 前后折腾了一个星期
        // 这声No我说的理直气壮
        // https://www.bilibili.com/video/BV16aqtYLE61
        BiliBiliMedia.LOGGER.info("No.");
    }
}
