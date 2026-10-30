package com.tideengine.mobile.data

import com.tideengine.mobile.api.ApiClient
import com.tideengine.mobile.api.dto.GameAccountBinDto
import com.tideengine.mobile.api.dto.GameAccountDto
import com.tideengine.mobile.api.dto.SuccessResponse
import com.tideengine.mobile.api.dto.TaskSwitchDto
import com.tideengine.mobile.api.dto.UpdateTaskSwitchRequest

class GameAccountRepository(private val api: ApiClient) {
    suspend fun list(): List<GameAccountDto> = api.get("/api/game-accounts")

    suspend fun binFile(id: String): GameAccountBinDto =
        api.get("/api/game-accounts/$id/bin")

    suspend fun setPrimary(id: String): GameAccountDto =
        api.post("/api/game-accounts/$id/primary")

    suspend fun remove(id: String): SuccessResponse =
        api.delete("/api/game-accounts/$id")

    suspend fun listSwitches(): List<TaskSwitchDto> = api.get("/api/task-switches")

    suspend fun switches(gameAccountId: String): List<TaskSwitchDto> =
        api.get("/api/game-accounts/$gameAccountId/task-switches")

    suspend fun enableSwitch(gameAccountId: String, handlerType: String): TaskSwitchDto =
        api.post("/api/game-accounts/$gameAccountId/task-switches/$handlerType/enable")

    suspend fun disableSwitch(gameAccountId: String, handlerType: String): TaskSwitchDto =
        api.post("/api/game-accounts/$gameAccountId/task-switches/$handlerType/disable")

    suspend fun updateSwitch(
        gameAccountId: String,
        handlerType: String,
        request: UpdateTaskSwitchRequest,
    ): TaskSwitchDto =
        api.patchJson("/api/game-accounts/$gameAccountId/task-switches/$handlerType", request)
}
