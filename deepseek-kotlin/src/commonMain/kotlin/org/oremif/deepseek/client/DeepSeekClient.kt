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
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonNamingStrategy
import org.oremif.deepseek.utils.computeRetryDelayMillis
import org.oremif.deepseek.utils.isRetryableStatus

/**
 * Creates a new instance of [DeepSeekClient] with optional configuration.
 *
 * This constructor function allows creating a client with minimal setup using a token,
 * or with advanced configuration via the [block] parameter.
 *
 * Example:
 * ```kotlin
 * // Simple client with API token
 * val client = DeepSeekClient("your-api-token")
 *
 * // Client with custom configuration
 * val customClient = DeepSeekClient("your-api-token") {
 *     chatCompletionTimeout(60000)
 *     jsonConfig { prettyPrint = false }
 * }
 * ```
 *
 * @param token The DeepSeek API token for authentication
 * @param block Configuration block for additional customization
 * @return A new [DeepSeekClient] instance
 */
public fun DeepSeekClient(token: String? = null, block: DeepSeekClient.Builder.() -> Unit = {}): DeepSeekClient =
    DeepSeekClient.Builder(token).apply(block).build()

/**
 * Creates a new instance of [DeepSeekClientStream] with optional configuration.
 *
 * This constructor function creates a streaming-capable client for receiving real-time
 * responses from the DeepSeek API, with optional advanced configuration.
 *
 * Example:
 * ```kotlin
 * // Simple streaming client with API token
 * val streamClient = DeepSeekClientStream("your-api-token")
 *
 * // Stream client with custom configuration
 * val customStreamClient = DeepSeekClientStream("your-api-token") {
 *     fimCompletionTimeout(90000)
 * }
 * ```
 *
 * @param token The DeepSeek API token for authentication
 * @param block Configuration block for additional customization
 * @return A new [DeepSeekClientStream] instance
 */
public fun DeepSeekClientStream(
    token: String? = null, block: DeepSeekClientStream.Builder.() -> Unit = {}
): DeepSeekClientStream = DeepSeekClientStream.Builder(token).apply(block).build()

/**
 * Base class for DeepSeek API clients providing core functionality.
 *
 * This abstract class serves as a foundation for both standard and streaming
 * DeepSeek clients, handling HTTP interactions and common configuration.
 *
 * The client is designed to be long-lived: create one instance and reuse it for the
 * lifetime of your application. The underlying HTTP client automatically releases idle
 * connections and threads, so calling [close] is usually unnecessary.
 *
 * @property client The underlying HTTP client used for API requests
 * @property config Configuration options for the DeepSeek client
 */
