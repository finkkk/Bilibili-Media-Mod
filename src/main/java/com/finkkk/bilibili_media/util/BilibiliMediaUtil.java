package com.finkkk.bilibili_media.util;

import com.finkkk.bilibili_media.BiliBiliMedia;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BilibiliMediaUtil {
    private static Map<String, String> DATA;
    public static Path getDownloadPath(){
        var p = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles");
        if (!p.toFile().isDirectory() && !p.toFile().mkdir()) {
            throw new RuntimeException("文件夹创建失败");
        }
        return p;
    }

    public static URI getUri(File file){
        return URI.create("http://127.0.0.1:" + SimpleFileServer.PORT + "/download?file=" + file.getName());
    }

    public static void loadJson(){
        var p = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").resolve("video.json");
        if(!p.toFile().isFile()){
            try {
                if(!p.toFile().createNewFile()){
                    throw new IOException("文件创建失败");
                }
                DATA = new HashMap<>();
                saveJson();
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            DATA = gson.fromJson(new String(Files.readAllBytes(p)), new TypeToken<Map<String, String>>() {}.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveJson(){
        var p = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").resolve("video.json");
        if(!p.toFile().isFile()){
            try {
                if(!p.toFile().createNewFile()){
                    throw new IOException("文件创建失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.write(p, gson.toJson(DATA).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static String tryGetLocalFile(String url){
        return DATA.get(url);
    }

    public static void updateVideoFile(String url, File file){
        DATA.put(url, getUri(file).toString());
        saveJson();
    }

    // 受保护文件白名单（统一用小写）
    private static final Set<String> PROTECTED_FILES = Set.of(
            "bbdown.exe",
            "ffmpeg.exe"
    );

    public static void clearFile() {
        File dir = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles").toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            BiliBiliMedia.LOGGER.error("目录创建失败: {}", dir.getAbsolutePath());
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (PROTECTED_FILES.contains(name)) {
                continue; // 白名单文件不删除
            }

            if (f.isDirectory()) {
                deleteDirectory(f);
            } else if (!f.delete()) {
                BiliBiliMedia.LOGGER.warn("无法删除文件: {}", f.getAbsolutePath());
            }
        }
    }

    /** 递归删除文件夹 */
    private static void deleteDirectory(File dir) {
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    if (!f.delete()) {
                        BiliBiliMedia.LOGGER.warn("无法删除文件: {}", f.getAbsolutePath());
                    }
                }
            }
        }
        if (!dir.delete()) {
            BiliBiliMedia.LOGGER.warn("无法删除目录: {}", dir.getAbsolutePath());
        }
    }

    public static boolean isFinkkk() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            return Objects.equal(
                    BiliBiliMedia.MY_UUID,
                    mc.player.getUUID()
            );
        }
        return false;
    }

    public static void tryBBDown() {
        File exe = getDownloadPath().resolve("BBDown.exe").toFile();
        if (!exe.exists()) { //  检查是否存在
            releaseBBDown();
        }
        ExecCheckerUtil.checkExecutableAsync("BBDown", exe, "--help");
    }

    public static void tryFFmpeg() {
        File exe = getDownloadPath().resolve("ffmpeg.exe").toFile();
        if (!exe.exists()) { //  检查是否存在
            releaseFFmpeg();
        }
        ExecCheckerUtil.checkExecutableAsync("ffmpeg", exe, "-version");
    }


    public static void releaseBBDown(){
        var outputPath = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles/BBDown.exe");
        if(outputPath.toFile().exists()){
            BiliBiliMedia.LOGGER.info("检测到BBDown");
            return;
        }
        if(!System.getProperty("os.name").toLowerCase().contains("windows")){
            BiliBiliMedia.LOGGER.info("非 Windows 系统，不释放 BBDown");
            return;
        }
        try (InputStream input = BilibiliMediaUtil.class.getResourceAsStream("/assets/bilibili_media/bbdown/BBDown.exe")) {
            if (input == null) {
                BiliBiliMedia.LOGGER.error("未找到BBDown");
                return;
            }
            Files.copy(input, outputPath, StandardCopyOption.REPLACE_EXISTING);
            boolean setExec = outputPath.toFile().setExecutable(true);
            if (!setExec) {
                BiliBiliMedia.LOGGER.warn("无法设置可执行权限: {}", outputPath);
            }
            BiliBiliMedia.LOGGER.info("释放exe成功");
        } catch (IOException e) {
            BiliBiliMedia.LOGGER.error("无法解压BBDown：", e);
        }
    }

    public static void releaseFFmpeg() {
        var outputPath = FMLPaths.GAMEDIR.get().resolve("BiliBiliMediaFiles/ffmpeg.exe");
        if (outputPath.toFile().exists()) {
            BiliBiliMedia.LOGGER.info("检测到 ffmpeg");
            return;
        }
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            BiliBiliMedia.LOGGER.info("非 Windows 系统，不释放 ffmpeg");
            return;
        }
        try (InputStream input = BilibiliMediaUtil.class.getResourceAsStream("/assets/bilibili_media/ffmpeg/ffmpeg.exe")) {
            if (input == null) {
                BiliBiliMedia.LOGGER.error("未找到 ffmpeg 资源文件");
                return;
            }
            Files.copy(input, outputPath, StandardCopyOption.REPLACE_EXISTING);
            boolean setExec = outputPath.toFile().setExecutable(true);
            if (!setExec) {
                BiliBiliMedia.LOGGER.warn("无法设置可执行权限: {}", outputPath);
            }
            BiliBiliMedia.LOGGER.info("释放 ffmpeg.exe 成功: {}", outputPath);
        } catch (IOException e) {
            BiliBiliMedia.LOGGER.error("无法释放 ffmpeg：", e);
        }
    }

}
