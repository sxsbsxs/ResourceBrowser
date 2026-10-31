# ResourceBrowser

一个基于 Android WebView 的开源浏览器，界面参考 Chrome Android，逐步集成媒体资源识别、下载、书签与历史功能。

## 当前已实现

- [x] Kotlin + Jetpack Compose Android 工程
- [x] Chrome 风格地址栏、加载进度和底部导航
- [x] WebView 网页加载、标题、前进/后退和主页
- [x] 多标签页：新建、切换、关闭，每个标签独立 WebView
- [x] 根据请求 URL 扩展名发现 MP4、WebM、M3U8、MP3、PDF、MPD 等资源
- [x] 媒体资源列表查看、清空及点击打开
- [x] 页面内资源出现后显示悬浮快捷入口
- [x] 书签添加和查看
- [x] 浏览历史查看和清空
- [x] WebView 下载监听，通过 Android DownloadManager 创建下载任务
- [x] 基础 WebView 安全设置：关闭 file/content access、阻止混合内容

## 后续开发

- [ ] Room 数据库（当前书签和历史使用 SharedPreferences）
- [ ] 更完整的媒体识别：fetch/XHR hook、blob URL、Range 请求与分片关联
- [ ] 资源悬浮球与底部资源面板
- [ ] 下载任务管理：进度、暂停、继续、取消、打开文件
- [ ] M3U8 master playlist 清晰度解析和合规下载流程
- [ ] DASH 播放列表提示与支持策略
- [ ] 真正隔离的隐私模式（独立 Cookie、缓存和存储；当前只提供不记历史的会话开关基础）
- [ ] 设置页、深色主题
- [ ] GitHub Actions 构建验证、APK Release

## 构建

需要 Android Studio、JDK 17、Android SDK 35 与 Gradle 8.13。

```bash
gradle assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

## 注意事项

资源发现目前依赖网络请求 URL 的文件扩展名，无法保证识别所有网站或动态媒体流。下载功能将网页通过 WebView 暴露的下载请求交给 Android DownloadManager；目前尚无完整的下载任务管理界面。仅对用户有权访问和保存的内容提供识别与下载功能；不提供 DRM 破解、付费墙绕过或访问控制规避功能。

## License

Apache-2.0，详见 [LICENSE](LICENSE)。
