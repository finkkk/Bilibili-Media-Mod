# Bilibili-Media-Mod

**Bilibili-Media-Mod** 是一款基于 [WaterMedia](https://modrinth.com/mod/watermedia) 的扩展模组，使玩家能够在 Minecraft 游戏内直接加载并播放哔哩哔哩视频。  

相比原版 [BiliBiliMedia](https://modrinth.com/mod/bilibilimedia) 模组，本分支进行了功能增强与优化，提供更好的即用体验。

---

##  功能特性

-  **即开即用**：内置 **FFmpeg** 与 **BBDown**，无需额外配置或安装。  
-  **缓存自动清理**：新增视频缓存清理功能，默认启用，避免存储空间占用过大。  
-  **完全兼容 WaterMedia**：支持哔哩哔哩视频直链解析与播放。  
-  **平台支持**：当前已在 **Windows 系统** 测试通过，其他平台兼容性尚未验证。  

---

##  下载与安装

1. 安装 [WaterMedia](https://modrinth.com/mod/watermedia) 模组作为前置模组。  
2. 将本模组放入 `mods` 文件夹。
3. 可搭配waterframes模组配合使用。
4. 启动游戏，即可在 WaterFrames 中直接使用哔哩哔哩视频链接进行播放。  

---

##  相关链接

- 原版模组页面：[Modrinth - BiliBiliMedia](https://modrinth.com/mod/bilibilimedia)  
- 原版源码仓库：[Gitee - bilibiliMediaNeo](https://gitee.com/gly091020/bilibiliMediaNeo)  

---

## ⚠ 声明

本项目为 **非官方分支版本**，与原作者无直接关联。  
其目的是在原版基础上增强功能并提升易用性，本用于 [Naturecraft VR服务器观影功能使用](https://naturecraft.cn)

---

##  许可证

本项目遵循原仓库所使用的开源协议。请参考源码库中的 LICENSE 文件。

---

##  额外依赖(仅手动构建需要)

由于 GitHub 对文件大小有限制，本仓库不直接包含 `ffmpeg.exe` 与 `BBDown.exe`。

如需手动构建，请用户手动下载后放置到项目内的以下目录：

```bash
src/main/resources/assets/bilibili_media/ffmpeg/ffmpeg.exe
src/main/resources/assets/bilibili_media/bbdown/BBDown.exe
```

FFmpeg 下载地址：https://ffmpeg.org/

BBDown 下载地址：https://github.com/nilaoda/BBDown

Release发布版本的模组无需手动下载依赖，已内置完毕，开箱即用。