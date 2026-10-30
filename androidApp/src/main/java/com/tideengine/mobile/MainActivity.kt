package com.tideengine.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tideengine.mobile.bridge.GameManagerActivity
import com.tideengine.mobile.ui.TideApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TideApp(
                onOpenManager = { accountId ->
                    GameManagerActivity.start(this, accountId)
                },
            )
        }
    }
}
