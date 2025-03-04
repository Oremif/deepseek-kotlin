package org.oremif.deepseek.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.random.Random

public fun DeepSeekClient(token: String? = null, block: DeepSeekClient.Builder.() -> Unit = {}): DeepSeekClient =
    DeepSeekClient.Builder(token).apply(block).build()

public fun DeepSeekClientStream(
    token: String? = null, block: DeepSeekClientStream.Builder.() -> Unit = {}
): DeepSeekClientStream = DeepSeekClientStream.Builder(token).apply(block).build()

public abstract class DeepSeekClientBase(
    public val client: HttpClient, public val config: DeepSeekClientConfig,
) : AutoCloseable {
    public abstract class Builder(token: String? = null) {
        protected val deepSeekBaseUrl: String = "https://api.deepseek.com"

        @OptIn(ExperimentalSerializationApi::class)
        protected var jsonConfig: Json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

        protected var chatCompletionTimeout: Int = 45_000
        protected var fimCompletionTimeout: Int = 60_000

        protected open var client: HttpClient = HttpClient {
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

            install(HttpTimeout) {
                socketTimeoutMillis = 300_000L
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
                sanitizeHeader { header -> header == "Authorization" }
            }
        }

        public fun jsonConfig(block: Json.() -> Unit): Builder {
            jsonConfig.apply(block)
            client.config {
                install(ContentNegotiation) { json(jsonConfig.apply(block)) }

            }
            return this
        }

        public fun chatCompletionTimeout(timeout: Int): Builder {
            chatCompletionTimeout = timeout
            return this
        }

        public fun fimCompletionTimeout(timeout: Int): Builder {
            fimCompletionTimeout = timeout
            return this
        }

        public fun httpClient(block: HttpClientConfig<*>.() -> Unit): Builder {
            client = HttpClient { block(this) }
            return this
        }
    }

    override fun close() {
        client.close()
    }
}

public class DeepSeekClient internal constructor(
    client: HttpClient, config: DeepSeekClientConfig
) : DeepSeekClientBase(client, config) {

    public class Builder(token: String? = null) : DeepSeekClientBase.Builder(token) {
        public fun build(): DeepSeekClient {
            return DeepSeekClient(
                client = client,
                config = DeepSeekClientConfig(
                    jsonConfig,
                    chatCompletionTimeout.toLong(),
                    fimCompletionTimeout.toLong()
                )
            )
        }
    }

}

public class DeepSeekClientStream internal constructor(
    client: HttpClient, config: DeepSeekClientConfig
) : DeepSeekClientBase(client, config) {
    public class Builder(token: String? = null) : DeepSeekClientBase.Builder(token) {
        override var client: HttpClient = super.client.config {
            install(SSE)
        }

        public fun build(): DeepSeekClientStream {
            return DeepSeekClientStream(
                client = client,
                config = DeepSeekClientConfig(
                    jsonConfig,
                    chatCompletionTimeout.toLong(),
                    fimCompletionTimeout.toLong()
                )
            )
        }
    }
}
