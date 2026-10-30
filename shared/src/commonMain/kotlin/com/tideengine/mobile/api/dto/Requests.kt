package com.tideengine.mobile.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val displayName: String,
    val password: String,
    val emailCode: String,
)

@Serializable
data class EmailCodeRequest(val email: String)

@Serializable
data class EmailCodeResponse(
    val success: Boolean = false,
    val expiresIn: Int = 0,
    val cooldown: Int = 0,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val email: String? = null,
)

@Serializable
data class TaskUpsertRequest(
    val name: String,
    val description: String? = null,
    val scheduleType: TaskScheduleType,
    val schedule: JsonObject = JsonObject(emptyMap()),
    val handlerType: String,
    val handlerParams: JsonObject = JsonObject(emptyMap()),
    val gameAccountId: String? = null,
    val enabled: Boolean? = null,
)

@Serializable
data class UpdateTaskSwitchRequest(
    val scheduleType: TaskScheduleType? = null,
    val schedule: JsonObject? = null,
    val handlerParams: JsonObject? = null,
)
