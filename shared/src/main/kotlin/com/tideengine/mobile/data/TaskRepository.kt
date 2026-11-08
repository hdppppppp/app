package com.tideengine.mobile.data

import com.tideengine.mobile.api.ApiClient
import com.tideengine.mobile.api.dto.Paged
import com.tideengine.mobile.api.dto.SuccessResponse
import com.tideengine.mobile.api.dto.TaskDto
import com.tideengine.mobile.api.dto.TaskExecutionDto
import com.tideengine.mobile.api.dto.TaskHandlersResponse

class TaskRepository(private val api: ApiClient) {
    suspend fun list(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        enabled: Boolean? = null,
        handlerType: String? = null,
    ): Paged<TaskDto> = api.get(
        "/api/tasks",
        buildMap {
            put("page", page.toString())
            put("pageSize", pageSize.toString())
            search?.takeIf { it.isNotBlank() }?.let { put("search", it) }
            enabled?.let { put("enabled", it.toString()) }
            handlerType?.takeIf { it.isNotBlank() }?.let { put("handlerType", it) }
        },
    )

    suspend fun handlers(): TaskHandlersResponse = api.get("/api/tasks/handlers")

    suspend fun detail(id: String): TaskDto = api.get("/api/tasks/$id")

    suspend fun setEnabled(id: String, enabled: Boolean): TaskDto =
        api.post(if (enabled) "/api/tasks/$id/enable" else "/api/tasks/$id/disable")

    suspend fun run(id: String): TaskExecutionDto? = api.post("/api/tasks/$id/run")

    suspend fun executions(
        id: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): Paged<TaskExecutionDto> = api.get(
        "/api/tasks/$id/executions",
        mapOf("page" to page.toString(), "pageSize" to pageSize.toString()),
    )

    suspend fun remove(id: String): SuccessResponse = api.delete("/api/tasks/$id")
}
