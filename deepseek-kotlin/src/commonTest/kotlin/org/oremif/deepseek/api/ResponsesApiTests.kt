package org.oremif.deepseek.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ImageDetail
import org.oremif.deepseek.models.ReasoningEffort
import org.oremif.deepseek.models.responses.*
import org.oremif.deepseek.testing.mockEngine
import org.oremif.deepseek.testing.testClient

class ResponsesApiTests {

    /** The example response of the API reference, verbatim. */
    private val responseBody =
        """
        {
            "id": "24778070-1c36-4ae0-a4bd-870afc7fc13e",
            "object": "response",
            "created_at": 1753000000,
            "status": "completed",
            "model": "deepseek-flash",
            "output": [
                {
                    "type": "reasoning",
                    "id": "rs_1",
                    "status": "completed",
                    "content": [
                        {
                            "type": "reasoning_text",
                            "text": "The user greets me. I should reply politely."
                        }
                    ],
                    "summary": []
                },
                {
                    "type": "message",
                    "id": "msg_1",
                    "status": "completed",
                    "role": "assistant",
                    "content": [
                        {
                            "type": "output_text",
                            "text": "Hello! How can I help you today?",
                            "annotations": []
                        }
                    ]
                }
            ],
            "usage": {
                "input_tokens": 22,
                "input_tokens_details": {"cached_tokens": 0},
                "output_tokens": 29,
                "output_tokens_details": {"reasoning_tokens": 27},
                "total_tokens": 51
            },
            "store": false,
            "parallel_tool_calls": true,
            "previous_response_id": null,
            "error": null,
            "incomplete_details": null
        }
        """
            .trimIndent()

    private fun MockRequestHandleScope.jsonResponse(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData =
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private fun capturingClient(
        body: String = responseBody,
        capture: suspend (HttpRequestData) -> Unit,
    ) =
        testClient(
            mockEngine { request ->
                capture(request)
                jsonResponse(body)
            }
        )

    @Test
    fun `createResponse posts to the responses endpoint and parses the documented example`() =
        runTest {
            var method: HttpMethod? = null
            var path: String? = null
            val client = capturingClient {
                method = it.method
                path = it.url.encodedPath
            }

            val response = client.createResponse("Hello")

            method shouldBe HttpMethod.Post
            path.shouldNotBeNull().shouldEndWith("/responses")

            response.id shouldBe "24778070-1c36-4ae0-a4bd-870afc7fc13e"
            response.`object` shouldBe "response"
            response.createdAt shouldBe 1753000000L
            response.status shouldBe ResponseStatus.COMPLETED
            response.model shouldBe "deepseek-flash"
            response.error.shouldBeNull()
            response.incompleteDetails.shouldBeNull()

            response.output shouldHaveSize 2
            val reasoning = response.output[0].shouldBeInstanceOf<ReasoningItem>()
            reasoning.id shouldBe "rs_1"
            reasoning.status shouldBe ResponseStatus.COMPLETED
            reasoning.text shouldBe "The user greets me. I should reply politely."

            val message = response.output[1].shouldBeInstanceOf<MessageItem>()
            message.role shouldBe ResponseRole.ASSISTANT
            message.content.shouldBeInstanceOf<ResponseContent.Parts>().parts shouldBe
                listOf(OutputTextPart("Hello! How can I help you today?"))

            response.outputText shouldBe "Hello! How can I help you today?"

            val usage = response.usage.shouldNotBeNull()
            usage.inputTokens shouldBe 22
            usage.inputTokensDetails.shouldNotBeNull().cachedTokens shouldBe 0
            usage.outputTokens shouldBe 29
            usage.outputTokensDetails.shouldNotBeNull().reasoningTokens shouldBe 27
            usage.totalTokens shouldBe 51
        }

    @Test
    fun `a plain input is sent as a string and unset fields are left out`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse("Hi, how are you?")

        val sent = body.shouldNotBeNull()
        sent shouldContain "\"input\":\"Hi, how are you?\""
        sent shouldContain "\"model\":\"deepseek-flash\""
        sent shouldNotContain "instructions"
        sent shouldNotContain "reasoning"
        sent shouldNotContain "stream"
        sent shouldNotContain "tools"
    }

    @Test
    fun `an item list is sent as an array of typed items`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse(
            responseParams { instructions = "You are a helpful assistant." },
            listOf(
                MessageItem(ResponseRole.USER, "What is 6 * 7?"),
                MessageItem(ResponseRole.ASSISTANT, "42"),
                MessageItem(ResponseRole.USER, "And 6 * 8?"),
            ),
        )

