package com.pulse.minimove

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/** Offline Android host for the bundled PULSE app. No Internet permission. */
class MainActivity : Activity() {
    private lateinit var browser: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browser = WebView(this).apply {
            setBackgroundColor(Color.rgb(11, 14, 19))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url.toString()
                    return !url.startsWith("file:///android_asset/") && url != "about:blank"
                }
            }
            addJavascriptInterface(Bridge(this@MainActivity), "NativeBridge")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(browser)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (browser.canGoBack()) browser.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        browser.removeJavascriptInterface("NativeBridge")
        browser.destroy()
        super.onDestroy()
    }

    class Bridge(private val activity: Activity) {
        @JavascriptInterface
        fun shareData(json: String) {
            if (json.length > 3_000_000) return
            activity.runOnUiThread {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                activity.startActivity(Intent.createChooser(intent, "Экспорт истории ПУЛЬС"))
            }
        }
    }
}
