package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.ListsModels
import org.oremif.deepseek.utils.validateResponse

/**
 * Retrieves the list of DeepSeek models available for the current API key.
 *
 * Example:
 * ```kotlin
 * val modelsList = client.models()
 * modelsList.data.forEach { model ->
 *     println("${model.id} (owned by ${model.ownedBy})")
 * }
 * ```
 *
 * @return A [ListsModels] object containing information about available models
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClientBase.models(): ListsModels {
    val response = client.get("models")
    validateResponse(response)
    return response.body()
}
