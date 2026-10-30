package com.tideengine.mobile.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tideengine.mobile.api.dto.PublicUser

object AppSession {
    var user by mutableStateOf<PublicUser?>(null)

    fun clear() {
        user = null
    }
}
