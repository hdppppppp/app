package com.tideengine.mobile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import org.json.JSONObject

/**
 * Kuikly 页面宿主。内置模式下不需要动态下发 bundle，contextCode 传空字符串，
 * 页面由 KSP 生成的 KuiklyCoreEntry 注册。
 */
class TideRenderActivity : AppCompatActivity() {
    private lateinit var container: FrameLayout
    private lateinit var delegator: KuiklyRenderViewBaseDelegator

    private val pageName: String
        get() = intent.getStringExtra(EXTRA_PAGE_NAME).orEmpty().ifBlank { PAGE_LOGIN }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F3F1EB"))
        }
        setContentView(container)

        val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase,
            ) {
                if (!isSucceed) {
                    Log.e(TAG, "Kuikly page load failed: $errorReason mode=${executeMode.mode}")
                }
            }
        }
        delegator = KuiklyRenderViewBaseDelegator(delegate)
        delegator.onAttach(container, "", pageName, readPageData())
    }

    override fun onResume() {
        super.onResume()
        delegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        delegator.onPause()
    }

    override fun onDestroy() {
        if (::delegator.isInitialized) {
            delegator.onDetach()
        }
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::delegator.isInitialized && delegator.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    private fun readPageData(): Map<String, Any> {
        val raw = intent.getStringExtra(EXTRA_PAGE_DATA) ?: return emptyMap()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        val result = mutableMapOf<String, Any>()
        json.keys().forEach { key ->
            result[key] = json.get(key)
        }
        return result
    }

    companion object {
        private const val TAG = "TideRenderActivity"
        private const val PAGE_LOGIN = "login"
        private const val EXTRA_PAGE_NAME = "pageName"
        private const val EXTRA_PAGE_DATA = "pageData"

        fun start(context: Context, pageName: String, pageData: JSONObject) {
            val intent = Intent(context, TideRenderActivity::class.java)
            intent.putExtra(EXTRA_PAGE_NAME, pageName)
            intent.putExtra(EXTRA_PAGE_DATA, pageData.toString())
            context.startActivity(intent)
        }
    }
}
