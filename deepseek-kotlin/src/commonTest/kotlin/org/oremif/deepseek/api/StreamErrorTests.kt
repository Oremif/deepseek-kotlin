package org.oremif.deepseek.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.oremif.deepseek.client.DeepSeekClientStream
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.ChatCompletionRequest
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.FIMCompletionRequest
import org.oremif.deepseek.models.UserMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class SseMockEngine(config: MockEngineConfig) : MockEngine(config) {
    override val supportedCapabilities: Set<HttpClientEngineCapability<out Any>> =
        super.supportedCapabilities + SSECapability
}

private fun sseMockEngine(handler: MockRequestHandler): SseMockEngine =
    SseMockEngine(MockEngineConfig().apply { requestHandlers.add(handler) })

class StreamErrorTests {

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    private fun streamClient(engine: HttpClientEngine): DeepSeekClientStream {
        val http = HttpClient(engine) {
            install(ContentNegotiation) { json(jsonConfig) }
            install(SSE)
            defaultRequest {
                url("https://api.deepseek.com")
                contentType(ContentType.Application.Json)
            }
        }
        return DeepSeekClientStream {
            jsonConfig(jsonConfig)
            httpClient(http)
        }
    }

    private val chatRequest = ChatCompletionRequest(
        messages = listOf(UserMessage("Hi")),
        model = ChatModel.DEEPSEEK_CHAT,
        stream = true
    )

    private val fimRequest = FIMCompletionRequest(
        model = ChatModel.DEEPSEEK_CHAT,
        prompt = "def foo():",
        stream = true
    )

    @Test
    fun `chat stream 400 with JSON body becomes BadRequestException with parsed error`() = runTest {
        val engine = sseMockEngine {
            respond(
                content = """{"error":{"message":"Invalid model","type":"invalid_request_error","code":"model_not_found"}}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = streamClient(engine)
        val ex = assertFailsWith<DeepSeekException.BadRequestException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        assertEquals(400, ex.statusCode)
        val error = assertNotNull(ex.error, "error body should be parsed")
        assertEquals("Invalid model", error.error.message)
        assertEquals("invalid_request_error", error.error.type)
        assertEquals("model_not_found", error.error.code)
    }

    @Test
    fun `chat stream 401 with JSON body becomes UnauthorizedException`() = runTest {
        val engine = sseMockEngine {
            respond(
                content = """{"error":{"message":"Invalid API key","type":"authentication_error"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = streamClient(engine)
        val ex = assertFailsWith<DeepSeekException.UnauthorizedException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        assertEquals(401, ex.statusCode)
        assertEquals("Invalid API key", ex.error?.error?.message)
    }

    @Test
    fun `chat stream 503 maps to OverloadServerException`() = runTest {
        val engine = sseMockEngine {
            respond(
                content = """{"error":{"message":"Server overloaded"}}""",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = streamClient(engine)
        val ex = assertFailsWith<DeepSeekException.OverloadServerException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        assertEquals(503, ex.statusCode)
        assertEquals("Server overloaded", ex.error?.error?.message)
    }

    @Test
    fun `chat stream 500 with non-JSON body still throws typed exception with null error`() = runTest {
        val engine = sseMockEngine {
            respond(
                content = "Internal server error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }
        val client = streamClient(engine)
        val ex = assertFailsWith<DeepSeekException.InternalServerException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        assertEquals(500, ex.statusCode)
        assertNull(ex.error, "unparsable body must not crash — error should be null")
    }

    @Test
    fun `chat stream network error before response propagates instead of being swallowed`() = runTest {
        val engine = sseMockEngine {
            throw RuntimeException("Network unreachable")
        }
        val client = streamClient(engine)
        val thrown = assertFailsWith<Throwable> {
            client.chatCompletionStream(chatRequest).toList()
        }
        assertTrue(
            thrown !is DeepSeekException,
            "network error must not be wrapped as DeepSeekException when no HTTP response is available"
        )
    }

    @Test
    fun `fim stream 400 with JSON body becomes BadRequestException with parsed error`() = runTest {
        val engine = sseMockEngine {
            respond(
                content = """{"error":{"message":"Bad FIM prompt","type":"invalid_request_error"}}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = streamClient(engine)
        val ex = assertFailsWith<DeepSeekException.BadRequestException> {
            client.fimCompletionStream(fimRequest).toList()
        }
        assertEquals(400, ex.statusCode)
        assertEquals("Bad FIM prompt", ex.error?.error?.message)
    }

    @Test
    fun `fim stream network error before response propagates instead of being swallowed`() = runTest {
        val engine = sseMockEngine {
            throw RuntimeException("Network unreachable")
        }
        val client = streamClient(engine)
        val thrown = assertFailsWith<Throwable> {
            client.fimCompletionStream(fimRequest).toList()
        }
        assertTrue(
            thrown !is DeepSeekException,
            "network error must not be wrapped as DeepSeekException when no HTTP response is available"
        )
    }
}