        val sent = body.shouldNotBeNull()
        sent shouldContain "\"instructions\":\"You are a helpful assistant.\""
        sent shouldContain "\"type\":\"message\""
        sent shouldContain "\"role\":\"user\""
        sent shouldContain "\"content\":\"What is 6 * 7?\""
        sent shouldContain "\"role\":\"assistant\""
    }

    @Test
    fun `reasoning effort and the output ceiling travel in their documented places`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse(
            responseParams {
                model = ChatModel.DEEPSEEK_V4_PRO
                reasoning = ReasoningConfig(ReasoningEffort.NONE)
                maxOutputTokens = 2048
                temperature = 0.7
                topP = 0.9
                topLogprobs = 5
                user = "user-42"
            },
            "Hello",
        )

        val sent = body.shouldNotBeNull()
        // `reasoning` nests only `effort`; `max_output_tokens` is its top-level sibling.
        sent shouldContain "\"reasoning\":{\"effort\":\"none\"}"
        sent shouldContain "\"max_output_tokens\":2048"
        sent shouldContain "\"temperature\":0.7"
        sent shouldContain "\"top_p\":0.9"
        sent shouldContain "\"top_logprobs\":5"
        sent shouldContain "\"user\":\"user-42\""
    }

    @Test
    fun `tools are sent flat and a named tool choice picks one of them`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse(
            responseParams {
                tools =
                    listOf(
                        FunctionTool(
                            name = "get_weather",
                            description = "Get the weather for a city",
                            parameters =
                                buildJsonObject {
                                    put("type", "object")
                                    putJsonObject("properties") {
                                        putJsonObject("city") { put("type", "string") }
                                    }
                                },
                        ),
                        WebSearchTool.webSearch,
                    )
                toolChoice = NamedResponseToolChoice.function("get_weather")
            },
            "What is the weather in Tokyo?",
        )

        val sent = body.shouldNotBeNull()
        sent shouldContain "\"type\":\"function\",\"name\":\"get_weather\""
        sent shouldContain "\"type\":\"web_search\""
        sent shouldContain "\"tool_choice\":{\"type\":\"function\",\"name\":\"get_weather\"}"
    }

    @Test
    fun `a coarse tool choice is sent as a bare string`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse(
            responseParams {
                tools = listOf(FunctionTool("get_weather"))
                toolChoice = ResponseToolChoiceMode.REQUIRED
            },
            "Weather?",
        )

        body.shouldNotBeNull() shouldContain "\"tool_choice\":\"required\""
    }

    @Test
    fun `structured output travels as a json_schema text format`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse(
            responseParams {
                text =
                    TextConfig.jsonSchema(
                        name = "weather",
                        schema =
                            buildJsonObject {
                                put("type", "object")
                                putJsonObject("properties") {
                                    putJsonObject("temp_c") { put("type", "number") }
                                }
                            },
                    )
            },
            "Weather in Tokyo?",
        )

        val sent = body.shouldNotBeNull()
        sent shouldContain "\"text\":{\"format\":{\"type\":\"json_schema\",\"name\":\"weather\""
        sent shouldContain "\"schema\":{\"type\":\"object\""
    }

    @Test
    fun `the input DSL builds a multimodal user message`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse {
            params { model = ChatModel.DEEPSEEK_FLASH }
            input {
                user {
                    text("What is in this image?")
                    image("https://example.com/cat.jpg", ImageDetail.LOW)
                    imageFile("file-api-abc123")
                }
            }
        }

        val sent = body.shouldNotBeNull()
        sent shouldContain "\"model\":\"deepseek-flash\""
        sent shouldContain "\"type\":\"input_text\",\"text\":\"What is in this image?\""
        sent shouldContain
            "\"type\":\"input_image\",\"image_url\":\"https://example.com/cat.jpg\",\"detail\":\"low\""
        sent shouldContain "\"type\":\"input_image\",\"file_id\":\"file-api-abc123\""
    }

    @Test
    fun `a function call is parsed and its arguments read back as JSON`() = runTest {
        val client =
            testClient(
                mockEngine {
                    jsonResponse(
                        """
                        {
                            "id": "resp-1",
                            "object": "response",
                            "created_at": 1753000000,
                            "status": "completed",
                            "model": "deepseek-v4-pro",
                            "output": [
                                {
                                    "type": "function_call",
                                    "id": "fc_item_1",
                                    "status": "completed",
                                    "call_id": "call_abc",
                                    "name": "get_weather",
                                    "arguments": "{\"city\": \"Tokyo\"}"
                                }
                            ]
                        }
                        """
                            .trimIndent()
                    )
                }
            )

        val response = client.createResponse("Weather in Tokyo?")

        response.outputText shouldBe ""
        val call = response.functionCalls.single()
        call.id shouldBe "fc_item_1"
        call.callId shouldBe "call_abc"
        call.name shouldBe "get_weather"
        call.arguments shouldBe """{"city": "Tokyo"}"""
        call.argumentsAsJsonOrNull()?.get("city")?.jsonPrimitive?.content shouldBe "Tokyo"
    }

    @Test
    fun `a web search call and an unfamiliar item both survive parsing`() = runTest {
        val client =
            testClient(
                mockEngine {
                    jsonResponse(
                        """
                        {
                            "id": "resp-2",
                            "object": "response",
                            "created_at": 1753000000,
                            "status": "completed",
                            "model": "deepseek-flash",
                            "output": [
                                {
                                    "type": "web_search_call",
                                    "id": "ws_1",
                                    "status": "completed",
                                    "action": {"type": "search", "query": "kotlin 2.4"}
                                },
                                {"type": "image_generation_call", "id": "ig_1", "result": "..."},
                                {
                                    "type": "message",
                                    "id": "msg_1",
                                    "status": "completed",
                                    "role": "assistant",
                                    "content": [{"type": "output_text", "text": "Kotlin 2.4."}]
                                }
                            ]
                        }
                        """
                            .trimIndent()
                    )
                }
            )

        val response = client.createResponse("What is the newest Kotlin?")

        response.output shouldHaveSize 3
        val search = response.output[0].shouldBeInstanceOf<WebSearchCallItem>()
        search.id shouldBe "ws_1"
        search.action.shouldNotBeNull()["query"]?.jsonPrimitive?.content shouldBe "kotlin 2.4"

        val unknown = response.output[1].shouldBeInstanceOf<UnknownItem>()
        unknown.type shouldBe "image_generation_call"
        unknown.raw["id"]?.jsonPrimitive?.content shouldBe "ig_1"

        response.outputText shouldBe "Kotlin 2.4."
    }

    @Test
    fun `an incomplete response reports why it stopped`() = runTest {
        val client =
            testClient(
                mockEngine {
                    jsonResponse(
                        """
                        {
                            "id": "resp-3",
                            "object": "response",
                            "created_at": 1753000000,
                            "status": "incomplete",
                            "incomplete_details": {"reason": "max_output_tokens"},
                            "model": "deepseek-flash",
                            "output": []
                        }
                        """
                            .trimIndent()
                    )
                }
            )

        val response = client.createResponse("Write a novel")

        response.status shouldBe ResponseStatus.INCOMPLETE
        response.incompleteDetails.shouldNotBeNull().reason shouldBe
            IncompleteReason.MAX_OUTPUT_TOKENS
        response.usage.shouldBeNull()
    }

    @Test
    fun `a failed response reports the error object`() = runTest {
        val client =
            testClient(
                mockEngine {
                    jsonResponse(
                        """
                        {
                            "id": "resp-4",
                            "object": "response",
                            "created_at": 1753000000,
                            "status": "failed",
                            "error": {"code": "server_error", "message": "Something went wrong"},
                            "model": "deepseek-flash",
                            "output": []
                        }
                        """
                            .trimIndent()
                    )
                }
            )

        val response = client.createResponse("Hello")

        response.status shouldBe ResponseStatus.FAILED
        val error = response.error.shouldNotBeNull()
        error.code shouldBe "server_error"
        error.message shouldBe "Something went wrong"
    }

    @Test
    fun `output items replay verbatim as the input of the next turn`() = runTest {
        var lastBody: String? = null
        val client =
            testClient(
                mockEngine { request ->
                    lastBody = request.body.toByteArray().decodeToString()
                    jsonResponse(responseBody)
                }
            )
        val params = responseParams { instructions = "You are a helpful assistant." }

        val first = client.createResponse(params, "Hello")
        client.createResponse(params) {
            user("Hello")
            items(first.output)
            user("And now in French?")
        }

        val sent = lastBody.shouldNotBeNull()
        sent shouldContain "\"type\":\"reasoning\""
        sent shouldContain "\"type\":\"reasoning_text\""
        sent shouldContain "\"id\":\"rs_1\""
        sent shouldContain "\"type\":\"output_text\""
        sent shouldContain "\"And now in French?\""
    }

    @Test
    fun `a tool result carrying an image is sent as content parts`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse(responseParams { model = ChatModel.DEEPSEEK_FLASH }) {
            user("Read the screenshot the tool returned.")
            functionCall("fc1", "take_screenshot", "{}")
            functionCallOutput("fc1") {
                text("here it is")
                image("data:image/png;base64,AAAA")
            }
        }

        val sent = body.shouldNotBeNull()
        sent shouldContain "\"type\":\"function_call\",\"call_id\":\"fc1\""
        sent shouldContain "\"type\":\"function_call_output\",\"call_id\":\"fc1\""
        sent shouldContain "\"type\":\"input_image\",\"image_url\":\"data:image/png;base64,AAAA\""
    }

    @Test
    fun `streaming parameters reused for a unary call stop asking for a stream`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse(responseStreamParams { model = ChatModel.DEEPSEEK_FLASH }, "Hi")

        body.shouldNotBeNull() shouldContain "\"stream\":false"
    }

    @Test
    fun `a request needs at least an input or instructions`() = runTest {
        shouldThrow<IllegalArgumentException> { responseParams {}.createRequest() }

        responseParams { instructions = "Introduce yourself." }
            .createRequest()
            .instructions shouldBe "Introduce yourself."
    }

    @Test
    fun `parameters are validated on build and on copy`() {
        shouldThrow<IllegalArgumentException> { responseParams { maxOutputTokens = 0 } }
        shouldThrow<IllegalArgumentException> { responseParams { temperature = 2.5 } }
        shouldThrow<IllegalArgumentException> { responseParams { topP = 1.5 } }
        shouldThrow<IllegalArgumentException> { responseParams { topLogprobs = 21 } }
        shouldThrow<IllegalArgumentException> { responseParams { user = "bad id!" } }
        shouldThrow<IllegalArgumentException> {
            responseParams { tools = listOf(FunctionTool("f"), FunctionTool("f")) }
        }
        shouldThrow<IllegalArgumentException> { responseParams {}.copy(temperature = 3.0) }
    }

    @Test
    fun `a function tool name must match the documented charset and length`() {
        shouldThrow<IllegalArgumentException> { FunctionTool("get weather") }
        shouldThrow<IllegalArgumentException> { FunctionTool("") }
        shouldThrow<IllegalArgumentException> { FunctionTool("f".repeat(129)) }
        FunctionTool("get_weather-2").name shouldBe "get_weather-2"
    }

    @Test
    fun `an image part must carry exactly one source`() {
        shouldThrow<IllegalArgumentException> { InputImagePart() }
        shouldThrow<IllegalArgumentException> {
            InputImagePart(imageUrl = "https://example.com/a.jpg", fileId = "file-api-1")
        }
    }

    @Test
    fun `the input DSL rejects an empty conversation`() {
        shouldThrow<IllegalArgumentException> { ResponseRequest.InputBuilder().build() }
        shouldThrow<IllegalArgumentException> { ResponseRequest.ContentBuilder().build() }
    }

    @Test
    fun `an unfamiliar item replays byte for byte`() = runTest {
        var body: String? = null
        val client = capturingClient { body = it.body.toByteArray().decodeToString() }

        client.createResponse {
            input {
                item(
                    UnknownItem(
                        "image_generation_call",
                        buildJsonObject {
                            put("type", "image_generation_call")
                            put("id", "ig_1")
                            put("result", "AAAA")
                        },
                    )
                )
            }
        }

        val sent = body.shouldNotBeNull()
        sent shouldContain """{"type":"image_generation_call","id":"ig_1","result":"AAAA"}"""
    }

    @Test
    fun `outputText also reads a message that came back as a plain string`() = runTest {
        val client =
            testClient(
                mockEngine {
                    jsonResponse(
                        """
                        {
                            "id": "resp-5",
                            "object": "response",
                            "created_at": 1753000000,
                            "status": "completed",
                            "model": "deepseek-flash",
                            "output": [
                                {"type": "message", "role": "assistant", "content": "Plain."}
                            ]
                        }
                        """
                            .trimIndent()
                    )
                }
            )

        val response = client.createResponse("Hello")

        response.output
            .single()
            .shouldBeInstanceOf<MessageItem>()
            .content
            .shouldBeInstanceOf<ResponseContent.Text>()
            .text shouldBe "Plain."
        response.outputText shouldBe "Plain."
    }

    @Test
    fun `createResponse maps 400 to BadRequestException with the parsed error`() = runTest {
        val client =
            testClient(
                mockEngine {
                    jsonResponse(
                        """{"error": {"message": "Invalid model", "type": "invalid_request_error"}}""",
                        HttpStatusCode.BadRequest,
                    )
                }
            )

        val ex =
            shouldThrow<DeepSeekException.BadRequestException> { client.createResponse("Hello") }

        ex.statusCode shouldBe 400
        ex.error?.error?.message shouldBe "Invalid model"
    }
}
