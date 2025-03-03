package org.oremif.deepseek.api

import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.oremif.deepseek.models.ChatCompletionParams
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.*

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
                }
            ) {
                incoming.collect { event ->
                    event.data?.trim()?.takeIf { it != "[DONE]" }?.let { data ->
                        val chatChunk = jsonConfig.decodeFromString<ChatCompletionChunk>(data)
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

public suspend fun DeepSeekClientStream.chat(
    params: ChatCompletionParams,
    messages: List<ChatMessage>
): Flow<ChatCompletionChunk> {
    val request = (if (params.stream == false) params.copy(stream = true) else params).createRequest(messages)
    return chatCompletionStream(request)
}

public suspend fun DeepSeekClientStream.chat(messages: List<ChatMessage>): Flow<ChatCompletionChunk> =
    chat(ChatCompletionParams(ChatModel.DEEPSEEK_CHAT, stream = true), messages)

public suspend fun DeepSeekClientStream.chat(message: String): Flow<ChatCompletionChunk> =
    chat(listOf(UserMessage(content = message)))

public suspend fun DeepSeekClientStream.chat(
    params: ChatCompletionParams,
    blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit,
): Flow<ChatCompletionChunk> =
    chat(params, ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

public suspend fun DeepSeekClientStream.chat(
    blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit
): Flow<ChatCompletionChunk> =
    chat(ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

public suspend fun DeepSeekClientStream.chatCompletion(
    block: ChatCompletionRequest.StreamBuilder.() -> Unit
): Flow<ChatCompletionChunk> {
    val request = ChatCompletionRequest.StreamBuilder().apply(block).build()
    return chatCompletionStream(request)
}
