package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.models.ListsModels
import org.oremif.deepseek.utils.validateResponse

/**
 * Retrieves the list of available DeepSeek AI models.
 *
 * This function fetches information about all models accessible with your current API credentials,
 * including model names, capabilities, and version details.
 *
 * Example:
 * ```kotlin
 * // Get available models
 * val modelsList = client.models()
 *
 * // Display model information
 * modelsList.data.forEach { model ->
 *     println("Model: ${model.id}")
 *     println("Owner: ${model.ownedBy}")
 *     println("Created: ${model.created}")
 * }
 * ```
 *
 * @return A [ListsModels] object containing information about available models
 */
public suspend fun DeepSeekClientBase.models(): ListsModels {
    val response = client.get("models")
    validateResponse(response)
    return response.body()
}
