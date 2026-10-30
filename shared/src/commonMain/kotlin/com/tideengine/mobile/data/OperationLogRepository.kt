package com.tideengine.mobile.data

import com.tideengine.mobile.api.ApiClient
import com.tideengine.mobile.api.dto.OperationLogDto
import com.tideengine.mobile.api.dto.OperationLogStatus
import com.tideengine.mobile.api.dto.Paged

class OperationLogRepository(private val api: ApiClient) {
    suspend fun list(
        page: Int = 1,
        pageSize: Int = 30,
        search: String? = null,
        status: OperationLogStatus? = null,
        handlerType: String? = null,
        gameAccountId: String? = null,
    ): Paged<OperationLogDto> = api.get(
        "/api/operation-logs",
        buildMap {
            put("page", page.toString())
            put("pageSize", pageSize.toString())
            search?.takeIf { it.isNotBlank() }?.let { put("search", it) }
            status?.let { put("status", it.wireValue) }
            handlerType?.takeIf { it.isNotBlank() }?.let { put("handlerType", it) }
            gameAccountId?.takeIf { it.isNotBlank() }?.let { put("gameAccountId", it) }
        },
    )
}

private val OperationLogStatus.wireValue: String
    get() = when (this) {
        OperationLogStatus.Succeeded -> "succeeded"
        OperationLogStatus.Failed -> "failed"
        OperationLogStatus.Skipped -> "skipped"
    }
