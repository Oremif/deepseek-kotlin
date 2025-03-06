package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.models.ListsModels
import org.oremif.deepseek.utils.validateResponse

public suspend fun DeepSeekClientBase.models(): ListsModels {
    val response = client.get("models")
    validateResponse(response)
    return response.body()
}
