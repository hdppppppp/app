package com.tideengine.mobile.ui.components

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/** 头像处理失败统一走该异常，message 直接用于界面提示，保持与 Web 端一致的中文文案。 */
class AvatarProcessException(message: String) : Exception(message)

private const val AVATAR_OUTPUT_SIZE = 256
private const val AVATAR_JPEG_QUALITY = 86
private const val AVATAR_DATA_URL_MAX_CHARS = 700_000
private const val AVATAR_MAX_SOURCE_BYTES = 8L * 1024 * 1024

/**
 * 与 Web 端 avatar-image.ts 等价：居中裁切为正方形，白底避免透明 PNG 转 JPEG 出现黑边，
 * 再压缩为 256×256 JPEG 并编码成 data URL 提交。
 */
suspend fun createAvatarDataUrl(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    checkSourceSize(resolver, uri)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw AvatarProcessException("图片无法解析，请更换后重试")
    }

    val source = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    } ?: throw AvatarProcessException("图片无法解析，请更换后重试")

    try {
        val output = drawSquare(source)
        try {
            val stream = ByteArrayOutputStream()
            output.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, stream)
            val dataUrl = "data:image/jpeg;base64," +
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            if (dataUrl.length > AVATAR_DATA_URL_MAX_CHARS) {
                throw AvatarProcessException("头像压缩后仍超出大小限制，请更换图片重试")
            }
            dataUrl
        } finally {
            output.recycle()
        }
    } finally {
        source.recycle()
    }
}

/** 超大原图按需降采样解码，避免把几百 MB 的照片整张载入内存。 */
private fun sampleSize(width: Int, height: Int): Int {
    val maxSide = maxOf(width, height)
    var sample = 1
    while (maxSide / (sample * 2) >= AVATAR_OUTPUT_SIZE) {
        sample *= 2
    }
    return sample
}

private fun checkSourceSize(resolver: ContentResolver, uri: Uri) {
    resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && !cursor.isNull(index)) {
                if (cursor.getLong(index) > AVATAR_MAX_SOURCE_BYTES) {
                    throw AvatarProcessException("头像原图不能超过 8MB，请压缩后重试")
                }
            }
        }
    }
}

private fun drawSquare(source: Bitmap): Bitmap {
    val side = min(source.width, source.height)
    if (side <= 0) {
        throw AvatarProcessException("图片尺寸无效，请更换后重试")
    }
    val srcRect = Rect(
        (source.width - side) / 2,
        (source.height - side) / 2,
        (source.width - side) / 2 + side,
        (source.height - side) / 2 + side,
    )
    val output = Bitmap.createBitmap(AVATAR_OUTPUT_SIZE, AVATAR_OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(
        source,
        srcRect,
        RectF(0f, 0f, AVATAR_OUTPUT_SIZE.toFloat(), AVATAR_OUTPUT_SIZE.toFloat()),
        paint,
    )
    return output
}
