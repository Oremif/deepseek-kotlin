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
 * @property client The underlying HTTP client used for API requests
 * @property config Configuration options for the DeepSeek client
 */
public abstract class DeepSeekClientBase(
    public val client: HttpClient, public val config: DeepSeekClientConfig,
) : AutoCloseable {

    /**
     * Base builder class for configuring DeepSeek clients.
     *
     * Provides common configuration options that apply to all DeepSeek client types.
     *
     * @param token The DeepSeek API token for authentication
     */
    public abstract class Builder(token: String? = null) {
        /**
         * Base URL for the DeepSeek API.
         */
        protected val deepSeekBaseUrl: String = "https://api.deepseek.com"

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

        /**
         * The HTTP client used for API requests.
         */
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

        /**
         * Configures the JSON serialization and deserialization settings.
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
         * @param block Configuration block for JSON settings
         * @return This builder for chaining
         */
        public fun jsonConfig(block: Json.() -> Unit): Builder {
            jsonConfig.apply(block)
            client.config {
                install(ContentNegotiation) { json(jsonConfig.apply(block)) }

            }
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
         * Configures the underlying HTTP client with custom settings.
         *
         * Use this method for advanced customization of the HTTP client.
         *
         * Example:
         * ```kotlin
         * val client = DeepSeekClient("token") {
         *     httpClient {
         *         install(HttpTimeout) {
         *             requestTimeoutMillis = 60000
         *         }
         *     }
         * }
         * ```
         *
         * @param block Configuration block for the HTTP client
         * @return This builder for chaining
         */
        public fun httpClient(block: HttpClientConfig<*>.() -> Unit): Builder {
            client = HttpClient { block(this) }
            return this
        }
    }

    /**
     * Closes the underlying HTTP client and releases resources.
     */
    override fun close() {
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
         * The HTTP client with Server-Sent Events support for streaming responses.
         */
        override var client: HttpClient = super.client.config {
            install(SSE)
        }

        /**
         * Builds and returns a new DeepSeekClientStream instance with the configured settings.
         *
         * @return A new DeepSeekClientStream instance
         */
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
