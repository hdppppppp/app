package com.tideengine.mobile

import android.app.Application
import com.taotao.music.update.HotUpdate
import com.taotao.music.update.HotUpdateSession

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
