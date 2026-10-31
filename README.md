# ResourceBrowser

一个基于 Android WebView 的开源浏览器，目标是提供接近 Chrome Android 的浏览布局，并集成媒体资源识别、下载管理、书签、历史记录与隐私模式。

## 当前状态

项目正在搭建中。当前提交建立 Android / Kotlin / Jetpack Compose 工程骨架；后续按模块逐步实现功能，不将尚未实现的功能标记为完成。

## 技术栈

- Kotlin
- Jetpack Compose + Material 3
- Android WebView
- Room（历史记录、资源记录）
- DataStore（偏好设置）
- Android DownloadManager（直链下载）

## 开发环境

- Android Studio（建议使用稳定版）
- JDK 17
- Android SDK（compileSdk 35）
- Gradle 8.13

## 构建

```bash
gradle assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 功能路线

- [x] Android 项目骨架
- [ ] Chrome 风格浏览器主界面与 WebView
- [ ] 多标签页
- [ ] 资源嗅探与分类
- [ ] 书签与浏览历史
- [ ] 下载管理
- [ ] HLS / M3U8 清晰度解析
- [ ] 隐私模式与设置
- [ ] GitHub Actions 自动构建 APK

## 安全与合规

仅对用户有权访问和保存的内容提供识别与下载功能；不提供 DRM 破解、付费墙绕过或访问控制规避功能。

## License

Apache-2.0
