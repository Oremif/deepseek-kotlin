package org.oremif.deepseek.api

import io.ktor.client.plugins.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.*

/**
 * Streams chat completion responses chunk by chunk from the DeepSeek API.
 *
 * This function handles the low-level communication with the Server-Sent Events (SSE)
 * endpoint, allowing you to receive and process model responses in real-time as they're
 * generated.
 *
 * Example:
 * ```kotlin
 * val request = ChatCompletionRequest(
 *     model = "deepseek-chat",
 *     messages = listOf(UseMessage("Write a story")),
 *     stream = true
 * )
 *
 * client.chatCompletionStream(request).collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param request The chat completion request with streaming enabled
 * @return A [Flow] of [ChatCompletionChunk] objects representing incremental updates
 */
public suspend fun DeepSeekClientBase.chatCompletionStream(request: ChatCompletionRequest): Flow<ChatCompletionChunk> {
    return flow {
        try {
            client.sse(
                urlString = "chat/completions",
                request = {
                    method = HttpMethod.Post
                    accept(ContentType.Text.EventStream)
                    headers {
                        append(HttpHeaders.CacheControl, "no-cache")
                        append(HttpHeaders.Connection, "keep-alive")
                    }
                    setBody(request)
                    timeout {
                        requestTimeoutMillis = config.chatCompletionTimeout
                    }
                }
            ) {
                incoming.collect { event ->
                    event.data?.trim()?.takeIf { it != "[DONE]" }?.let { data ->
                        val chatChunk = config.jsonConfig.decodeFromString<ChatCompletionChunk>(data)
                        emit(chatChunk)
                    }
                }
            }
        } catch (e: SSEClientException) {
            e.response?.let { response ->
                throw DeepSeekException.from(response.status.value, response.headers, null)
            }
        }
    }
}

/**
 * Streams chat responses using custom parameters and messages.
 *
 * This function lets you specify both behavioral parameters and conversation messages
 * while getting real-time streaming responses from the model.
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionStreamParams {
 *     temperature = 0.8
 *     maxTokens = 2000
 * }
 *
 * val messages = listOf(
 *     SystemMessage("You are a storyteller"),
 *     UserMessage("Tell me a short sci-fi story")
 * )
 *
 * client.chat(params, messages).collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param params Parameters controlling the model's behavior
 * @param messages The conversation history as a list of messages
 * @return A [Flow] of [ChatCompletionChunk] objects representing the streaming response
 */
public suspend fun DeepSeekClientStream.chat(
    params: ChatCompletionParams,
    messages: List<ChatMessage>
): Flow<ChatCompletionChunk> {
    val request = (if (params.stream == false) params.copy(stream = true) else params).createRequest(messages)
    return chatCompletionStream(request)
}

/**
 * Streams chat responses using default parameters.
 *
 * This simplified function uses the default DEEPSEEK_CHAT model with streaming enabled,
 * providing a clean way to get streaming responses with minimal configuration.
 *
 * Example:
 * ```kotlin
 * val messages = listOf(
 *     SystemMessage("You are a coding assistant"),
 *     UserMessage("Explain how to use Flows in Kotlin")
 * )
 *
 * client.chat(messages).collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param messages The conversation history as a list of messages
 * @return A [Flow] of [ChatCompletionChunk] objects representing the streaming response
 */
public suspend fun DeepSeekClientStream.chat(messages: List<ChatMessage>): Flow<ChatCompletionChunk> {
    val params = if (config.params is ChatCompletionParams)
        config.params
    else
        ChatCompletionParams(ChatModel.DEEPSEEK_CHAT, stream = true)
    return chat(params, messages)
}

/**
 * Streams chat responses for a single user message.
 *
 * This is the most straightforward way to get streaming responses from the model,
 * requiring only a simple text message.
 *
 * Example:
 * ```kotlin
 * client.chat("Write a poem about programming").collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param message The message text to send to the model
 * @return A [Flow] of [ChatCompletionChunk] objects representing the streaming response
 */
public suspend fun DeepSeekClientStream.chat(message: String): Flow<ChatCompletionChunk> =
    chat(listOf(UserMessage(content = message)))

/**
 * Streams chat responses using custom parameters and a message builder DSL.
 *
 * This approach combines custom parameters with an intuitive way to build
 * the conversation history.
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionStreamParams {
 *     temperature = 0.7
 *     maxTokens = 1000
 * }
 *
 * client.chat(params) {
 *     system("You are a technical writer")
 *     user("Create a tutorial introduction for Kotlin coroutines")
 * }.collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param params Parameters controlling the model's behavior
 * @param blockMessage A builder block for constructing the conversation
 * @return A [Flow] of [ChatCompletionChunk] objects representing the streaming response
 */
public suspend fun DeepSeekClientStream.chat(
    params: ChatCompletionParams,
    blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit,
): Flow<ChatCompletionChunk> =
    chat(params, ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

/**
 * Streams chat responses using default parameters and a message builder DSL.
 *
 * This function offers a clean, readable way to build conversations with
 * default streaming settings.
 *
 * Example:
 * ```kotlin
 * client.chat {
 *     system("You are a helpful assistant")
 *     user("Explain quantum computing")
 * }.collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param blockMessage A builder block for constructing the conversation
 * @return A [Flow] of [ChatCompletionChunk] objects representing the streaming response
 */
public suspend fun DeepSeekClientStream.chat(
    blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit
): Flow<ChatCompletionChunk> =
    chat(ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

/**
 * Streams a fully customizable chat completion request.
 *
 * This approach gives you complete control over all aspects of the streaming
 * request through a dedicated builder pattern.
 *
 * Example:
 * ```kotlin
 * client.chatCompletion {
 *     model = "deepseek-chat"
 *     messages {
 *         system("You are an expert programmer")
 *         user("Show me how to implement a binary search tree in Kotlin")
 *     }
 *     temperature = 0.7
 *     maxTokens = 2000
 * }.collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param block A builder block for constructing the complete streaming request
 * @return A [Flow] of [ChatCompletionChunk] objects representing the streaming response
 */
public suspend fun DeepSeekClientStream.chatCompletion(
    block: ChatCompletionRequest.StreamBuilder.() -> Unit
): Flow<ChatCompletionChunk> {
    val request = ChatCompletionRequest.StreamBuilder().apply(block).build()
    return chatCompletionStream(request)
}
