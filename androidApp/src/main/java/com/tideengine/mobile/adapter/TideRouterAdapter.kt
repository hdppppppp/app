package com.tideengine.mobile.adapter

import android.app.Activity
import android.content.Context
import com.tencent.kuikly.core.render.android.adapter.IKRRouterAdapter
import com.tideengine.mobile.TideRenderActivity
import com.tideengine.mobile.bridge.GameManagerActivity
import org.json.JSONObject

class TideRouterAdapter : IKRRouterAdapter {
    override fun openPage(context: Context, pageName: String, pageData: JSONObject) {
        if (pageName == PAGE_GAME_MANAGER) {
            val accountId = pageData.optString("accountId")
            if (accountId.isNotBlank()) {
                GameManagerActivity.start(context, accountId)
            }
            return
        }
        TideRenderActivity.start(context, pageName, pageData)
    }

    override fun closePage(context: Context) {
        (context as? Activity)?.finish()
    }

    private companion object {
        const val PAGE_GAME_MANAGER = "gameManager"
    }
}
