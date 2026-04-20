package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.FIMCompletion
import org.oremif.deepseek.models.FIMCompletionParams
import org.oremif.deepseek.models.FIMCompletionRequest
import org.oremif.deepseek.utils.validateResponse

/**
 * Sends a Fill-In-the-Middle (FIM) completion request to the DeepSeek API.
 *
 * This function handles the low-level API communication for FIM completions,
 * which allow the model to complete text given a prompt (and optionally a suffix).
 *
 * Example:
 * ```kotlin
 * val params = fimCompletionParams { suffix = "# End of function" }
 * val request = params.createRequest("def calculate_area(radius):")
 * val completion = client.fimCompletion(request)
 * println(completion.choices.first().text)
 * ```
 *
 * @param request The FIM completion request containing all parameters
 * @return A [FIMCompletion] containing the model's response
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClientBase.fimCompletion(request: FIMCompletionRequest): FIMCompletion {
    val response = client.post("beta/completions") {
        setBody(request)
        timeout {
            requestTimeoutMillis = config.fimCompletionTimeout
        }
    }
    validateResponse(response)
    return response.body()
}

/**
 * Generates a completion using the Fill-In-the-Middle capability with custom parameters.
 *
 * FIM allows the model to complete text given a prompt and optional parameters like
 * suffix text, temperature, and other generation controls.
 *
 * Example:
 * ```kotlin
 * val params = fimCompletionParams {
 *     temperature = 0.7
 *     maxTokens = 200
 *     suffix = "}"  // The code should end with a closing bracket
 * }
 *
 * val completion = client.fim(params, "function calculateArea(radius) {")
 * println(completion.choices.first().text)
 * ```
 *
 * @param params Parameters controlling the completion behavior
 * @param prompt The text to start completion from
 * @return A [FIMCompletion] containing the model's response
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.fim(params: FIMCompletionParams, prompt: String): FIMCompletion {
    val request = (if (params.stream == true) params.copy(stream = false) else params).createRequest(prompt)
    return fimCompletion(request)
}

/**
 * Generates a completion using the Fill-In-the-Middle capability with default parameters.
 *
 * This is the simplest way to use FIM completions when you only need to specify
 * the starting text.
 *
 * Example:
 * ```kotlin
 * val completion = client.fim("def sort_array(arr):")
 * println(completion.choices.first().text)
 * ```
 *
 * @param prompt The text to start completion from
 * @return A [FIMCompletion] containing the model's response
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.fim(prompt: String): FIMCompletion =
    fim(FIMCompletionParams(), prompt)

/**
 * Creates a fully customizable FIM completion request using a builder pattern.
 *
 * This approach gives you complete control over all aspects of the FIM request,
 * including prompt, suffix, and generation parameters.
 *
 * Example:
 * ```kotlin
 * val completion = client.fimCompletion {
 *     prompt("public class Calculator {")
 *     params {
 *         suffix = "}"
 *         temperature = 0.8
 *         maxTokens = 500
 *     }
 * }
 * println(completion.choices.first().text)
 * ```
 *
 * @param block A builder block for constructing the complete request
 * @return A [FIMCompletion] containing the model's response
 * @throws DeepSeekException if the API returns a non-2xx status
 */
public suspend fun DeepSeekClient.fimCompletion(block: FIMCompletionRequest.Builder.() -> Unit): FIMCompletion {
    val request = FIMCompletionRequest.Builder().apply(block).build()
    return fimCompletion(request)
}