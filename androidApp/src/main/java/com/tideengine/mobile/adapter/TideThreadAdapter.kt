package com.tideengine.mobile.adapter

import com.tencent.kuikly.core.render.android.adapter.IKRThreadAdapter
import java.util.concurrent.Executors

class TideThreadAdapter : IKRThreadAdapter {
    override fun executeOnSubThread(task: () -> Unit) {
        executor.execute(task)
    }

    private companion object {
        val executor = Executors.newFixedThreadPool(2)
    }
}
