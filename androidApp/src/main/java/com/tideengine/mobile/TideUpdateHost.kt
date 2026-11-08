package com.tideengine.mobile

import android.content.Context
import com.taotao.music.update.UpdateConfigStore
import com.taotao.music.update.UpdateHost
import com.tideengine.mobile.auth.TokenStorage

/**
 * 热更新模块（:hotupdate）的宿主契约实现。
 *
 * 依赖方向保持 app -> 模块：模块只认 UpdateHost 接口，这里把登录令牌、设备号、
 * 远程配置缓存与「划掉更新」记录从本 App 的存储注入进去。令牌走 TokenStorage
 * 的当前值，不触发续期 —— 检查更新是后台动作，令牌过期时服务端会退回按设备号分桶，
 * 不能因为更新检查把用户踢回登录页。
 *
 * 服务端地址复用 API_BASE_URL：热更新接口与业务 API 同源（/api/v1/app 路径），
 * 内网部署改一个属性就能同时切换两者。
 */
class TideUpdateHost(context: Context, private val apiBaseUrl: String) : UpdateHost {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 热更新检查地址与业务 API 同源；baseUrl 去掉尾斜杠后直接拼 /api/v1/app 路径。
    override val endpoint: String = apiBaseUrl.trimEnd('/')

    override val userAgent: String = "TideEngine-Android/${BuildConfig.VERSION_NAME}"

    override val config: UpdateConfigStore = SharedPreferencesConfigStore(appContext)

    override fun currentToken(): String? = TokenStorage.load()

    override fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        // 首次生成后必须稳定：灰度分桶按设备号哈希，翻转会导致更新提示时有时无。
        val generated = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    override fun dismissedVersionCode(): Long = prefs.getLong(KEY_DISMISSED, 0L)

    override fun commitDismissed(versionCode: Long) {
        prefs.edit().putLong(KEY_DISMISSED, versionCode).apply()
    }

    companion object {
        private const val PREFS_NAME = "tide_update"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DISMISSED = "dismissed_version_code"
    }
}

/**
 * 远程配置的最小落盘实现：SharedPreferences 键值对 + configVersion。
 *
 * 读取在首帧与离线时都会发生，必须同步可查；所有读取都带调用方默认值兜底，
 * 缓存为空时不影响界面。
 */
private class SharedPreferencesConfigStore(context: Context) : UpdateConfigStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("tide_remote_config", Context.MODE_PRIVATE)

    override fun save(values: Map<String, String>, version: Long) {
        if (version != 0L && version == prefs.getLong(KEY_VERSION, 0L)) return
        prefs.edit()
            .clear()
            .putLong(KEY_VERSION, version)
            .apply()
        // clear() 与写入分两次提交：同一 editor 里 clear 后再 put 会按序执行，
        // 但分开写能保证异常中断时至少不留半套新配置。
        prefs.edit().apply {
            for ((key, value) in values) putString(key, value)
        }.apply()
    }

    override fun text(key: String, default: String): String =
        prefs.getString(key, null) ?: default

    override fun boolean(key: String, default: Boolean): Boolean =
        if (prefs.contains(key)) prefs.getBoolean(key, default) else default

    override fun int(key: String, default: Int): Int =
        if (prefs.contains(key)) prefs.getInt(key, default) else default

    override fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_VERSION = "config_version"
    }
}
