package com.tideengine.mobile

import android.app.Application
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tideengine.mobile.adapter.TideExceptionAdapter
import com.tideengine.mobile.adapter.TideImageAdapter
import com.tideengine.mobile.adapter.TideLogAdapter
import com.tideengine.mobile.adapter.TideRouterAdapter
import com.tideengine.mobile.adapter.TideThreadAdapter

class TideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileBootstrap.install(this, BuildConfig.API_BASE_URL)
        installKuiklyAdapters()
    }

    private fun installKuiklyAdapters() {
        if (KuiklyRenderAdapterManager.krImageAdapter == null) {
            KuiklyRenderAdapterManager.krImageAdapter = TideImageAdapter(this)
        }
        if (KuiklyRenderAdapterManager.krLogAdapter == null) {
            KuiklyRenderAdapterManager.krLogAdapter = TideLogAdapter
        }
        if (KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter == null) {
            KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter = TideExceptionAdapter
        }
        if (KuiklyRenderAdapterManager.krRouterAdapter == null) {
            KuiklyRenderAdapterManager.krRouterAdapter = TideRouterAdapter()
        }
        if (KuiklyRenderAdapterManager.krThreadAdapter == null) {
            KuiklyRenderAdapterManager.krThreadAdapter = TideThreadAdapter()
        }
    }
}
