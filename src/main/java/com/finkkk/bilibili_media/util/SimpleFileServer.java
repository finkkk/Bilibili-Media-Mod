package com.finkkk.bilibili_media.util;

import com.finkkk.bilibili_media.BiliBiliMedia;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleFileServer {

    private static final String DOWNLOAD_DIR = BilibiliMediaUtil.getDownloadPath().toString();
    public static final int PORT = 9095;
    private static final int THREAD_POOL_SIZE = 12; // 可根据需要调整线程池大小

    public static boolean enableRangeRequests = false; // 断点续传开关(默认关闭)

    // 保存启动时要提示的消息（只保存一条）
    private static String pendingMessage = null;

    public static HttpServer startServer() {
        enableRangeRequests = BiliBiliMedia.config.enableRangeRequests;

        int port = PORT;
        int maxTries = 20;
        HttpServer server = null;

        for (int i = 0; i < maxTries; i++) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (IOException e) {
                if (e instanceof java.net.BindException) {
                    BiliBiliMedia.LOGGER.warn("[bilibili_media] 端口 {} 被占用，尝试下一个端口 {}", port, port + 1);
                    port++;
                } else {
                    BiliBiliMedia.LOGGER.error("[bilibili_media] 启动本地服务器失败: {}", e.getMessage(), e);
                    setPendingMessage("[bilibili_media] 本地HTTP服务器启动失败: " + e.getMessage());
                    return null;
                }
            }
        }

        if (server == null) {
            String msg = "[B站解析模组] 启动本地服务器失败，端口范围已尝试完毕，本地视频缓存功能将禁用";
            BiliBiliMedia.LOGGER.error(msg);
            setPendingMessage(msg);
            return null;
        }

        server.createContext("/download", new FileDownloadHandler(enableRangeRequests));
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        server.setExecutor(executor);
        server.start();

        String msg = "[B站解析模组] 本地HTTP服务器已启动，端口 " + port;
        BiliBiliMedia.LOGGER.info(msg);
        setPendingMessage(msg);
        return server;
    }

    // 保存消息（只在客户端生效）
    private static void setPendingMessage(String message) {
        if (FMLEnvironment.dist.isClient()) {
            pendingMessage = message;
        }
    }

    // 玩家进入世界时调用
    public static void flushPendingMessage() {
        if (pendingMessage != null && Minecraft.getInstance().player != null) {
            ChatFormatting color = pendingMessage.contains("失败") ? ChatFormatting.RED : ChatFormatting.GREEN;
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(pendingMessage).withStyle(color), false
            );
            pendingMessage = null; // 打印后清空
        }
    }

    static class FileDownloadHandler implements HttpHandler {
        private final boolean enableRangeRequests;

        public FileDownloadHandler(boolean enableRangeRequests) {
            this.enableRangeRequests = enableRangeRequests;
        }

        @Override
        public void handle(HttpExchange exchange) {
            try {
                String query = exchange.getRequestURI().getQuery();
                String fileName = query != null && query.startsWith("file=")
                        ? query.substring(5)
                        : null;

                if (fileName == null || fileName.isEmpty()) {
                    sendError(exchange, 400, "Missing file parameter. Usage: /download?file=filename");
                    return;
                }

                Path filePath = Paths.get(DOWNLOAD_DIR, fileName).normalize();
                if (!filePath.startsWith(Paths.get(DOWNLOAD_DIR))) {
                    sendError(exchange, 403, "Access denied");
                    return;
                }

                File file = filePath.toFile();
                if (!file.exists() || !file.isFile()) {
                    sendError(exchange, 404, "File not found: " + fileName);
                    return;
                }

                // 设置响应头
                String mimeType = Files.probeContentType(filePath);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(file.length()));

                // 根据开关决定是否处理 Range 请求
                if (enableRangeRequests) {
                    String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
                    if (rangeHeader != null) {
                        handleRangeRequest(exchange, file, rangeHeader);
                        return;
                    }
                }

                // 默认发送完整文件
                sendFullFile(exchange, file);
            } catch (Exception e) {
//                BiliBiliMedia.LOGGER.error("处理请求时出错", e);
                try {
                    sendError(exchange, 500, "Internal server error");
                } catch (IOException ex) {
//                    BiliBiliMedia.LOGGER.error("发送错误响应时出错", ex);
                }
                // WaterMedia会在第一次请求直接中断后进行第二次请求，造成日志污染
            }
        }

        private void sendFullFile(HttpExchange exchange, File file) throws IOException {
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(file.toPath(), os);
            }
            BiliBiliMedia.LOGGER.info("发送文件：{}", file);
        }

        private void handleRangeRequest(HttpExchange exchange, File file, String rangeHeader) throws IOException {
            long fileLength = file.length();
            long start, end = fileLength - 1;

            String range = rangeHeader.replaceAll("bytes=", "");
            String[] parts = range.split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1) {
                end = Long.parseLong(parts[1]);
            }

            long contentLength = end - start + 1;
            exchange.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            exchange.sendResponseHeaders(206, contentLength);

            try (OutputStream os = exchange.getResponseBody();
                 var fis = Files.newInputStream(file.toPath())) {
                fis.skipNBytes(start);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                while (remaining > 0) {
                    int read = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) break;
                    os.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        }

        private void sendError(HttpExchange exchange, int code, String message) throws IOException {
            exchange.sendResponseHeaders(code, message.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(message.getBytes());
            }
        }
    }
}