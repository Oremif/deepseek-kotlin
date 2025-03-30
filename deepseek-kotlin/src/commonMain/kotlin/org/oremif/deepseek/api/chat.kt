package org.oremif.deepseek.api

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientBase
import org.oremif.deepseek.models.*
import org.oremif.deepseek.utils.validateResponse

/**
 * Sends a chat completion request to the DeepSeek API.
 *
 * This is a low-level function that handles the direct HTTP communication with the API.
 * Most users will prefer the higher-level [chat] functions instead.
 *
 * Example:
 * ```kotlin
 * val request = ChatCompletionRequest(
 *     model = "deepseek-chat",
 *     messages = listOf(UserMessage("Hello!"))
 * )
 * val response = client.chatCompletion(request)
 * ```
 *
 * @param request The fully configured request object containing all parameters for the API call
 * @return A [ChatCompletion] containing the model's response
 */
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

/**
 * Sends a chat request with custom parameters and messages.
 *
 * This is a versatile function that gives you control over both the model's behavior
 * and the conversation context.
 *
 * Example:
 * ```kotlin
 * // Create custom parameters
 * val params = chatCompletionParams {
 *     temperature = 0.7
 *     maxTokens = 1000
 * }
 *
 * // Define the conversation
 * val messages = listOf(
 *     SystemMessage("You are a helpful assistant specialized in Kotlin"),
 *     UserMessage("What are coroutines?")
 * )
 *
 * // Get the response
 * val response = client.chat(params, messages)
 * println(response.choices.first().message.content)
 * ```
 *
 * @param params Configuration parameters that control the model's behavior
 * @param messages The conversation history as a list of messages
 * @return A [ChatCompletion] containing the model's response
 */
public suspend fun DeepSeekClient.chat(params: ChatCompletionParams, messages: List<ChatMessage>): ChatCompletion {
    val request = (if (params.stream == true) params.copy(stream = false) else params).createRequest(messages)
    return chatCompletion(request)
}

/**
 * Sends a chat request with default parameters.
 *
 * This simplified version uses the default model settings and only requires
 * providing the conversation history.
 *
 * Example:
 * ```kotlin
 * val messages = listOf(
 *     SystemMessage("You are a helpful assistant"),
 *     UserMessage("What is Kotlin?")
 * )
 * val response = client.chat(messages)
 * ```
 *
 * @param messages The conversation history as a list of messages
 * @return A [ChatCompletion] containing the model's response
 */
public suspend fun DeepSeekClient.chat(messages: List<ChatMessage>): ChatCompletion {
    val params = if (config.params is ChatCompletionParams)
        config.params
    else
        ChatCompletionParams(ChatModel.DEEPSEEK_CHAT)
    return chat(params, messages)
}

/**
 * Sends a single user message to the chat API.
 *
 * This is the simplest way to interact with the DeepSeek chat API,
 * perfect for quick queries.
 *
 * Example:
 * ```kotlin
 * val response = client.chat("What's the capital of France?")
 * println(response.choices.first().message.content)
 * ```
 *
 * @param message The message text to send to the model
 * @return A [ChatCompletion] containing the model's response
 */
public suspend fun DeepSeekClient.chat(message: String): ChatCompletion =
    chat(listOf(UserMessage(content = message)))

/**
 * Sends a chat request with custom parameters and a DSL for building messages.
 *
 * This approach combines the flexibility of custom parameters with an intuitive
 * way to construct the conversation.
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionParams {
 *     temperature = 0.8
 *     maxTokens = 2000
 * }
 *
 * val response = client.chat(params) {
 *     system("You are a Kotlin expert")
 *     user("Explain extension functions")
 * }
 * ```
 *
 * @param params Configuration parameters that control the model's behavior
 * @param blockMessage A builder block for constructing the conversation
 * @return A [ChatCompletion] containing the model's response
 */
public suspend fun DeepSeekClient.chat(
    params: ChatCompletionParams,
    blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit,
): ChatCompletion = chat(params, ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

/**
 * Sends a chat request with default parameters and a DSL for building messages.
 *
 * This approach provides a clean, readable way to construct conversations with
 * default model settings.
 *
 * Example:
 * ```kotlin
 * val response = client.chat {
 *     system("You are a helpful AI assistant")
 *     user("Tell me about Kotlin")
 *     assistant("Kotlin is a modern programming language")
 *     user("What about its coroutines?")
 * }
 * ```
 *
 * @param blockMessage A builder block for constructing the conversation
 * @return A [ChatCompletion] containing the model's response
 */
public suspend fun DeepSeekClient.chat(blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit): ChatCompletion =
    chat(ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

/**
 * Sends a fully customizable chat completion request.
 *
 * This approach gives you complete control over all aspects of the request
 * through a convenient builder pattern.
 *
 * Example:
 * ```kotlin
 * val response = client.chatCompletion {
 *     model = "deepseek-chat"
 *     messages {
 *         system("You are a Kotlin expert")
 *         user("How do I use flow in Kotlin?")
 *     }
 *     temperature = 0.7
 *     maxTokens = 2000
 *     frequencyPenalty = 0.5
 * }
 * ```
 *
 * @param block A builder block for constructing the complete request
 * @return A [ChatCompletion] containing the model's response
 */
public suspend fun DeepSeekClient.chatCompletion(block: ChatCompletionRequest.Builder.() -> Unit): ChatCompletion {
    val request = ChatCompletionRequest.Builder().apply(block).build()
    return chatCompletion(request)
}