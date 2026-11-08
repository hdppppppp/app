package com.tideengine.mobile

import android.app.Application
import com.taotao.music.update.HotUpdate
import com.taotao.music.update.HotUpdateSession
import kotlinx.coroutines.launch

class TideApplication : Application() {

    lateinit var hotUpdate: HotUpdateSession
        private set

    /** 本次启动生效的热修复补丁版本，0 表示没有。 */
    var activePatchVersion: Int = 0
        private set

    override fun onCreate() {
        super.onCreate()
        MobileBootstrap.install(this, BuildConfig.API_BASE_URL)
        // 热更新接入（上游 README 的三步纪律）：
        // ① 尽早加载已安装补丁，晚于第一次业务调用就白打了；
        // ② 创建管理器并把 ① 的返回值传给 initialPatchVersion，避免启动时
        //    已加载的补丁被误判成「有新补丁」再下载一遍；
        // ③ 启动后检查一次更新，静默失败不打扰用户。
        activePatchVersion = HotUpdate.installPatches(this)
        hotUpdate = HotUpdate.create(this, TideUpdateHost(this, BuildConfig.API_BASE_URL), initialPatchVersion = activePatchVersion)
        hotUpdate.launchCheck()
    }
}

/**
 * HotUpdateSession 的界面动作扩展：check / download / retry 是 suspend 函数，
 * UI 回调（onClick / onXxx lambda）不是协程上下文。Session 的 scope 是 private，
 * 这里自建主线程作用域 launch —— 与模块内部默认作用域（Main.immediate）一致，
 * UpdateManager 内部的互斥锁保证同一时刻只有一次检查/下载，不会因多次 launch 重复执行。
 */
fun HotUpdateSession.launchDownload() = launchUiAction { updateManager.download() }

fun HotUpdateSession.launchRetry() = launchUiAction { updateManager.retry() }

private fun HotUpdateSession.launchUiAction(block: suspend () -> Unit) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch { block() }
}
