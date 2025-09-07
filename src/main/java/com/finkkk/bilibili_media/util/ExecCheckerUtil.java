package com.finkkk.bilibili_media.util;

import com.finkkk.bilibili_media.BiliBiliMedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class ExecCheckerUtil {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    /**
     * 异步检测某个可执行文件是否可运行
     * @param name       程序名（仅用于日志）
     * @param exe        可执行文件路径
     * @param args       启动参数，例如 "--help", "-version"
     */
    public static void checkExecutableAsync(String name, File exe, String... args) {
        if (!exe.exists() || !exe.isFile()) {
            BiliBiliMedia.LOGGER.warn("[{}] 未找到: {}", name, exe.getAbsolutePath());
            return;
        }

        EXECUTOR.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(Stream.concat(Stream.of(exe.getAbsolutePath()), Arrays.stream(args)).toList());
                pb.directory(exe.getParentFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();

                // 🔥 读取子进程输出并打印到日志
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        BiliBiliMedia.LOGGER.info("[{} 输出] {}", name, line);
                    }
                }

                int exit = process.waitFor();

                if (exit == 0) {
                    BiliBiliMedia.LOGGER.info("[{}] 可运行: {}", name, exe.getAbsolutePath());
                } else {
                    BiliBiliMedia.LOGGER.warn("[{}] 运行失败，退出码 {}", name, exit);
                }
            } catch (IOException e) {
                BiliBiliMedia.LOGGER.error("[{}] 启动失败: {}", name, e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                BiliBiliMedia.LOGGER.error("[{}] 检测被中断", name, e);
            }
        });
    }
}