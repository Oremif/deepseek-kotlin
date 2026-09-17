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
import org.oremif.deepseek.models.responses.*

/**
 * Streams a Responses API call as semantic server-sent events.
 *
 * Unlike the chat stream, which repeats the whole envelope in every chunk, the Responses API
 * narrates the response as it is assembled — see [ResponseStreamEvent]. There is no `[DONE]`
 * marker: the flow ends after [ResponseCompletedEvent], [ResponseIncompleteEvent] or
 * [ResponseFailedEvent], each carrying the finished [ModelResponse] with its usage.
 *
 * Example:
 * ```kotlin
 * val request = responseStreamParams { model = ChatModel.DEEPSEEK_FLASH }
 *     .createRequest("Write a haiku about Kotlin")
 *
 * client.createResponseStream(request).collect { event ->
 *     if (event is OutputTextDeltaEvent) print(event.delta)
 * }
 * ```
 *
 * @param request The request to stream; its `stream` flag is expected to be set
 * @return A [Flow] of the events the API emits, in order
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a non-2xx
 *   status
 */
public fun DeepSeekClientBase.createResponseStream(
    request: ResponseRequest
): Flow<ResponseStreamEvent> {
    return flow {
        try {
            client.sse(
                urlString = RESPONSES_PATH,
                request = {
                    method = HttpMethod.Post
                    accept(ContentType.Text.EventStream)
                    headers {
                        append(HttpHeaders.CacheControl, "no-cache")
                        append(HttpHeaders.Connection, "keep-alive")
                    }
                    setBody(request)
                    timeout { requestTimeoutMillis = config.responseTimeout }
                },
            ) {
                incoming.collect { sse ->
                    // The API sends no terminator, but an OpenAI-compatible proxy in front of it
                    // may add the chat endpoint's, so the marker is skipped rather than parsed.
                    sse.data
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() && it != "[DONE]" }
                        ?.let { data ->
                            emit(config.jsonConfig.decodeFromString<ResponseStreamEvent>(data))
                        }
                }
            }
        } catch (e: SSEClientException) {
            val response = e.response ?: throw e
            val error = runCatching {
                config.jsonConfig.decodeFromString<DeepSeekError>(response.bodyAsText())
            }
                .getOrNull()
            throw DeepSeekException.from(
                response.status.value,
                response.headers.toDeepSeekHeaders(),
                error,
            )
        }
    }
}

/**
 * Streams a conversation through the Responses API with custom parameters.
 *
 * Example:
 * ```kotlin
 * val params = responseStreamParams { instructions = "You are a helpful assistant." }
 * client.createResponse(params, history).collect { event ->
 *     if (event is OutputTextDeltaEvent) print(event.delta)
 * }
 * ```
 *
 * @param params Parameters controlling the model's behavior
 * @param input The conversation so far, in the order the model should see it
 * @return A [Flow] of the events the API emits, in order
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a non-2xx
 *   status
 */
public fun DeepSeekClientStream.createResponse(
    params: ResponseParams,
    input: List<ResponseItem>,
): Flow<ResponseStreamEvent> = createResponseStream(params.streaming().createRequest(input))

/**
 * Streams a single user message through the Responses API with custom parameters.
 *
 * @param params Parameters controlling the model's behavior
 * @param input Text of the user's message
 * @return A [Flow] of the events the API emits, in order
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a non-2xx
 *   status
 */
public fun DeepSeekClientStream.createResponse(
    params: ResponseParams,
    input: String,
): Flow<ResponseStreamEvent> = createResponseStream(params.streaming().createRequest(input))

/**
 * Streams a single user message through the Responses API with default parameters.
 *
 * Example:
 * ```kotlin
 * client.createResponse("Write a haiku about Kotlin").collect { event ->
 *     if (event is OutputTextDeltaEvent) print(event.delta)
 * }
 * ```
 *
 * @param input Text of the user's message
 * @return A [Flow] of the events the API emits, in order
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a non-2xx
 *   status
 */
public fun DeepSeekClientStream.createResponse(input: String): Flow<ResponseStreamEvent> =
    createResponse(ResponseParams(stream = true), input)

/**
 * Streams a conversation through the Responses API with default parameters.
 *
 * @param input The conversation so far, in the order the model should see it
 * @return A [Flow] of the events the API emits, in order
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a non-2xx
 *   status
 */
public fun DeepSeekClientStream.createResponse(
    input: List<ResponseItem>
): Flow<ResponseStreamEvent> = createResponse(ResponseParams(stream = true), input)

/**
 * Streams a conversation built by a DSL, with custom parameters.
 *
 * @param params Parameters controlling the model's behavior
 * @param block A builder block for constructing the conversation
 * @return A [Flow] of the events the API emits, in order
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a non-2xx
 *   status
 */
public fun DeepSeekClientStream.createResponse(
    params: ResponseParams,
    block: ResponseRequest.InputBuilder.() -> Unit,
): Flow<ResponseStreamEvent> =
    createResponse(params, ResponseRequest.InputBuilder().apply(block).build())

/**
 * Streams a fully customizable Responses API request.
 *
 * Example:
 * ```kotlin
 * client.createResponse {
 *     params {
 *         model = ChatModel.DEEPSEEK_V4_PRO
 *         reasoning = ReasoningConfig(ReasoningEffort.LOW)
 *     }
 *     input { user("Write a haiku about Kotlin") }
 * }.collect { event ->
 *     if (event is OutputTextDeltaEvent) print(event.delta)
 * }
 * ```
 *
 * @param block A builder block for constructing the complete streaming request
 * @return A [Flow] of the events the API emits, in order
 * @throws DeepSeekException from the returned [Flow]'s collector if the API returns a non-2xx
 *   status
 */
public fun DeepSeekClientStream.createResponse(
    block: ResponseRequest.StreamBuilder.() -> Unit
): Flow<ResponseStreamEvent> =
    createResponseStream(ResponseRequest.StreamBuilder().apply(block).build())
