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

public fun DeepSeekClient(token: String? = null, block: DeepSeekClient.Builder.() -> Unit): DeepSeekClient =
    DeepSeekClient.Builder(token).apply(block).build()

public fun DeepSeekClientStream(
    token: String? = null, block: DeepSeekClientStream.Builder.() -> Unit
): DeepSeekClientStream = DeepSeekClientStream.Builder(token).apply(block).build()

public abstract class DeepSeekClientBase(
    internal val client: HttpClient, internal val jsonConfig: Json = Json
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
    }

    override fun close() {
        client.close()
    }
}

public class DeepSeekClient internal constructor(
    client: HttpClient, jsonConfig: Json
) : DeepSeekClientBase(client, jsonConfig) {

    public class Builder(token: String? = null) : DeepSeekClientBase.Builder(token) {
        public fun build(): DeepSeekClient {
            return DeepSeekClient(client = client, jsonConfig = jsonConfig)
        }
    }

}

public class DeepSeekClientStream internal constructor(
    client: HttpClient, jsonConfig: Json
) : DeepSeekClientBase(client, jsonConfig) {
    public class Builder(token: String? = null) : DeepSeekClientBase.Builder(token) {
        override var client: HttpClient = super.client.config {
            install(SSE)
        }

        public fun build(): DeepSeekClientStream {
            return DeepSeekClientStream(client = client, jsonConfig = jsonConfig)
        }
    }
}
