package com.tideengine.mobile.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.GameAccountBinDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 单账号游戏容器：从服务端读取 Bin 后写入 WebView 的 localStorage，
 * 再加载服务端托管的 /game/game.html。Bin 只保存在内存，不落盘。
 *
 * 多窗口网格与分组管理属于下一阶段，这里先保证单账号可启动、可返回。
 */
class GameManagerActivity : AppCompatActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var webView: WebView
    private lateinit var statusView: TextView
    private lateinit var progressBar: ProgressBar

    private var accountId: String = ""
    private var fileName: String = "account"
    private var binHex: String? = null
    private var injected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID).orEmpty()
        if (accountId.isBlank()) {
            finish()
            return
        }
        setContentView(buildLayout())
        loadBin()
    }

    private fun buildLayout(): ViewGroup {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#131311"))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#1d1d19"))
        }
        toolbar.addView(
            TextView(this).apply {
                text = "游戏多开"
                setTextColor(Color.parseColor("#f1ede3"))
                textSize = 16f
                setOnClickListener { finish() }
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        toolbar.addView(
            TextView(this).apply {
                text = "返回"
                setTextColor(Color.parseColor("#e8875f"))
                textSize = 14f
                setPadding(dp(8), dp(6), dp(8), dp(6))
                setOnClickListener { finish() }
            },
        )

        statusView = TextView(this).apply {
            text = "正在读取账号 Bin…"
            setTextColor(Color.parseColor("#b8b1a3"))
            textSize = 12f
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = android.view.View.GONE
        }

        webView = WebView(this).apply {
            settings.configureWebView()
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    progressBar.visibility = android.view.View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    progressBar.visibility = android.view.View.GONE
                    val hex = binHex
                    if (hex != null && !injected) {
                        val key = "$fileName-bincontent"
                        val script = "localStorage.setItem(${JSONObject.quote(key)}, ${JSONObject.quote(hex)});"
                        view?.evaluateJavascript(script) {
                            injected = true
                            statusView.text = "Bin 已注入，正在重新加载游戏…"
                            view.reload()
                        }
                    } else {
                        statusView.text = "游戏已加载"
                    }
                }
            }
            webChromeClient = WebChromeClient()
        }

        root.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(statusView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(progressBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun loadBin() {
        if (!ServiceLocator.isInitialized) {
            statusView.text = "客户端尚未初始化"
            return
        }
        scope.launch {
            try {
                val bin: GameAccountBinDto = ServiceLocator.gameAccounts.binFile(accountId)
                fileName = bin.fileName
                binHex = bin.hex
                statusView.text = "正在打开游戏…"
                loadGamePage()
            } catch (error: ApiException) {
                statusView.text = "读取 Bin 失败：${error.message}"
            } catch (error: Throwable) {
                statusView.text = "读取 Bin 失败，请稍后重试"
                Log.e(TAG, "loadBin failed", error)
            }
        }
    }

    private fun loadGamePage() {
        val base = ServiceLocator.apiBaseUrl.trimEnd('/')
        val url = "$base/game/game.html?fileName=${encode(fileName)}&fromManager=true&windowId=${encode(accountId)}"
        webView.loadUrl(url)
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    override fun onDestroy() {
        scope.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "GameManagerActivity"
        private const val EXTRA_ACCOUNT_ID = "accountId"

        fun start(context: Context, accountId: String) {
            val intent = Intent(context, GameManagerActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT_ID, accountId)
            context.startActivity(intent)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebSettings.configureWebView() {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    mediaPlaybackRequiresUserGesture = false
    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    cacheMode = WebSettings.LOAD_DEFAULT
    loadWithOverviewMode = true
    useWideViewPort = true
}
