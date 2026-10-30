package com.tideengine.mobile.adapter

import android.util.Log
import com.tencent.kuikly.core.render.android.adapter.IKRUncaughtExceptionHandlerAdapter

object TideExceptionAdapter : IKRUncaughtExceptionHandlerAdapter {
    private const val TAG = "TideKuiklyException"

    override fun uncaughtException(throwable: Throwable) {
        Log.e(TAG, throwable.stackTraceToString())
    }
}
