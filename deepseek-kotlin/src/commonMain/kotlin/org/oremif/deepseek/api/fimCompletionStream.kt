package org.oremif.deepseek.api

import io.ktor.client.plugins.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.errors.DeepSeekError
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.errors.toDeepSeekHeaders
import org.oremif.deepseek.models.FIMCompletion
import org.oremif.deepseek.models.FIMCompletionParams
import org.oremif.deepseek.models.FIMCompletionRequest

/**
 * Streams Fill-In-the-Middle (FIM) completions chunk by chunk from the DeepSeek API.
 *
 * This function handles the low-level streaming communication with the API, allowing
 * you to receive and process completions in real-time as they're generated.
 *
 * Example:
 * ```kotlin
 * val params = fimCompletionStreamParams { suffix = "}" }
 * val request = params.createRequest("function calculate(x, y) {")
 *
 * client.fimCompletionStream(request).collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.text ?: "")
 * }
 * ```
 *
 * @param request The FIM completion request with streaming enabled
 * @return A [Flow] of [FIMCompletion] objects representing incremental updates
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a
 * non-2xx status
 */
public fun DeepSeekClientBase.fimCompletionStream(request: FIMCompletionRequest): Flow<FIMCompletion> {
    return flow {
        try {
            client.sse(
                urlString = "beta/completions",
                request = {
                    method = HttpMethod.Post
                    accept(ContentType.Text.EventStream)
                    headers {
                        append(HttpHeaders.CacheControl, "no-cache")
                        append(HttpHeaders.Connection, "keep-alive")
                    }
                    setBody(request)
                    timeout {
                        requestTimeoutMillis = config.fimCompletionTimeout
                    }
                }
            ) {
                incoming.collect { event ->
                    event.data?.trim()?.takeIf { it != "[DONE]" }?.let { data ->
                        val fimChunk = config.jsonConfig.decodeFromString<FIMCompletion>(data)
                        emit(fimChunk)
                    }
                }
            }
        } catch (e: SSEClientException) {
            val response = e.response ?: throw e
            val error = runCatching {
                config.jsonConfig.decodeFromString<DeepSeekError>(response.bodyAsText())
            }.getOrNull()
            throw DeepSeekException.from(response.status.value, response.headers.toDeepSeekHeaders(), error)
        }
    }
}

/**
 * Streams FIM completions using custom parameters and a prompt.
 *
 * This function gives you control over generation behavior while receiving
 * streaming responses for Fill-In-the-Middle completions.
 *
 * Example:
 * ```kotlin
 * val params = fimCompletionStreamParams {
 *     temperature = 0.7
 *     maxTokens = 500
 *     suffix = "}"
 * }
 *
 * client.fim(params, "class Calculator {").collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.text ?: "")
 * }
 * ```
 *
 * @param params Parameters controlling the completion behavior
 * @param prompt The text to start the completion from
 * @return A [Flow] of [FIMCompletion] objects representing the streaming response
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a
 * non-2xx status
 */
public fun DeepSeekClientStream.fim(
    params: FIMCompletionParams,
    prompt: String
): Flow<FIMCompletion> {
    val request =
        (if (params.stream == null || !params.stream) params.copy(stream = true) else params).createRequest(prompt)
    return fimCompletionStream(request)
}

/**
 * Streams FIM completions with default parameters.
 *
 * This simplified function streams completions with default settings,
 * requiring only the prompt text to get started.
 *
 * Example:
 * ```kotlin
 * client.fim("def calculate_area(radius):").collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.text ?: "")
 * }
 * ```
 *
 * @param prompt The text to start the completion from
 * @return A [Flow] of [FIMCompletion] objects representing the streaming response
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a
 * non-2xx status
 */
public fun DeepSeekClientStream.fim(prompt: String): Flow<FIMCompletion> =
    fim(FIMCompletionParams(stream = true), prompt)

/**
 * Streams a fully customizable FIM completion using a builder pattern.
 *
 * This approach gives you complete control over all aspects of the streaming
 * FIM request through a dedicated builder pattern.
 *
 * Example:
 * ```kotlin
 * client.fimCompletion {
 *     prompt("public interface DataProcessor {")
 *     params {
 *         suffix = "}"
 *         temperature = 0.8
 *         maxTokens = 300
 *     }
 * }.collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.text ?: "")
 * }
 * ```
 *
 * @param block A builder block for constructing the complete streaming request
 * @return A [Flow] of [FIMCompletion] objects representing the streaming response
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a
 * non-2xx status
 */
public fun DeepSeekClientStream.fimCompletion(
    block: FIMCompletionRequest.StreamBuilder.() -> Unit
): Flow<FIMCompletion> {
    val request = FIMCompletionRequest.StreamBuilder().apply(block).build()
    return fimCompletionStream(request)
}