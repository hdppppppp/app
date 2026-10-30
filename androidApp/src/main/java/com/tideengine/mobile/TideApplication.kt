package com.tideengine.mobile

import android.app.Application

class TideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileBootstrap.install(this, BuildConfig.API_BASE_URL)
    }
}
