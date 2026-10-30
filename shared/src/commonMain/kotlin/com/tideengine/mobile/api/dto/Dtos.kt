package com.tideengine.mobile.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class Paged<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
)

@Serializable
data class SuccessResponse(val success: Boolean = false)

@Serializable
enum class UserRole {
    @SerialName("admin")
    Admin,

    @SerialName("editor")
    Editor,

    @SerialName("viewer")
    Viewer,
}

@Serializable
enum class UserStatus {
    @SerialName("active")
    Active,

    @SerialName("disabled")
    Disabled,
}

@Serializable
enum class TaskScheduleType {
    @SerialName("once")
    Once,

    @SerialName("daily")
    Daily,

    @SerialName("weekly")
    Weekly,

    @SerialName("monthly")
    Monthly,

    @SerialName("interval")
    Interval,
}

@Serializable
enum class TaskExecutionStatus {
    @SerialName("running")
    Running,

    @SerialName("succeeded")
    Succeeded,

    @SerialName("failed")
    Failed,

    @SerialName("skipped")
    Skipped,
}

@Serializable
enum class OperationLogStatus {
    @SerialName("succeeded")
    Succeeded,

    @SerialName("failed")
    Failed,

    @SerialName("skipped")
    Skipped,
}

@Serializable
data class GameAccountDto(
    val id: String,
    val serverName: String,
    val serverId: Int,
    val originalServerId: String,
    val roleId: String,
    val nickname: String,
    val headImg: String? = null,
    val authMethod: String,
    val isPrimary: Boolean = false,
    val verifiedAt: String? = null,
    val boundAt: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class GameAccountBinDto(
    val fileName: String,
    val hex: String,
)

@Serializable
data class PublicUser(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val giteaId: String? = null,
    val giteaUsername: String? = null,
    val role: UserRole,
    val status: UserStatus,
    val gameServer: String? = null,
    val gameServerId: Int? = null,
    val gameAccountId: String? = null,
    val gameNickname: String? = null,
    val gameAuthMethod: String? = null,
    val gameVerifiedAt: String? = null,
    val gameBoundAt: String? = null,
    val lastLoginAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val gameAccounts: List<GameAccountDto>? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val user: PublicUser,
)

@Serializable
data class LoginRequest(
    val identifier: String,
    val password: String,
    val rememberMe: Boolean = false,
)

@Serializable
data class AuthOptions(
    val allowRegistration: Boolean = false,
    val defaultRole: UserRole = UserRole.Viewer,
    val giteaEnabled: Boolean = false,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class TaskDto(
    val id: String,
    val userId: String,
    val gameAccountId: String? = null,
    val name: String,
    val description: String = "",
    val scheduleType: TaskScheduleType,
    val schedule: JsonObject = JsonObject(emptyMap()),
    val handlerType: String,
    val handlerParams: JsonObject = JsonObject(emptyMap()),
    val source: String = "manual",
    val enabled: Boolean = false,
    val nextRunAt: String? = null,
    val lastRunAt: String? = null,
    val rateLimit: JsonObject? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TaskExecutionDto(
    val id: String,
    val taskId: String,
    val status: TaskExecutionStatus,
    val startedAt: String,
    val finishedAt: String? = null,
    val durationMs: Int? = null,
    val result: JsonElement? = null,
    val error: String? = null,
    val triggeredBy: String = "system",
    val createdAt: String,
)

@Serializable
data class TaskHandlersResponse(
    val handlers: List<String> = emptyList(),
)

@Serializable
data class TaskSwitchLastExecution(
    val status: TaskExecutionStatus,
    val message: String? = null,
    val finishedAt: String? = null,
)

@Serializable
data class TaskSwitchDto(
    val gameAccountId: String,
    val handlerType: String,
    val label: String,
    val description: String = "",
    val enabled: Boolean = false,
    val taskId: String? = null,
    val scheduleType: TaskScheduleType,
    val schedule: JsonObject = JsonObject(emptyMap()),
    val handlerParams: JsonObject = JsonObject(emptyMap()),
    val nextRunAt: String? = null,
    val lastRunAt: String? = null,
    val lastExecution: TaskSwitchLastExecution? = null,
)

@Serializable
data class OperationLogDto(
    val id: String,
    val userId: String,
    val taskId: String? = null,
    val taskName: String = "",
    val executionId: String? = null,
    val gameAccountId: String? = null,
    val roleId: String? = null,
    val nickname: String? = null,
    val serverName: String? = null,
    val handlerType: String = "",
    val action: String = "",
    val status: OperationLogStatus,
    val params: JsonElement? = null,
    val result: JsonElement? = null,
    val error: String? = null,
    val durationMs: Int? = null,
    val triggeredBy: String = "system",
    val createdAt: String,
)

@Serializable
data class LoginSessionDto(
    val id: String,
    val deviceName: String,
    val browser: String,
    val os: String,
    val deviceType: String,
    val ip: String,
    val createdAt: String,
    val lastUsedAt: String,
    val expiresAt: String,
    val revokedAt: String? = null,
    val isCurrent: Boolean = false,
)
