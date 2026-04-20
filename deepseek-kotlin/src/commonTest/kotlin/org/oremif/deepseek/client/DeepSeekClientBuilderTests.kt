package org.oremif.deepseek.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.sse.SSE
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
            assertNotNull(http.pluginOrNull(Auth), "Auth must survive httpClient { }")
            assertNotNull(
                http.pluginOrNull(ContentNegotiation),
                "ContentNegotiation must survive httpClient { }"
            )
            assertNotNull(
                http.pluginOrNull(HttpRequestRetry),
                "HttpRequestRetry must survive httpClient { }"
            )
            assertNotNull(http.pluginOrNull(HttpTimeout), "HttpTimeout must survive httpClient { }")
            assertNotNull(http.pluginOrNull(Logging), "Logging must survive httpClient { }")
        }
    }

    @Test
    fun `httpClient block preserves SSE and base plugins on DeepSeekClientStream`() {
        DeepSeekClientStream("test-token") {
            httpClient { /* additive configuration — intentionally empty */ }
        }.use { client ->
            val http = client.client
            assertNotNull(http.pluginOrNull(SSE), "SSE must survive httpClient { } on stream client")
            assertNotNull(http.pluginOrNull(Auth), "Auth must survive on stream client")
            assertNotNull(
                http.pluginOrNull(ContentNegotiation),
                "ContentNegotiation must survive on stream client"
            )
            assertNotNull(
                http.pluginOrNull(HttpRequestRetry),
                "HttpRequestRetry must survive on stream client"
            )
            assertNotNull(
                http.pluginOrNull(HttpTimeout),
                "HttpTimeout must survive on stream client"
            )
            assertNotNull(http.pluginOrNull(Logging), "Logging must survive on stream client")
        }
    }

    @Test
    fun `httpClient with full replacement drops default plugins`() {
        val replacement = HttpClient { /* no plugins */ }
        DeepSeekClient("test-token") {
            httpClient(replacement)
        }.use { client ->
            assertNull(
                client.client.pluginOrNull(Auth),
                "Replacement overload must not silently re-add Auth"
            )
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
            assertFalse(cfg.prettyPrint, "prettyPrint must reflect user setting")
            assertFalse(cfg.ignoreUnknownKeys, "ignoreUnknownKeys must reflect user setting")
            assertFalse(cfg.isLenient, "isLenient must reflect user setting")
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
            assertFalse(cfg.prettyPrint, "prettyPrint must be flipped")
            assertTrue(cfg.ignoreUnknownKeys, "ignoreUnknownKeys default must survive")
            assertNotNull(cfg.namingStrategy, "SnakeCase namingStrategy default must survive")
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
            assertSame(
                custom, client.config.jsonConfig,
                "Json overload must store the exact instance provided"
            )
        }
    }

    @Test
    fun `jsonConfig block applies to stream client`() {
        DeepSeekClientStream("test-token") {
            jsonConfig {
                prettyPrint = false
            }
        }.use { client ->
            assertFalse(
                client.config.jsonConfig.configuration.prettyPrint,
                "Stream client must honour jsonConfig { } block"
            )
            assertNotNull(
                client.client.pluginOrNull(SSE),
                "SSE must survive with custom jsonConfig on stream client"
            )
            assertNotNull(
                client.client.pluginOrNull(ContentNegotiation),
                "ContentNegotiation must be installed with custom jsonConfig on stream client"
            )
        }
    }
}
