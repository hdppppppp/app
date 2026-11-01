package com.tideengine.mobile.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tideengine.mobile.ServiceLocator
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 服务端返回站内相对地址（`/api/avatars/...`），展示前补全为服务端根地址。 */
fun resolveAvatarUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val root = if (ServiceLocator.isInitialized) ServiceLocator.apiBaseUrl.trimEnd('/') else ""
    return buildString {
        append(root)
        if (!trimmed.startsWith('/')) append('/')
        append(trimmed)
    }
}

private sealed interface AvatarUiState {
    data object Failure : AvatarUiState
    data object Loading : AvatarUiState
    data class Success(val bitmap: ImageBitmap) : AvatarUiState
}

/** 头像体积小（≤512KB），只做进程内缓存；URL 每次上传都带随机后缀，无需持久化。 */
private object AvatarBitmapCache {
    private const val MAX_BYTES = 8 * 1024 * 1024
    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
    }

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap.asImageBitmap())
    }
}

/**
 * 用户头像：加载服务端图片，完成前显示进度，失败或未设置时回退到传入的首字母占位；
 * 加载结果按绝对 URL 缓存，同一头像不会重复请求。
 */
@Composable
fun AvatarImage(
    url: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable BoxScope.() -> Unit,
) {
    val state = produceState<AvatarUiState>(
        initialValue = if (url.isNullOrBlank()) AvatarUiState.Failure else AvatarUiState.Loading,
        url,
    ) {
        runCatching {
            if (url.isNullOrBlank()) {
                value = AvatarUiState.Failure
                return@runCatching
            }
            if (!ServiceLocator.isInitialized) {
                value = AvatarUiState.Failure
                return@runCatching
            }
            val absolute = resolveAvatarUrl(url)
            value = AvatarUiState.Loading
            AvatarBitmapCache.get(absolute)?.let { cached ->
                value = AvatarUiState.Success(cached)
                return@runCatching
            }
            val bytes = withContext(Dispatchers.IO) { ServiceLocator.api.fetchBytes(absolute) }
            val bitmap = bytes?.let { withContext(Dispatchers.Default) { decode(it) } }
            value = if (bitmap != null) {
                AvatarBitmapCache.put(absolute, bitmap)
                AvatarUiState.Success(bitmap)
            } else {
                AvatarUiState.Failure
            }
        }.onFailure {
            value = AvatarUiState.Failure
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state.value) {
            AvatarUiState.Failure -> fallback()
            AvatarUiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(16.dp))
            is AvatarUiState.Success -> Image(
                bitmap = current.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun decode(bytes: ByteArray): Bitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
