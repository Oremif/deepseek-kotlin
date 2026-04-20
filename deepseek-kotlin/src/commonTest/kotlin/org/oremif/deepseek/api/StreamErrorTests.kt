package org.oremif.deepseek.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeInstanceOf
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.ChatCompletionRequest
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.FIMCompletionRequest
import org.oremif.deepseek.models.UserMessage
import org.oremif.deepseek.testing.sseMockEngine
import org.oremif.deepseek.testing.testStreamClient
import kotlin.test.Test

class StreamErrorTests {

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
        val client = testStreamClient(engine)
        val ex = shouldThrow<DeepSeekException.BadRequestException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        ex.statusCode shouldBe 400
        val error = ex.error.shouldNotBeNull()
        error.error.message shouldBe "Invalid model"
        error.error.type shouldBe "invalid_request_error"
        error.error.code shouldBe "model_not_found"
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
        val client = testStreamClient(engine)
        val ex = shouldThrow<DeepSeekException.UnauthorizedException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        ex.statusCode shouldBe 401
        ex.error?.error?.message shouldBe "Invalid API key"
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
        val client = testStreamClient(engine)
        val ex = shouldThrow<DeepSeekException.OverloadServerException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        ex.statusCode shouldBe 503
        ex.error?.error?.message shouldBe "Server overloaded"
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
        val client = testStreamClient(engine)
        val ex = shouldThrow<DeepSeekException.InternalServerException> {
            client.chatCompletionStream(chatRequest).toList()
        }
        ex.statusCode shouldBe 500
        ex.error.shouldBeNull()
    }

    @Test
    fun `chat stream network error before response propagates instead of being swallowed`() = runTest {
        val engine = sseMockEngine {
            throw RuntimeException("Network unreachable")
        }
        val client = testStreamClient(engine)
        val thrown = shouldThrow<Throwable> {
            client.chatCompletionStream(chatRequest).toList()
        }
        thrown.shouldNotBeInstanceOf<DeepSeekException>()
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
        val client = testStreamClient(engine)
        val ex = shouldThrow<DeepSeekException.BadRequestException> {
            client.fimCompletionStream(fimRequest).toList()
        }
        ex.statusCode shouldBe 400
        ex.error?.error?.message shouldBe "Bad FIM prompt"
    }

    @Test
    fun `fim stream network error before response propagates instead of being swallowed`() = runTest {
        val engine = sseMockEngine {
            throw RuntimeException("Network unreachable")
        }
        val client = testStreamClient(engine)
        val thrown = shouldThrow<Throwable> {
            client.fimCompletionStream(fimRequest).toList()
        }
        thrown.shouldNotBeInstanceOf<DeepSeekException>()
    }
}
