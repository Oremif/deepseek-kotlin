package org.oremif.deepseek.client

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test

private inline fun <T : DeepSeekClientBase, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        close()
    }

class DeepSeekClientBuilderTests {

    @Test
    fun `httpClient block preserves default plugins on DeepSeekClient`() {
        DeepSeekClient("test-token") {
            httpClient { /* additive configuration — intentionally empty */ }
        }.use { client ->
            val http = client.client
            http.pluginOrNull(Auth).shouldNotBeNull()
            http.pluginOrNull(ContentNegotiation).shouldNotBeNull()
            http.pluginOrNull(HttpRequestRetry).shouldNotBeNull()
            http.pluginOrNull(HttpTimeout).shouldNotBeNull()
        }
    }

    @Test
    fun `httpClient block preserves SSE and base plugins on DeepSeekClientStream`() {
        DeepSeekClientStream("test-token") {
            httpClient { /* additive configuration — intentionally empty */ }
        }.use { client ->
            val http = client.client
            http.pluginOrNull(SSE).shouldNotBeNull()
            http.pluginOrNull(Auth).shouldNotBeNull()
            http.pluginOrNull(ContentNegotiation).shouldNotBeNull()
            http.pluginOrNull(HttpRequestRetry).shouldNotBeNull()
            http.pluginOrNull(HttpTimeout).shouldNotBeNull()
        }
    }

    @Test
    fun `logging is not installed by default`() {
        DeepSeekClient("test-token").use { client ->
            client.client.pluginOrNull(Logging).shouldBeNull()
        }
    }

    @Test
    fun `logging block installs Logging plugin`() {
        DeepSeekClient("test-token") {
            logging { level = LogLevel.BODY }
        }.use { client ->
            client.client.pluginOrNull(Logging).shouldNotBeNull()
        }
    }

    @Test
    fun `logging block with no args installs Logging with defaults`() {
        DeepSeekClient("test-token") {
            logging()
        }.use { client ->
            client.client.pluginOrNull(Logging).shouldNotBeNull()
        }
    }

    @Test
    fun `logging block applies to stream client and survives httpClient block`() {
        DeepSeekClientStream("test-token") {
            logging { level = LogLevel.HEADERS }
            httpClient { /* additive configuration — intentionally empty */ }
        }.use { client ->
            client.client.pluginOrNull(Logging).shouldNotBeNull()
            client.client.pluginOrNull(SSE).shouldNotBeNull()
        }
    }

    @Test
    fun `logging block can be called multiple times and accumulates sanitizers`() {
        DeepSeekClient("test-token") {
            logging { level = LogLevel.BODY }
            logging {
                sanitizeHeader { header -> header == "Cookie" }
                sanitizeHeader { header -> header == "X-Trace-Id" }
            }
        }.use { client ->
            client.client.pluginOrNull(Logging).shouldNotBeNull()
        }
    }

    @Test
    fun `httpClient with full replacement drops default plugins`() {
        val replacement = HttpClient { /* no plugins */ }
        DeepSeekClient("test-token") {
            httpClient(replacement)
        }.use { client ->
            client.client.pluginOrNull(Auth).shouldBeNull()
        }
    }

    @Test
    fun `jsonConfig block mutates settings through JsonBuilder receiver`() {
        DeepSeekClient("test-token") {
            jsonConfig {
                prettyPrint = false
                ignoreUnknownKeys = false
                isLenient = false
            }
        }.use { client ->
            val cfg = client.config.jsonConfig.configuration
            cfg.prettyPrint.shouldBeFalse()
            cfg.ignoreUnknownKeys.shouldBeFalse()
            cfg.isLenient.shouldBeFalse()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `jsonConfig block preserves unset defaults`() {
        DeepSeekClient("test-token") {
            jsonConfig {
                prettyPrint = false
            }
        }.use { client ->
            val cfg = client.config.jsonConfig.configuration
            cfg.prettyPrint.shouldBeFalse()
            cfg.ignoreUnknownKeys.shouldBeTrue()
            cfg.namingStrategy.shouldNotBeNull()
        }
    }

    @Test
    fun `jsonConfig Json overload stores provided instance`() {
        val custom = Json {
            prettyPrint = false
            coerceInputValues = true
        }
        DeepSeekClient("test-token") {
            jsonConfig(custom)
        }.use { client ->
            client.config.jsonConfig shouldBeSameInstanceAs custom
        }
    }

    @Test
    fun `jsonConfig block applies to stream client`() {
        DeepSeekClientStream("test-token") {
            jsonConfig {
                prettyPrint = false
            }
        }.use { client ->
            client.config.jsonConfig.configuration.prettyPrint.shouldBeFalse()
            client.client.pluginOrNull(SSE).shouldNotBeNull()
            client.client.pluginOrNull(ContentNegotiation).shouldNotBeNull()
        }
    }
}
