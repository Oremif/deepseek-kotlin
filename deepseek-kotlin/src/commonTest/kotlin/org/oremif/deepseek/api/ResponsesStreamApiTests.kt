package org.oremif.deepseek.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.oremif.deepseek.errors.DeepSeekException
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.responses.*
import org.oremif.deepseek.testing.sseMockEngine
import org.oremif.deepseek.testing.testStreamClient
import kotlin.test.Test

class ResponsesStreamApiTests {

    /** Renders events the way the API does: a named `event:` line followed by its `data:` line. */
    private fun sseBody(vararg events: Pair<String, String>): String =
        events.joinToString(separator = "") { (name, data) -> "event: $name\ndata: $data\n\n" }

    private fun streamingClient(body: String) =
        testStreamClient(
            sseMockEngine {
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers =
                        headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
                )
            }
        )

    private fun response(status: String, extra: String = "") =
        """{"id":"resp-1","object":"response","created_at":1753000000,"status":"$status","model":"deepseek-flash","output":[]$extra}"""

    private val completedResponse =
        """{"id":"resp-1","object":"response","created_at":1753000000,"status":"completed","model":"deepseek-flash",""" +
            """"output":[{"type":"message","id":"msg_1","status":"completed","role":"assistant","content":[{"type":"output_text","text":"Hello!"}]}],""" +
            """"usage":{"input_tokens":22,"input_tokens_details":{"cached_tokens":0},"output_tokens":29,"output_tokens_details":{"reasoning_tokens":27},"total_tokens":51}}"""

    @Test
    fun `the stream narrates reasoning and answer and ends without a DONE marker`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.created" to
                        """{"type":"response.created","sequence_number":0,"response":${response("in_progress")}}""",
                    "response.in_progress" to
                        """{"type":"response.in_progress","sequence_number":1,"response":${response("in_progress")}}""",
                    "response.output_item.added" to
                        """{"type":"response.output_item.added","sequence_number":2,"output_index":0,"item":{"type":"reasoning","id":"rs_1","status":"in_progress","content":[]}}""",
                    "response.content_part.added" to
                        """{"type":"response.content_part.added","sequence_number":3,"item_id":"rs_1","output_index":0,"content_index":0,"part":{"type":"reasoning_text","text":""}}""",
                    "response.reasoning_text.delta" to
                        """{"type":"response.reasoning_text.delta","sequence_number":4,"item_id":"rs_1","output_index":0,"content_index":0,"delta":"The user"}""",
                    "response.reasoning_text.delta" to
                        """{"type":"response.reasoning_text.delta","sequence_number":5,"item_id":"rs_1","output_index":0,"content_index":0,"delta":" greets me."}""",
                    "response.reasoning_text.done" to
                        """{"type":"response.reasoning_text.done","sequence_number":6,"item_id":"rs_1","output_index":0,"content_index":0,"text":"The user greets me."}""",
                    "response.content_part.done" to
                        """{"type":"response.content_part.done","sequence_number":7,"item_id":"rs_1","output_index":0,"content_index":0,"part":{"type":"reasoning_text","text":"The user greets me."}}""",
                    "response.output_item.done" to
                        """{"type":"response.output_item.done","sequence_number":8,"output_index":0,"item":{"type":"reasoning","id":"rs_1","status":"completed","content":[{"type":"reasoning_text","text":"The user greets me."}]}}""",
                    "response.output_item.added" to
                        """{"type":"response.output_item.added","sequence_number":9,"output_index":1,"item":{"type":"message","id":"msg_1","status":"in_progress","role":"assistant","content":[]}}""",
                    "response.output_text.delta" to
                        """{"type":"response.output_text.delta","sequence_number":10,"item_id":"msg_1","output_index":1,"content_index":0,"delta":"Hello"}""",
                    "response.output_text.delta" to
                        """{"type":"response.output_text.delta","sequence_number":11,"item_id":"msg_1","output_index":1,"content_index":0,"delta":"!"}""",
                    "response.output_text.done" to
                        """{"type":"response.output_text.done","sequence_number":12,"item_id":"msg_1","output_index":1,"content_index":0,"text":"Hello!"}""",
                    "response.completed" to
                        """{"type":"response.completed","sequence_number":13,"response":$completedResponse}""",
                )
            )

        val events = client.createResponse("Hello").toList()

        events shouldHaveSize 14
        events.map { it.sequenceNumber } shouldBe (0..13).toList()

        events[0].shouldBeInstanceOf<ResponseCreatedEvent>().isTerminal.shouldBeFalse()
        events[1].shouldBeInstanceOf<ResponseInProgressEvent>()

        val itemAdded = events[2].shouldBeInstanceOf<OutputItemAddedEvent>()
        itemAdded.outputIndex shouldBe 0
        itemAdded.item.shouldBeInstanceOf<ReasoningItem>().id shouldBe "rs_1"

        val partAdded = events[3].shouldBeInstanceOf<ContentPartAddedEvent>()
        partAdded.itemId shouldBe "rs_1"
        partAdded.contentIndex shouldBe 0
        partAdded.part.shouldBeInstanceOf<ReasoningTextPart>()

        events.filterIsInstance<ReasoningTextDeltaEvent>().joinToString("") { it.delta } shouldBe
            "The user greets me."
        events.filterIsInstance<ReasoningTextDoneEvent>().single().text shouldBe
            "The user greets me."
        events[7].shouldBeInstanceOf<ContentPartDoneEvent>()
        events[8].shouldBeInstanceOf<OutputItemDoneEvent>()

        events.filterIsInstance<OutputTextDeltaEvent>().joinToString("") { it.delta } shouldBe
            "Hello!"
        val textDone = events.filterIsInstance<OutputTextDoneEvent>().single()
        textDone.itemId shouldBe "msg_1"
        textDone.outputIndex shouldBe 1
        textDone.text shouldBe "Hello!"

        val completed = events.last().shouldBeInstanceOf<ResponseCompletedEvent>()
        completed.isTerminal.shouldBeTrue()
        completed.response.outputText shouldBe "Hello!"
        completed.response.usage.shouldNotBeNull().totalTokens shouldBe 51
    }

    @Test
    fun `the streaming request asks the API for a stream`() = runTest {
        var body: String? = null
        val client =
            testStreamClient(
                sseMockEngine { request ->
                    body = request.body.toByteArray().decodeToString()
                    respond(
                        content =
                            sseBody(
                                "response.completed" to
                                    """{"type":"response.completed","sequence_number":0,"response":$completedResponse}"""
                            ),
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Text.EventStream.toString(),
                            ),
                    )
                }
            )

        client
            .createResponse {
                params { model = ChatModel.DEEPSEEK_V4_PRO }
                input { user("Hello") }
            }
            .toList() shouldHaveSize 1

        val sent = body.shouldNotBeNull()
        sent shouldContain "\"stream\":true"
        sent shouldContain "\"model\":\"deepseek-v4-pro\""
    }

    @Test
    fun `function call arguments arrive as deltas and a done event`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.output_item.added" to
                        """{"type":"response.output_item.added","sequence_number":0,"output_index":0,"item":{"type":"function_call","id":"fc_1","status":"in_progress","call_id":"call_abc","name":"get_weather","arguments":""}}""",
                    "response.function_call_arguments.delta" to
                        """{"type":"response.function_call_arguments.delta","sequence_number":1,"item_id":"fc_1","output_index":0,"delta":"{\"city\":"}""",
                    "response.function_call_arguments.delta" to
                        """{"type":"response.function_call_arguments.delta","sequence_number":2,"item_id":"fc_1","output_index":0,"delta":"\"Tokyo\"}"}""",
                    "response.function_call_arguments.done" to
                        """{"type":"response.function_call_arguments.done","sequence_number":3,"item_id":"fc_1","output_index":0,"arguments":"{\"city\":\"Tokyo\"}"}""",
                )
            )

        val events = client.createResponse("Weather in Tokyo?").toList()

        events.filterIsInstance<FunctionCallArgumentsDeltaEvent>().joinToString("") {
            it.delta
        } shouldBe """{"city":"Tokyo"}"""
        val done = events.last().shouldBeInstanceOf<FunctionCallArgumentsDoneEvent>()
        done.itemId shouldBe "fc_1"
        done.arguments shouldBe """{"city":"Tokyo"}"""
        done.toolInput shouldBe done.arguments
    }

    @Test
    fun `custom tool input arrives under its own field name`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.custom_tool_call_input.delta" to
                        """{"type":"response.custom_tool_call_input.delta","sequence_number":0,"item_id":"ct_1","output_index":0,"delta":"*** Begin"}""",
                    "response.custom_tool_call_input.done" to
                        """{"type":"response.custom_tool_call_input.done","sequence_number":1,"item_id":"ct_1","output_index":0,"input":"*** Begin Patch"}""",
                )
            )

        val events = client.createResponse("Patch it").toList()

        events[0].shouldBeInstanceOf<CustomToolCallInputDeltaEvent>().delta shouldBe "*** Begin"
        val done = events[1].shouldBeInstanceOf<CustomToolCallInputDoneEvent>()
        done.input shouldBe "*** Begin Patch"
        done.toolInput shouldBe "*** Begin Patch"
    }

    @Test
    fun `web search status updates are typed`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.web_search_call.in_progress" to
                        """{"type":"response.web_search_call.in_progress","sequence_number":0,"item_id":"ws_1","output_index":0}""",
                    "response.web_search_call.searching" to
                        """{"type":"response.web_search_call.searching","sequence_number":1,"item_id":"ws_1","output_index":0}""",
                    "response.web_search_call.completed" to
                        """{"type":"response.web_search_call.completed","sequence_number":2,"item_id":"ws_1","output_index":0}""",
                )
            )

        val events = client.createResponse("What is new?").toList()

        events shouldHaveSize 3
        events.forEach { it.shouldBeInstanceOf<WebSearchCallEvent>().itemId shouldBe "ws_1" }
        events[0].shouldBeInstanceOf<WebSearchCallInProgressEvent>()
        events[1].shouldBeInstanceOf<WebSearchCallSearchingEvent>()
        events[2].shouldBeInstanceOf<WebSearchCallCompletedEvent>()
    }

    @Test
    fun `an incomplete stream ends with the truncated response`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.incomplete" to
                        """{"type":"response.incomplete","sequence_number":0,"response":${
                            response("incomplete", ""","incomplete_details":{"reason":"max_output_tokens"}""")
                        }}"""
                )
            )

        val event = client.createResponse("Write a novel").toList().single()

        val incomplete = event.shouldBeInstanceOf<ResponseIncompleteEvent>()
        incomplete.isTerminal.shouldBeTrue()
        incomplete.response.incompleteDetails.shouldNotBeNull().reason shouldBe
            IncompleteReason.MAX_OUTPUT_TOKENS
    }

    @Test
    fun `a failed stream ends with the error carried by the response`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.failed" to
                        """{"type":"response.failed","sequence_number":0,"response":${
                            response("failed", ""","error":{"code":"server_error","message":"boom"}""")
                        }}"""
                )
            )

        val failed =
            client
                .createResponse("Hello")
                .toList()
                .single()
                .shouldBeInstanceOf<ResponseFailedEvent>()

        failed.isTerminal.shouldBeTrue()
        failed.response.error.shouldNotBeNull().message shouldBe "boom"
    }

    @Test
    fun `an event the SDK does not model is preserved instead of breaking the stream`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.image_generation_call.partial_image" to
                        """{"type":"response.image_generation_call.partial_image","sequence_number":0,"item_id":"ig_1","partial_image_b64":"AAAA"}""",
                    "response.output_text.delta" to
                        """{"type":"response.output_text.delta","sequence_number":1,"item_id":"msg_1","output_index":0,"content_index":0,"delta":"Hi"}""",
                )
            )

        val events = client.createResponse("Hello").toList()

        val unknown = events[0].shouldBeInstanceOf<UnknownResponseStreamEvent>()
        unknown.type shouldBe "response.image_generation_call.partial_image"
        unknown.sequenceNumber shouldBe 0
        unknown.raw["partial_image_b64"]?.jsonPrimitive?.content shouldBe "AAAA"
        events[1].shouldBeInstanceOf<OutputTextDeltaEvent>().delta shouldBe "Hi"
    }

    @Test
    fun `a familiar event missing a field it needs also falls back to unknown`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.output_text.delta" to
                        """{"type":"response.output_text.delta","sequence_number":7,"delta":"Hi"}"""
                )
            )

        val unknown =
            client
                .createResponse("Hello")
                .toList()
                .single()
                .shouldBeInstanceOf<UnknownResponseStreamEvent>()

        unknown.type shouldBe "response.output_text.delta"
        unknown.sequenceNumber shouldBe 7
    }

    @Test
    fun `a DONE marker from an OpenAI-compatible proxy is skipped`() = runTest {
        val client =
            streamingClient(
                sseBody(
                    "response.completed" to
                        """{"type":"response.completed","sequence_number":0,"response":$completedResponse}""",
                    "done" to "[DONE]",
                )
            )

        client.createResponse("Hello").toList() shouldHaveSize 1
    }

    @Test
    fun `a non-2xx status becomes a DeepSeekException from the collector`() = runTest {
        val client =
            testStreamClient(
                sseMockEngine {
                    respond(
                        content =
                            """{"error":{"message":"Invalid model","type":"invalid_request_error","code":"model_not_found"}}""",
                        status = HttpStatusCode.BadRequest,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType,
                                ContentType.Application.Json.toString(),
                            ),
                    )
                }
            )

        val ex =
            shouldThrow<DeepSeekException.BadRequestException> {
                client.createResponse("Hello").toList()
            }

        ex.statusCode shouldBe 400
        ex.error.shouldNotBeNull().error.message shouldBe "Invalid model"
    }
}
