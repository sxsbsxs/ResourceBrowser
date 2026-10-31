# ResourceBrowser

一个基于 Android WebView 的开源浏览器，目标是提供接近 Chrome Android 的浏览布局，并集成媒体资源识别、下载管理、书签、历史记录与隐私模式。

## 当前状态

正在积极开发。当前版本已建立 Android 工程，并开始实现 Chrome 风格浏览主界面与 WebView 基础导航。多标签、资源嗅探、下载管理等仍在后续开发中。

## 已实现

- [x] Android / Kotlin / Jetpack Compose 工程骨架
- [x] Chrome Android 风格地址栏与底部导航布局
- [x] WebView 基础网页加载、标题与加载进度
- [x] 地址输入：网址直达与 Google 搜索
- [x] WebView 前进、后退、主页
- [x] 基础 WebView 安全设置（禁用 file/content access、阻止混合内容）

## 开发路线

- [ ] 多标签页（每个标签独立 WebView）
- [ ] 资源嗅探与分类、资源悬浮入口
- [ ] 书签与浏览历史
- [ ] 下载管理与下载进度
- [ ] HLS / M3U8 清晰度解析与下载
- [ ] 隐私模式与设置
- [ ] GitHub Actions 自动构建 APK

## 技术栈

- Kotlin、Android WebView
- Jetpack Compose + Material 3
- Room（计划用于历史与资源记录）
- DataStore（计划用于偏好设置）
- Android DownloadManager（计划用于直链下载）

## 构建

需要 Android Studio、JDK 17、Android SDK 35 与 Gradle 8.13。

```bash
gradle assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

## 安全与合规

仅对用户有权访问和保存的内容提供识别与下载功能；不提供 DRM 破解、付费墙绕过或访问控制规避功能。

## License

Apache-2.0，详见 [LICENSE](LICENSE)。