public abstract class DeepSeekClientBase(
    public val client: HttpClient, public val config: DeepSeekClientConfig,
) {

    /**
     * Base builder class for configuring DeepSeek clients.
     *
     * Provides common configuration options that apply to all DeepSeek client types.
     *
     * @param token The DeepSeek API token for authentication
     */
    public abstract class Builder(protected val token: String? = null) {
        /**
         * Base URL for the DeepSeek API.
         */
        protected var deepSeekBaseUrl: String = "https://api.deepseek.com"

        /**
         * JSON configuration for serialization and deserialization.
         */
        @OptIn(ExperimentalSerializationApi::class)
        protected var jsonConfig: Json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

        /**
         * Timeout in milliseconds for chat completion requests.
         */
        protected var chatCompletionTimeout: Int = 45_000

        /**
         * Timeout in milliseconds for file-in-the-middle completion requests.
         */
        protected var fimCompletionTimeout: Int = 60_000

        private val httpClientConfigBlocks: MutableList<HttpClientConfig<*>.() -> Unit> = mutableListOf()
        private var httpClientOverride: HttpClient? = null

        public fun baseUrl(url: String): Builder {
            deepSeekBaseUrl = url
            return this
        }

        /**
         * Configures the JSON serialization and deserialization settings.
         *
         * The [block] receives a [JsonBuilder] and is applied on top of the current
         * [jsonConfig], preserving previously-set fields. The resulting [Json] is used by the
         * HTTP client built in [DeepSeekClient.Builder.build] / [DeepSeekClientStream.Builder.build].
         *
         * Example:
         * ```kotlin
         * val client = DeepSeekClient("token") {
         *     jsonConfig {
         *         prettyPrint = false
         *         ignoreUnknownKeys = true
         *     }
         * }
         * ```
         *
         * @param block Configuration block receiving a [JsonBuilder]
         * @return This builder for chaining
         */
        public fun jsonConfig(block: JsonBuilder.() -> Unit): Builder {
            jsonConfig = Json(from = jsonConfig, builderAction = block)
            return this
        }

        /**
         * Replaces the JSON configuration with the provided instance.
         *
         * The given [json] is used as-is by the HTTP client's content negotiation. Any previous
         * [jsonConfig] settings are discarded.
         *
         * @param json Fully configured [Json] instance
         * @return This builder for chaining
         */
        public fun jsonConfig(json: Json): Builder {
            jsonConfig = json
            return this
        }

        /**
         * Sets the timeout for chat completion requests.
         *
         * @param timeout Timeout in milliseconds
         * @return This builder for chaining
         */
        public fun chatCompletionTimeout(timeout: Int): Builder {
            chatCompletionTimeout = timeout
            return this
        }

        /**
         * Sets the timeout for file-in-the-middle completion requests.
         *
         * @param timeout Timeout in milliseconds
         * @return This builder for chaining
         */
        public fun fimCompletionTimeout(timeout: Int): Builder {
            fimCompletionTimeout = timeout
            return this
        }

        /**
         * Applies additional configuration on top of the default HTTP client.
         *
         * The [block] is layered on top of the builder defaults (Auth, ContentNegotiation with
         * the current [jsonConfig], base URL via `defaultRequest`, HttpRequestRetry, HttpTimeout,
         * Logging — plus SSE for [DeepSeekClientStream]) at [DeepSeekClient.Builder.build] /
         * [DeepSeekClientStream.Builder.build] time. Calling this method multiple times layers
         * each [block] on top of the previous state, in the order they were added.
         *
         * Because the HTTP client is assembled lazily, [jsonConfig] changes are picked up
         * regardless of whether they are set before or after [httpClient].
         *
         * Use [httpClient] with an [HttpClient] argument if you want to replace the underlying
         * client entirely instead of extending it.
         *
         * Example — override the request timeout while keeping all other defaults:
         * ```kotlin
         * val client = DeepSeekClient("token") {
         *     httpClient {
         *         install(HttpTimeout) {
         *             requestTimeoutMillis = 60_000
         *         }
         *     }
         * }
         * ```
         *
         * @param block Additional configuration to merge on top of the default HTTP client
         * @return This builder for chaining
         */
        public fun httpClient(block: HttpClientConfig<*>.() -> Unit): Builder {
            httpClientConfigBlocks += block
            return this
        }

        /**
         * Replaces the underlying HTTP client entirely with the provided [client].
         *
         * Unlike the [httpClient] overload that takes a configuration block, this overload does
         * **not** preserve the builder defaults — the caller is responsible for installing Auth,
         * ContentNegotiation, the base URL in `defaultRequest`, retries, timeouts, logging, and
         * (for streaming) SSE. [jsonConfig] and any [httpClient] blocks added before or after
         * this call are ignored for the HTTP client itself, though [jsonConfig] is still
         * surfaced via `DeepSeekClientConfig.jsonConfig`.
         *
         * @param client Fully configured HTTP client to use
         * @return This builder for chaining
         */
        public fun httpClient(client: HttpClient): Builder {
            this.httpClientOverride = client
            return this
        }

        /**
         * Builds the default HTTP client with the accumulated [jsonConfig], [deepSeekBaseUrl],
         * and [token]. Subclasses override to layer additional plugins (e.g. SSE for the
         * streaming client) on top.
         */
        protected open fun defaultHttpClient(): HttpClient = HttpClient {
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
                retryIf { _, response -> isRetryableStatus(response.status.value) }
                retryOnException(maxRetries = 3, retryOnTimeout = true)
                delayMillis(respectRetryAfterHeader = true) { retry ->
                    computeRetryDelayMillis(retry)
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

        /**
         * Materialises the HTTP client: either the replacement set via [httpClient] with an
         * [HttpClient] argument, or the [defaultHttpClient] with every [httpClient] block
         * layered on top in insertion order.
         */
        protected fun buildHttpClient(): HttpClient {
            httpClientOverride?.let { return it }
            var client = defaultHttpClient()
            for (block in httpClientConfigBlocks) {
                client = client.config(block)
            }
            return client
        }
    }

    /**
     * Closes the underlying HTTP client and releases its resources.
     *
     * Calling this method is usually unnecessary — the underlying HTTP client automatically
     * releases threads and connections if they remain idle. Call this only if you need to
     * aggressively free resources (e.g. during application shutdown).
     *
     * The client cannot be reused after [close] is called.
     */
    public open fun close() {
        client.close()
    }
}

/**
 * Client for interacting with the DeepSeek API.
 *
 * This client provides methods for all DeepSeek API endpoints, including
 * chat completions, embeddings, and other AI capabilities.
 */
public class DeepSeekClient internal constructor(
    client: HttpClient, config: DeepSeekClientConfig
) : DeepSeekClientBase(client, config) {

    /**
     * Builder for configuring and creating DeepSeekClient instances.
     *
     * @param token The DeepSeek API token for authentication
     */
    public class Builder(token: String? = null) : DeepSeekClientBase.Builder(token) {
        /**
         * Builds and returns a new DeepSeekClient instance with the configured settings.
         *
         * @return A new DeepSeekClient instance
         */
        internal fun build(): DeepSeekClient {
            return DeepSeekClient(
                client = buildHttpClient(),
                config = DeepSeekClientConfig(
                    jsonConfig,
                    chatCompletionTimeout.toLong(),
                    fimCompletionTimeout.toLong()
                )
            )
        }
    }

}

/**
 * Client for streaming interactions with the DeepSeek API.
 *
 * This client specializes in handling streaming responses for real-time
 * processing of DeepSeek API outputs, particularly useful for chat completions.
 */
public class DeepSeekClientStream internal constructor(
    client: HttpClient, config: DeepSeekClientConfig
) : DeepSeekClientBase(client, config) {

    /**
     * Builder for configuring and creating DeepSeekClientStream instances.
     *
     * @param token The DeepSeek API token for authentication
     */
    public class Builder(token: String? = null) : DeepSeekClientBase.Builder(token) {
        /**
         * Layers [SSE] on top of the base default client so streaming endpoints can be used.
         */
        override fun defaultHttpClient(): HttpClient = super.defaultHttpClient().config {
            install(SSE)
        }

        /**
         * Builds and returns a new DeepSeekClientStream instance with the configured settings.
         *
         * @return A new DeepSeekClientStream instance
         */
        internal fun build(): DeepSeekClientStream {
            return DeepSeekClientStream(
                client = buildHttpClient(),
                config = DeepSeekClientConfig(
                    jsonConfig,
                    chatCompletionTimeout.toLong(),
                    fimCompletionTimeout.toLong()
                )
            )
        }
    }
}
