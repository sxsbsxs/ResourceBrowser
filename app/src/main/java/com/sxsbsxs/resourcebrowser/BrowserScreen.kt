package com.sxsbsxs.resourcebrowser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen() {
    val context = LocalContext.current
    var address by remember { mutableStateOf("https://www.google.com") }
    var progress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("新标签页") }

    val browserView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setSupportMultipleWindows(false)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    loading = true
                    progress = 0f
                    if (!url.isNullOrBlank()) address = url
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading = false
                    progress = 1f
                    pageTitle = view.title?.takeIf { it.isNotBlank() } ?: "新标签页"
                    if (!url.isNullOrBlank()) address = url
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return request.url.scheme !in listOf("http", "https")
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress / 100f
                    loading = newProgress < 100
                }

                override fun onReceivedTitle(view: WebView, title: String?) {
                    pageTitle = title?.takeIf { it.isNotBlank() } ?: "新标签页"
                }
            }
        }
    }

    LaunchedEffect(browserView) {
        webView = browserView
        if (browserView.url == null) browserView.loadUrl("https://www.google.com")
    }
    DisposableEffect(browserView) {
        onDispose { browserView.stopLoading() }
    }

    fun navigate(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val target = when {
            input.startsWith("https://", true) || input.startsWith("http://", true) -> input
            input.contains(" ") || !input.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(input)
            else -> "https://$input"
        }
        address = target
        browserView.loadUrl(target)
    }

    BackHandler(enabled = browserView.canGoBack()) { browserView.goBack() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(color = Color(0xFFF8F9FA), shadowElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .background(Color(0xFFE9EEF6), RoundedCornerShape(28.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", fontSize = 22.sp, color = Color(0xFF5F6368))
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                        if (address.isEmpty()) Text("搜索或输入网址", color = Color(0xFF5F6368), fontSize = 15.sp)
                        BasicTextField(
                            value = address,
                            onValueChange = { address = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF202124), fontSize = 15.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { navigate(address) }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (loading) {
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp)) {
                        Box(
                            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp)
                                .background(Color(0xFF4285F4))
                        )
                    }
                }
            }
        }

        AndroidView(
            factory = { browserView },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            update = { if (it !== webView) webView = it }
        )

        Divider(color = Color(0xFFE5E7EB), thickness = 0.7.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).background(Color(0xFFF8F9FA))
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserAction("‹", "后退", enabled = browserView.canGoBack()) { if (browserView.canGoBack()) browserView.goBack() }
            BrowserAction("›", "前进", enabled = browserView.canGoForward()) { if (browserView.canGoForward()) browserView.goForward() }
            BrowserAction("⌂", "主页") { browserView.loadUrl("https://www.google.com"); address = "https://www.google.com" }
            BrowserAction("▢", "标签页") { pageTitle = "标签页功能即将接入" }
            BrowserAction("⋮", "菜单") { pageTitle = "菜单功能即将接入" }
        }
    }
}

@Composable
private fun BrowserAction(symbol: String, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(symbol, fontSize = 22.sp, color = if (enabled) Color(0xFF3C4043) else Color(0xFFBDC1C6))
        Text(label, fontSize = 10.sp, color = Color(0xFF5F6368))
    }
}
