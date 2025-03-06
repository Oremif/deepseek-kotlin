package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.models.*
import org.oremif.deepseek.utils.validateResponse

public suspend fun DeepSeekClientBase.chatCompletion(request: ChatCompletionRequest): ChatCompletion {
    val response = client.post("chat/completions") {
        setBody(request)
        timeout {
            requestTimeoutMillis = config.chatCompletionTimeout
        }
    }
    validateResponse(response)
    return response.body()
}

public suspend fun DeepSeekClient.chat(params: ChatCompletionParams, messages: List<ChatMessage>): ChatCompletion {
    val request = (if (params.stream == true) params.copy(stream = false) else params).createRequest(messages)
    return chatCompletion(request)
}

public suspend fun DeepSeekClient.chat(messages: List<ChatMessage>): ChatCompletion =
    chat(ChatCompletionParams(ChatModel.DEEPSEEK_CHAT), messages)

public suspend fun DeepSeekClient.chat(message: String): ChatCompletion =
    chat(listOf(UserMessage(content = message)))

public suspend fun DeepSeekClient.chat(
    params: ChatCompletionParams,
    blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit,
): ChatCompletion = chat(params, ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

public suspend fun DeepSeekClient.chat(blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit): ChatCompletion =
    chat(ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

public suspend fun DeepSeekClient.chatCompletion(block: ChatCompletionRequest.Builder.() -> Unit): ChatCompletion {
    val request = ChatCompletionRequest.Builder().apply(block).build()
    return chatCompletion(request)
}