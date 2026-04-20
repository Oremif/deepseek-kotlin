package org.oremif.deepseek.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.sse.SSE
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
}
