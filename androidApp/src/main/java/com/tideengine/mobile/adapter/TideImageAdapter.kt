package com.tideengine.mobile.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.tencent.kuikly.core.render.android.KuiklyRenderViewContext
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.IKRImageAdapter
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * 头像等图片加载：base64 直接解码，http/file/assets 走轻量下载。
 * 没有引入图片库，避免为一个头像增加体积；失败时回退为 null 由 UI 显示首字母。
 */
class TideImageAdapter(private val context: Context) : IKRImageAdapter {
    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        imageParams: JSONObject?,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        val src = imageLoadOption.src
        when {
            src.startsWith(HRImageLoadOption.SCHEME_BASE64) -> callback(decodeBase64(src))
            src.startsWith(HRImageLoadOption.SCHEME_HTTP) -> executor.execute {
                val drawable = runCatching { download(src) }.getOrNull()
                mainHandler.post { callback(drawable) }
            }
            src.startsWith(HRImageLoadOption.SCHEME_FILE) -> {
                val path = src.removePrefix(HRImageLoadOption.SCHEME_FILE)
                callback(decodeFile(path))
            }
            src.startsWith(HRImageLoadOption.SCHEME_ASSETS) -> {
                val path = src.removePrefix(HRImageLoadOption.SCHEME_ASSETS)
                callback(decodeAsset(path))
            }
            else -> callback(null)
        }
    }

    override fun getDrawableWidth(kuiklyRenderViewContext: KuiklyRenderViewContext, drawable: Drawable): Float =
        drawable.intrinsicWidth.toFloat()

    override fun getDrawableHeight(kuiklyRenderViewContext: KuiklyRenderViewContext, drawable: Drawable): Float =
        drawable.intrinsicHeight.toFloat()

    private fun decodeBase64(src: String): Drawable? {
        val payload = src.substringAfter(',', "")
        if (payload.isBlank()) return null
        val bytes = runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull() ?: return null
        return bitmapDrawable(bytes)
    }

    private fun download(src: String): Drawable? {
        val connection = (URL(src).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 12000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        return try {
            connection.inputStream.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return null
                BitmapDrawable(context.resources, bitmap)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun decodeFile(path: String): Drawable? {
        val file = File(path.removePrefix("//"))
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)?.let { BitmapDrawable(context.resources, it) }
    }

    private fun decodeAsset(path: String): Drawable? = runCatching {
        context.assets.open(path.removePrefix("/")).use { stream ->
            BitmapFactory.decodeStream(stream)
        }?.let { BitmapDrawable(context.resources, it) }
    }.getOrNull()

    private fun bitmapDrawable(bytes: ByteArray): Drawable? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }
}
