package com.sxsbsxs.resourcebrowser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

private class BrowserTab(val id: Int, val view: WebView) {
    var title by mutableStateOf("新标签页")
    var url by mutableStateOf("about:blank")
    var progress by mutableFloatStateOf(0f)
    var loading by mutableStateOf(false)
}
private data class FoundResource(val url: String, val type: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen() {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    val prefs = remember { context.getSharedPreferences("browser_data", Context.MODE_PRIVATE) }
    val tabs = remember { mutableStateListOf<BrowserTab>() }
    val resources = remember { mutableStateListOf<FoundResource>() }
    var nextId by remember { mutableIntStateOf(1) }
    var activeId by remember { mutableIntStateOf(0) }
    var address by remember { mutableStateOf("https://www.google.com") }
    var panel by remember { mutableStateOf("") }
    var privateSession by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(prefs.getStringSet("bookmarks", emptySet())?.toList() ?: emptyList()) }
    var history by remember { mutableStateOf(prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()) }
    val active = tabs.firstOrNull { it.id == activeId }

    fun saveHistory(url: String) {
        if (privateSession || !url.startsWith("http")) return
        history = (listOf(url) + history.filterNot { it == url }).take(150)
        prefs.edit().putStringSet("history", history.toSet()).apply()
    }

    fun addTab(start: String = "https://www.google.com") {
        val tab = BrowserTab(nextId++, WebView(context))
        val web = tab.view
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        web.settings.setSupportMultipleWindows(false)
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                main.post {
                    tab.loading = true
                    tab.progress = 0f
                    if (!url.isNullOrBlank()) { tab.url = url; if (activeId == tab.id) address = url }
                }
            }
            override fun onPageFinished(view: WebView, url: String?) {
                main.post {
                    tab.loading = false
                    tab.progress = 1f
                    tab.title = view.title?.takeIf { it.isNotBlank() } ?: "新标签页"
                    if (!url.isNullOrBlank()) {
                        tab.url = url
                        if (activeId == tab.id) address = url
                        saveHistory(url)
                    }
                }
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                request.url.scheme !in listOf("http", "https")
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? {
                val uri = request.url
                val path = uri.path?.lowercase(Locale.ROOT).orEmpty()
                val type = when {
                    path.endsWith(".m3u8") -> "HLS / M3U8"
                    path.endsWith(".mpd") -> "DASH / MPD"
                    listOf(".mp4", ".webm", ".mov", ".flv", ".m4v").any { path.endsWith(it) } -> "视频"
                    listOf(".mp3", ".m4a", ".aac", ".wav", ".ogg", ".flac").any { path.endsWith(it) } -> "音频"
                    path.endsWith(".pdf") -> "PDF 文档"
                    else -> null
                }
                if (type != null && uri.scheme in listOf("http", "https")) main.post {
                    if (resources.none { it.url == uri.toString() }) {
                        resources.add(0, FoundResource(uri.toString(), type))
                        while (resources.size > 250) resources.removeAt(resources.lastIndex)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                main.post { tab.progress = newProgress / 100f; tab.loading = newProgress < 100 }
            }
            override fun onReceivedTitle(view: WebView, title: String?) {
                main.post { tab.title = title?.takeIf { it.isNotBlank() } ?: "新标签页" }
            }
        }
        web.setDownloadListener { url, userAgent, disposition, mime, _ ->
            try {
                val name = URLUtil.guessFileName(url, disposition, mime)
                val req = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mime)
                    .addRequestHeader("User-Agent", userAgent.orEmpty())
                    .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url).orEmpty())
                    .setTitle(name).setDescription("ResourceBrowser")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, name)
                (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                main.post { panel = "download_ok" }
            } catch (_: Exception) { main.post { panel = "download_fail" } }
        }
        tabs.add(tab)
        activeId = tab.id
        address = start
        web.loadUrl(start)
    }

    fun go(raw: String) {
        val s = raw.trim()
        if (s.isEmpty()) return
        val target = when {
            s.startsWith("https://", true) || s.startsWith("http://", true) -> s
            s.contains(" ") || !s.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(s)
            else -> "https://$s"
        }
        address = target
        active?.view?.loadUrl(target)
    }

    LaunchedEffect(Unit) { if (tabs.isEmpty()) addTab() }
    BackHandler(enabled = active?.view?.canGoBack() == true) { active?.view?.goBack() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(color = Color(0xFFF8F9FA), shadowElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).background(Color(0xFFE9EEF6), RoundedCornerShape(28.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", fontSize = 22.sp, color = Color(0xFF5F6368))
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(
                            value = address, onValueChange = { address = it }, singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF202124), fontSize = 15.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { go(address) }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("×", Modifier.clickable { address = "" }, fontSize = 22.sp, color = Color(0xFF5F6368))
                }
                if (active?.loading == true) {
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp)) {
                        Box(Modifier.fillMaxWidth(active.progress.coerceIn(0f, 1f)).height(2.dp).background(Color(0xFF4285F4)))
                    }
                }
            }
        }
        if (active != null) AndroidView(factory = { active.view }, modifier = Modifier.weight(1f).fillMaxWidth())
        else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("没有打开的标签页") }
        Divider(color = Color(0xFFE5E7EB), thickness = 0.7.dp)
        Row(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFFF8F9FA)).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            Action("‹", "后退", active?.view?.canGoBack() == true) { active?.view?.goBack() }
            Action("›", "前进", active?.view?.canGoForward() == true) { active?.view?.goForward() }
            Action("⌂", "主页") { go("https://www.google.com") }
            Action("▢", tabs.size.toString()) { panel = "tabs" }
            Action("⋮", "菜单") { panel = "menu" }
        }
    }

    when (panel) {
        "tabs" -> AlertDialog(
            onDismissRequest = { panel = "" }, title = { Text("标签页（" + tabs.size + "）") },
            text = {
                LazyColumn {
                    items(tabs, key = { it.id }) { tab ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { activeId = tab.id; address = tab.url; panel = "" }) {
                                Text(tab.title, style = MaterialTheme.typography.bodyLarge)
                                Text(tab.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            Text("×", Modifier.clickable {
                                tab.view.stopLoading(); tab.view.destroy(); tabs.remove(tab)
                                if (activeId == tab.id) {
                                    val next = tabs.lastOrNull()
                                    if (next == null) addTab() else { activeId = next.id; address = next.url }
                                }
                            }.padding(8.dp))
                        }
                        Divider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { panel = ""; addTab() }) { Text("新建标签页") } },
            dismissButton = { TextButton(onClick = { panel = "" }) { Text("关闭") } }
        )
        "menu" -> AlertDialog(
            onDismissRequest = { panel = "" }, title = { Text("浏览器菜单") },
            text = {
                Column {
                    MenuItem("添加当前页书签") {
                        val url = active?.url.orEmpty()
                        if (url.startsWith("http")) {
                            bookmarks = (bookmarks + url).distinct()
                            prefs.edit().putStringSet("bookmarks", bookmarks.toSet()).apply()
                        }
                        panel = ""
                    }
                    MenuItem("书签（" + bookmarks.size + "）") { panel = "bookmarks" }
                    MenuItem("浏览历史") { panel = "history" }
                    MenuItem("发现的媒体资源（" + resources.size + "）") { panel = "resources" }
                    MenuItem(if (privateSession) "退出隐私会话" else "新建隐私会话标签") {
                        privateSession = !privateSession
                        if (privateSession) addTab()
                        panel = ""
                    }
                }
            },
            confirmButton = { TextButton(onClick = { panel = "" }) { Text("完成") } }
        )
        "bookmarks", "history" -> {
            val isBookmarks = panel == "bookmarks"
            val entries = if (isBookmarks) bookmarks else history
            AlertDialog(
                onDismissRequest = { panel = "" }, title = { Text(if (isBookmarks) "书签" else "浏览历史") },
                text = {
                    LazyColumn {
                        items(entries) { url ->
                            Text(url, Modifier.fillMaxWidth().clickable {
                                active?.view?.loadUrl(url); address = url; panel = ""
                            }.padding(vertical = 9.dp), maxLines = 2)
                            Divider()
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { panel = "" }) { Text("关闭") } },
                dismissButton = if (!isBookmarks) ({ TextButton(onClick = {
                    history = emptyList(); prefs.edit().remove("history").apply()
                }) { Text("清空历史") } }) else null
            )
        }
        "resources" -> AlertDialog(
            onDismissRequest = { panel = "" }, title = { Text("发现的资源（" + resources.size + "）") },
            text = {
                LazyColumn {
                    items(resources, key = { it.url }) { item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            active?.view?.loadUrl(item.url); address = item.url; panel = ""
                        }.padding(vertical = 8.dp)) {
                            Text(item.type, color = Color(0xFF1A73E8), style = MaterialTheme.typography.labelMedium)
                            Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        Divider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { panel = "" }) { Text("关闭") } },
            dismissButton = { TextButton(onClick = { resources.clear() }) { Text("清空") } }
        )
        "download_ok" -> AlertDialog(onDismissRequest = { panel = "" }, title = { Text("已加入下载队列") }, confirmButton = { TextButton(onClick = { panel = "" }) { Text("知道了") } })
        "download_fail" -> AlertDialog(onDismissRequest = { panel = "" }, title = { Text("下载失败") }, confirmButton = { TextButton(onClick = { panel = "" }) { Text("关闭") } })
    }
}

@Composable
private fun MenuItem(label: String, action: () -> Unit) {
    Text(label, Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
}
@Composable
private fun Action(symbol: String, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 5.dp, vertical = 3.dp).clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(symbol, fontSize = 22.sp, color = if (enabled) Color(0xFF3C4043) else Color(0xFFBDC1C6))
        Text(label, fontSize = 10.sp, color = Color(0xFF5F6368))
    }
}
