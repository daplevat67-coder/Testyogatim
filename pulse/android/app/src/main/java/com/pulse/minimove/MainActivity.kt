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
import org.json.JSONObject

/** Offline Android host. Camera exercise tracking runs in separate on-device activity. */
class MainActivity : Activity() {
    private lateinit var browser: WebView
    private var counterTask: String? = null
    private val counterRequestCode = 701

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
                override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest): Boolean {
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != counterRequestCode) return
        val expected = counterTask
        counterTask = null
        if(resultCode!=RESULT_OK || data==null || expected==null)return
        val exercise = data.getStringExtra(PoseCounterActivity.EXERCISE)
        if(exercise!=expected)return
        val count = data.getIntExtra(PoseCounterActivity.RESULT_COUNT,0).coerceIn(0,100)
        browser.evaluateJavascript("window.pulseCounterResult && window.pulseCounterResult(${JSONObject.quote(exercise)},$count)",null)
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

    class Bridge(private val activity: MainActivity) {
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

        /** Accept only fixed internal exercise IDs; never pass arbitrary links or commands. */
        @JavascriptInterface
        fun startCounter(id: String, name: String, goal: Int, initial: Int) {
            if(id !in PoseCounterActivity.supported) return
            if(goal !in 1..100 || initial !in 0..100) return
            activity.runOnUiThread {
                if(activity.counterTask!=null)return@runOnUiThread
                activity.counterTask=id
                val counter=Intent(activity,PoseCounterActivity::class.java).apply{
                    putExtra(PoseCounterActivity.EXERCISE,id)
                    putExtra(PoseCounterActivity.NAME,name.take(70))
                    putExtra(PoseCounterActivity.GOAL,goal)
                    putExtra(PoseCounterActivity.INITIAL,initial)
                }
                @Suppress("DEPRECATION")
                activity.startActivityForResult(counter,activity.counterRequestCode)
            }
        }
    }
}
