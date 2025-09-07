package com.finkkk.bilibili_media.util;

import com.finkkk.bilibili_media.BiliBiliMedia;
import org.watermedia.api.network.patchs.AbstractPatch;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BilibiliPatch extends AbstractPatch {
    @Override
    public String platform() {
        return "bilibili";
    }

    @Override
    public boolean isValid(URI uri) {
        if(!BiliBiliMedia.config.enable){return false;}
        return uri.toString().contains("b23.tv") || uri.toString().contains("bilibili.com");
    }

    @Override
    public Result patch(URI uri, Quality prefQuality) throws FixingURLException {
        super.patch(uri, prefQuality);
        uri = URI.create(extractUrl(uri.toString()));

        var dl = BilibiliMediaUtil.tryGetLocalFile(uri.toString());
        if(dl != null){
            return new Result(URI.create(dl), false, false);
        }

        UUID videoUUID = UUID.randomUUID();

        patchWithBBDown(uri, videoUUID);

        File outFile;
        var l = BilibiliMediaUtil.getDownloadPath().toFile().listFiles((dir, name) -> name.contains(videoUUID.toString()));
        if(l == null || l.length == 0){
            BiliBiliMedia.LOGGER.error("下载成功，但找不到文件");
            throw new FixingURLException(uri, new RuntimeException("下载成功但找不到文件"));
        }
        outFile = Arrays.asList(l).get(0);
        BilibiliMediaUtil.updateVideoFile(uri.toString(), outFile);
        return new Result(BilibiliMediaUtil.getUri(outFile), false, false);
    }

    public static String extractUrl(String inputUrl) throws FixingURLException {
        Pattern pattern = Pattern.compile("https?://(?:www\\.bilibili\\.com/video/BV\\w+|b23\\.tv/\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputUrl);

        if (matcher.find()) {
            return matcher.group(0);
        } else {
            throw new FixingURLException(inputUrl, new RuntimeException("url is sus"));
        }
    }

    private static void patchWithBBDown(URI uri, UUID videoUUID) throws FixingURLException {
        try {
            int exitCode = getExitCode(uri, videoUUID);
            BiliBiliMedia.LOGGER.info("程序以{}退出", exitCode);
            if(exitCode != 0){
                throw new FixingURLException(uri, new RuntimeException("BBDown下载失败"));
            }

        } catch (IOException | InterruptedException e) {
            BiliBiliMedia.LOGGER.error("BBDown下载失败", e);
            throw new FixingURLException(uri, new RuntimeException("BBDown下载失败"));
        }
    }

    private static int getExitCode(URI uri, UUID videoUUID) throws IOException, InterruptedException {
        Path exePath = BilibiliMediaUtil.getDownloadPath().resolve("BBDown.exe");
        ProcessBuilder builder = new ProcessBuilder(
                exePath.toString(),
                uri.toString(),
                "--work-dir", BilibiliMediaUtil.getDownloadPath().toString(),
                "-F", videoUUID.toString(),
                "-M", videoUUID.toString(),
                "--skip-cover",
                "--skip-subtitle",
                "-p", "1");
        // 懒得写视频分辨率解析了，反正WaterMedia又不给

        // 设置工作目录
        builder.directory(BilibiliMediaUtil.getDownloadPath().toFile());

        builder.redirectErrorStream(true);

        Process process = builder.start();

        builder.environment();  // 我不知道为什么要加这行代码，但好像可以修复bug

        return process.waitFor();
    }
}
