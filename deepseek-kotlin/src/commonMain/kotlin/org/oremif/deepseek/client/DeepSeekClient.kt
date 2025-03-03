package org.oremif.deepseek.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.oremif.deepseek.models.*
import kotlin.random.Random

public fun DeepSeekClient(token: String? = null, block: DeepSeekClient.Builder.() -> Unit): DeepSeekClient {
    return DeepSeekClient.Builder(token).apply(block).build()
}

public class DeepSeekClient internal constructor(
    internal val client: HttpClient
) : AutoCloseable {

    public class Builder(token: String? = null) {
        private val deepSeekBaseUrl = "https://api.deepseek.com"

        @OptIn(ExperimentalSerializationApi::class)
        private var jsonConfig: Json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

        private var client: HttpClient = HttpClient {
            install(Auth) {
                if (token == null) return@install
                bearer { loadTokens { BearerTokens(token, "") } }
            }

            install(ContentNegotiation) { json(jsonConfig) }

            defaultRequest {
                url(deepSeekBaseUrl)
                contentType(ContentType.Application.Json)
            }

            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { _, response -> !response.status.isSuccess() }
                delayMillis { retry ->
                    val delay = (retry * 0.2).toLong().coerceAtLeast(1L)
                    retry + Random.nextLong(delay)
                }
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
                sanitizeHeader { header -> header == "Authorization" }
            }
        }

        public fun jsonConfig(block: Json.() -> Unit): Json {
            jsonConfig.apply(block)
            client.config {
                install(ContentNegotiation) { json(jsonConfig.apply(block)) }

            }
            return jsonConfig
        }

        public fun httpClient(block: HttpClient.() -> Unit): HttpClient {
            client.apply(block)
            return client
        }

        public fun build(): DeepSeekClient {
            return DeepSeekClient(client = client)
        }
    }

    override fun close() {
        client.close()
    }
}

public suspend fun DeepSeekClient.chat(params: ChatCompletionParams, messages: List<ChatMessage>): ChatCompletion {
    val request = ChatCompletionRequest(
        messages = messages,
        model = params.model,
        frequencyPenalty = params.frequencyPenalty,
        maxTokens = params.maxTokens,
        presencePenalty = params.presencePenalty,
        responseFormat = params.responseFormat,
        stop = params.stop,
        stream = false,
        streamOptions = null,
        temperature = params.temperature,
        topP = params.topP,
        tools = params.tools,
        toolChoice = params.toolChoice,
        logprobs = params.logprobs,
        topLogprobs = params.topLogprobs,
    )

    return client.post("chat/completions") {
        setBody(request)
    }.body<ChatCompletion>()
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
    val response = client.post("chat/completions") {
        setBody(request)
    }
    println(response.bodyAsText())
    return response.body<ChatCompletion>()
}
