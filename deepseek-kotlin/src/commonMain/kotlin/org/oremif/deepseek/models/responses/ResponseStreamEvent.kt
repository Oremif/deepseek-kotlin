package org.oremif.deepseek.models.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * One semantic server-sent event of a streamed Responses API call.
 *
 * The Responses API streams a different shape than `/chat/completions`: instead of chunks that each
 * repeat the whole envelope, it narrates the response as it is assembled — items open and close,
 * content parts open and close, and text arrives as deltas in between. There is no `[DONE]` marker;
 * the stream ends with [ResponseCompletedEvent], [ResponseIncompleteEvent] or
 * [ResponseFailedEvent], each carrying the finished [ModelResponse] with its usage.
 *
 * Example:
 * ```kotlin
 * client.createResponse("Write a haiku").collect { event ->
 *     when (event) {
 *         is OutputTextDeltaEvent -> print(event.delta)
 *         is ResponseCompletedEvent -> println("\n${event.response.usage?.totalTokens} tokens")
 *         else -> {}
 *     }
 * }
 * ```
 *
 * An event the SDK cannot map — an unknown `type`, or a known one whose payload does not fit —
 * arrives as an [UnknownResponseStreamEvent] carrying the raw JSON, so one unexpected event never
 * breaks the stream.
 *
 * @property type Raw event type, as sent in the `type` field.
 * @property sequenceNumber Position of the event in the stream; increases monotonically.
 * @see <a href="https://api-docs.deepseek.com/guides/responses_api">Using the Responses API</a>
 */
@Serializable(with = ResponseStreamEventSerializer::class)
public sealed interface ResponseStreamEvent {
    public val type: String
    public val sequenceNumber: Int
}

/**
 * An event carrying a snapshot of the whole response.
 *
 * @property response The response as it stands: freshly created, in progress, or finished — the
 *   terminal events carry the complete output and [ModelResponse.usage].
 */
public sealed class ResponseSnapshotEvent : ResponseStreamEvent {
    public abstract val response: ModelResponse

    /** Whether this is the last event of the stream. */
    public val isTerminal: Boolean
        get() = type == RESPONSE_COMPLETED || type == RESPONSE_INCOMPLETE || type == RESPONSE_FAILED

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseSnapshotEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            response == other.response
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + response.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, response=$response)"
}

/** The first event of the stream: the response exists and generation has started. */
public class ResponseCreatedEvent(
    override val sequenceNumber: Int,
    override val response: ModelResponse,
) : ResponseSnapshotEvent() {
    override val type: String
        get() = RESPONSE_CREATED
}

/** Progress ping: the response is still being generated. */
public class ResponseInProgressEvent(
    override val sequenceNumber: Int,
    override val response: ModelResponse,
) : ResponseSnapshotEvent() {
    override val type: String
        get() = RESPONSE_IN_PROGRESS
}

/** Terminal event of a response that finished normally, carrying its output and usage. */
public class ResponseCompletedEvent(
    override val sequenceNumber: Int,
    override val response: ModelResponse,
) : ResponseSnapshotEvent() {
    override val type: String
        get() = RESPONSE_COMPLETED
}

/**
 * Terminal event of a response that was cut short — by `max_output_tokens` or the content filter;
 * see [ModelResponse.incompleteDetails].
 */
public class ResponseIncompleteEvent(
    override val sequenceNumber: Int,
    override val response: ModelResponse,
) : ResponseSnapshotEvent() {
    override val type: String
        get() = RESPONSE_INCOMPLETE
}

/** Terminal event of a response that failed; see [ModelResponse.error]. */
public class ResponseFailedEvent(
    override val sequenceNumber: Int,
    override val response: ModelResponse,
) : ResponseSnapshotEvent() {
    override val type: String
        get() = RESPONSE_FAILED
}

/**
 * An event marking the start or the end of one item of [ModelResponse.output].
 *
 * @property outputIndex Position of the item in the output list.
 * @property item The item itself — empty when it opens, complete when it closes.
 */
public sealed class OutputItemEvent : ResponseStreamEvent {
    public abstract val outputIndex: Int
    public abstract val item: ResponseItem

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutputItemEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            outputIndex == other.outputIndex &&
            item == other.item
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + outputIndex
        result = 31 * result + item.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, outputIndex=$outputIndex, item=$item)"
}

/** A new output item — reasoning, message, tool call — has started. */
public class OutputItemAddedEvent(
    override val sequenceNumber: Int,
    override val outputIndex: Int,
    override val item: ResponseItem,
) : OutputItemEvent() {
    override val type: String
        get() = OUTPUT_ITEM_ADDED
}

/** An output item is complete; [item] holds its final form. */
public class OutputItemDoneEvent(
    override val sequenceNumber: Int,
    override val outputIndex: Int,
    override val item: ResponseItem,
) : OutputItemEvent() {
    override val type: String
        get() = OUTPUT_ITEM_DONE
}

/**
 * An event marking the start or the end of one content part inside an output item.
 *
 * @property itemId Identifier of the output item the part belongs to.
 * @property outputIndex Position of that item in the output list.
 * @property contentIndex Position of the part within the item's content.
 * @property part The part itself — empty when it opens, complete when it closes.
 */
public sealed class ContentPartEvent : ResponseStreamEvent {
    public abstract val itemId: String
    public abstract val outputIndex: Int
    public abstract val contentIndex: Int
    public abstract val part: ResponseContentPart

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContentPartEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            itemId == other.itemId &&
            outputIndex == other.outputIndex &&
            contentIndex == other.contentIndex &&
            part == other.part
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + itemId.hashCode()
        result = 31 * result + outputIndex
        result = 31 * result + contentIndex
        result = 31 * result + part.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, itemId='$itemId', " +
            "outputIndex=$outputIndex, contentIndex=$contentIndex, part=$part)"
}

/** A new content part has started inside an output item. */
public class ContentPartAddedEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val contentIndex: Int,
    override val part: ResponseContentPart,
) : ContentPartEvent() {
    override val type: String
        get() = CONTENT_PART_ADDED
}

/** A content part is complete; [part] holds its final form. */
public class ContentPartDoneEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val contentIndex: Int,
    override val part: ResponseContentPart,
) : ContentPartEvent() {
    override val type: String
        get() = CONTENT_PART_DONE
}

/**
 * An incremental piece of text produced inside a content part.
 *
 * @property itemId Identifier of the output item the text belongs to.
 * @property outputIndex Position of that item in the output list.
 * @property contentIndex Position of the part within the item's content.
 * @property delta The text fragment; concatenating the deltas of one part rebuilds it.
 */
public sealed class TextDeltaEvent : ResponseStreamEvent {
    public abstract val itemId: String
    public abstract val outputIndex: Int
    public abstract val contentIndex: Int
    public abstract val delta: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextDeltaEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            itemId == other.itemId &&
            outputIndex == other.outputIndex &&
            contentIndex == other.contentIndex &&
            delta == other.delta
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + itemId.hashCode()
        result = 31 * result + outputIndex
        result = 31 * result + contentIndex
        result = 31 * result + delta.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, itemId='$itemId', " +
            "outputIndex=$outputIndex, contentIndex=$contentIndex, delta='$delta')"
}

/** A fragment of the visible answer. This is the event most callers print as it arrives. */
public class OutputTextDeltaEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val contentIndex: Int,
    override val delta: String,
) : TextDeltaEvent() {
    override val type: String
        get() = OUTPUT_TEXT_DELTA
}

/** A fragment of the chain-of-thought a thinking model produces before its answer. */
public class ReasoningTextDeltaEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val contentIndex: Int,
    override val delta: String,
) : TextDeltaEvent() {
    override val type: String
        get() = REASONING_TEXT_DELTA
}

/**
 * The complete text of a content part, sent once its deltas are done.
 *
 * @property itemId Identifier of the output item the text belongs to.
 * @property outputIndex Position of that item in the output list.
 * @property contentIndex Position of the part within the item's content.
 * @property text The full text, equal to the concatenation of that part's deltas.
 */
public sealed class TextDoneEvent : ResponseStreamEvent {
    public abstract val itemId: String
    public abstract val outputIndex: Int
    public abstract val contentIndex: Int
    public abstract val text: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextDoneEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            itemId == other.itemId &&
            outputIndex == other.outputIndex &&
            contentIndex == other.contentIndex &&
            text == other.text
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + itemId.hashCode()
        result = 31 * result + outputIndex
        result = 31 * result + contentIndex
        result = 31 * result + text.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, itemId='$itemId', " +
            "outputIndex=$outputIndex, contentIndex=$contentIndex, text='$text')"
}

/** The visible answer of one content part, complete. */
public class OutputTextDoneEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val contentIndex: Int,
    override val text: String,
) : TextDoneEvent() {
    override val type: String
        get() = OUTPUT_TEXT_DONE
}

/** The chain-of-thought of one content part, complete. */
public class ReasoningTextDoneEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val contentIndex: Int,
    override val text: String,
) : TextDoneEvent() {
    override val type: String
        get() = REASONING_TEXT_DONE
}

/**
 * An incremental piece of the input the model is generating for a tool call.
 *
 * @property itemId Identifier of the tool-call item being built.
 * @property outputIndex Position of that item in the output list.
 * @property delta The fragment; concatenating the deltas rebuilds the call's arguments or input.
 */
public sealed class ToolInputDeltaEvent : ResponseStreamEvent {
    public abstract val itemId: String
    public abstract val outputIndex: Int
    public abstract val delta: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ToolInputDeltaEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            itemId == other.itemId &&
            outputIndex == other.outputIndex &&
            delta == other.delta
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + itemId.hashCode()
        result = 31 * result + outputIndex
        result = 31 * result + delta.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, itemId='$itemId', " +
            "outputIndex=$outputIndex, delta='$delta')"
}

/** A fragment of the JSON arguments the model is generating for a [FunctionCallItem]. */
public class FunctionCallArgumentsDeltaEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val delta: String,
) : ToolInputDeltaEvent() {
    override val type: String
        get() = FUNCTION_CALL_ARGUMENTS_DELTA
}

/** A fragment of the free-form input the model is generating for a [CustomToolCallItem]. */
public class CustomToolCallInputDeltaEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    override val delta: String,
) : ToolInputDeltaEvent() {
    override val type: String
        get() = CUSTOM_TOOL_CALL_INPUT_DELTA
}

/**
 * The complete input of a tool call, sent once its deltas are done.
 *
 * @property itemId Identifier of the tool-call item.
 * @property outputIndex Position of that item in the output list.
 * @property toolInput The finished input, spelled `arguments` for a function call and `input` for a
 *   custom tool call.
 */
public sealed class ToolInputDoneEvent : ResponseStreamEvent {
    public abstract val itemId: String
    public abstract val outputIndex: Int
    public abstract val toolInput: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ToolInputDoneEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            itemId == other.itemId &&
            outputIndex == other.outputIndex &&
            toolInput == other.toolInput
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + itemId.hashCode()
        result = 31 * result + outputIndex
        result = 31 * result + toolInput.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, itemId='$itemId', " +
            "outputIndex=$outputIndex, toolInput='$toolInput')"
}

/**
 * The finished JSON arguments of a function call.
 *
 * @property arguments Arguments as a raw JSON string; the same value the matching
 *   [FunctionCallItem] carries.
 */
public class FunctionCallArgumentsDoneEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    public val arguments: String,
) : ToolInputDoneEvent() {
    override val type: String
        get() = FUNCTION_CALL_ARGUMENTS_DONE

    override val toolInput: String
        get() = arguments
}

/**
 * The finished input of a custom tool call.
 *
 * @property input Raw input; the same value the matching [CustomToolCallItem] carries.
 */
public class CustomToolCallInputDoneEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
    public val input: String,
) : ToolInputDoneEvent() {
    override val type: String
        get() = CUSTOM_TOOL_CALL_INPUT_DONE

    override val toolInput: String
        get() = input
}

/**
 * A status update of a web search the API is running server-side.
 *
 * @property itemId Identifier of the [WebSearchCallItem] the update belongs to.
 * @property outputIndex Position of that item in the output list.
 */
public sealed class WebSearchCallEvent : ResponseStreamEvent {
    public abstract val itemId: String
    public abstract val outputIndex: Int

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSearchCallEvent) return false
        return type == other.type &&
            sequenceNumber == other.sequenceNumber &&
            itemId == other.itemId &&
            outputIndex == other.outputIndex
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + itemId.hashCode()
        result = 31 * result + outputIndex
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(sequenceNumber=$sequenceNumber, itemId='$itemId', outputIndex=$outputIndex)"
}

/** A web search call has been created and is about to run. */
public class WebSearchCallInProgressEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
) : WebSearchCallEvent() {
    override val type: String
        get() = WEB_SEARCH_CALL_IN_PROGRESS
}

/** The web search is running. */
public class WebSearchCallSearchingEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
) : WebSearchCallEvent() {
    override val type: String
        get() = WEB_SEARCH_CALL_SEARCHING
}

/** The web search has finished and its results are feeding back into the model. */
public class WebSearchCallCompletedEvent(
    override val sequenceNumber: Int,
    override val itemId: String,
    override val outputIndex: Int,
) : WebSearchCallEvent() {
    override val type: String
        get() = WEB_SEARCH_CALL_COMPLETED
}

/**
 * An event this version of the SDK cannot map: an unknown `type`, or a known one whose payload does
 * not fit the expected shape.
 *
 * Keeps the payload verbatim so the stream survives a schema the SDK has not caught up with.
 *
 * @property type Raw event type the API sent.
 * @property sequenceNumber Position of the event in the stream, `0` if the API omitted it.
 * @property raw The event exactly as it arrived.
 */
public class UnknownResponseStreamEvent(
    override val type: String,
    override val sequenceNumber: Int,
    public val raw: JsonObject,
) : ResponseStreamEvent {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnknownResponseStreamEvent) return false
        return type == other.type && sequenceNumber == other.sequenceNumber && raw == other.raw
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + raw.hashCode()
        return result
    }

    override fun toString(): String =
        "UnknownResponseStreamEvent(type='$type', sequenceNumber=$sequenceNumber, raw=$raw)"
}

internal const val RESPONSE_CREATED: String = "response.created"
internal const val RESPONSE_IN_PROGRESS: String = "response.in_progress"
internal const val RESPONSE_COMPLETED: String = "response.completed"
internal const val RESPONSE_INCOMPLETE: String = "response.incomplete"
internal const val RESPONSE_FAILED: String = "response.failed"
internal const val OUTPUT_ITEM_ADDED: String = "response.output_item.added"
internal const val OUTPUT_ITEM_DONE: String = "response.output_item.done"
internal const val CONTENT_PART_ADDED: String = "response.content_part.added"
internal const val CONTENT_PART_DONE: String = "response.content_part.done"
internal const val REASONING_TEXT_DELTA: String = "response.reasoning_text.delta"
internal const val REASONING_TEXT_DONE: String = "response.reasoning_text.done"
internal const val OUTPUT_TEXT_DELTA: String = "response.output_text.delta"
internal const val OUTPUT_TEXT_DONE: String = "response.output_text.done"
internal const val FUNCTION_CALL_ARGUMENTS_DELTA: String = "response.function_call_arguments.delta"
internal const val FUNCTION_CALL_ARGUMENTS_DONE: String = "response.function_call_arguments.done"
internal const val CUSTOM_TOOL_CALL_INPUT_DELTA: String = "response.custom_tool_call_input.delta"
internal const val CUSTOM_TOOL_CALL_INPUT_DONE: String = "response.custom_tool_call_input.done"
internal const val WEB_SEARCH_CALL_IN_PROGRESS: String = "response.web_search_call.in_progress"
internal const val WEB_SEARCH_CALL_SEARCHING: String = "response.web_search_call.searching"
internal const val WEB_SEARCH_CALL_COMPLETED: String = "response.web_search_call.completed"

/**
 * Maps the flat `{"type": ..., "sequence_number": ..., ...}` envelope onto the concrete event
 * classes.
 *
 * Field names are spelled out here rather than derived from a naming strategy, so events parse the
 * same way whatever `Json` the client was configured with. Anything that does not map — an unknown
 * type, or a payload missing a field the event needs — becomes an [UnknownResponseStreamEvent]
 * rather than failing the stream.
 */
internal object ResponseStreamEventSerializer : KSerializer<ResponseStreamEvent> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.responses.ResponseStreamEvent",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: ResponseStreamEvent) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val json = output.json
        val element = buildJsonObject {
            put("type", value.type)
            put("sequence_number", value.sequenceNumber)
            when (value) {
                is ResponseSnapshotEvent ->
                    put(
                        "response",
                        json.encodeToJsonElement(ModelResponse.serializer(), value.response),
                    )
                is OutputItemEvent -> {
                    put("output_index", value.outputIndex)
                    put("item", json.encodeToJsonElement(ResponseItemSerializer, value.item))
                }
                is ContentPartEvent -> {
                    put("item_id", value.itemId)
                    put("output_index", value.outputIndex)
                    put("content_index", value.contentIndex)
                    put("part", json.encodeToJsonElement(ResponseContentPartSerializer, value.part))
                }
                is TextDeltaEvent -> {
                    put("item_id", value.itemId)
                    put("output_index", value.outputIndex)
                    put("content_index", value.contentIndex)
                    put("delta", value.delta)
                }
                is TextDoneEvent -> {
                    put("item_id", value.itemId)
                    put("output_index", value.outputIndex)
                    put("content_index", value.contentIndex)
                    put("text", value.text)
                }
                is ToolInputDeltaEvent -> {
                    put("item_id", value.itemId)
                    put("output_index", value.outputIndex)
                    put("delta", value.delta)
                }
                is FunctionCallArgumentsDoneEvent -> {
                    put("item_id", value.itemId)
                    put("output_index", value.outputIndex)
                    put("arguments", value.arguments)
                }
                is CustomToolCallInputDoneEvent -> {
                    put("item_id", value.itemId)
                    put("output_index", value.outputIndex)
                    put("input", value.input)
                }
                is WebSearchCallEvent -> {
                    put("item_id", value.itemId)
                    put("output_index", value.outputIndex)
                }
                is UnknownResponseStreamEvent ->
                    value.raw.forEach { (key, entry) -> put(key, entry) }
            }
        }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ResponseStreamEvent {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val element = input.decodeJsonElement().jsonObject
        val type = element.discriminator()
        val sequenceNumber = element.int("sequence_number") ?: 0
        return runCatching { input.json.event(type, sequenceNumber, element) }.getOrNull()
            ?: UnknownResponseStreamEvent(type, sequenceNumber, element)
    }

    private fun Json.event(
        type: String,
        sequenceNumber: Int,
        element: JsonObject,
    ): ResponseStreamEvent? {
        fun response() =
            decodeFromJsonElement(ModelResponse.serializer(), element.getValue("response"))

        fun item() = decodeFromJsonElement(ResponseItemSerializer, element.getValue("item"))

        fun part() = decodeFromJsonElement(ResponseContentPartSerializer, element.getValue("part"))

        val itemId = element.string("item_id")
        val outputIndex = element.int("output_index")
        val contentIndex = element.int("content_index")

        return when (type) {
            RESPONSE_CREATED -> ResponseCreatedEvent(sequenceNumber, response())
            RESPONSE_IN_PROGRESS -> ResponseInProgressEvent(sequenceNumber, response())
            RESPONSE_COMPLETED -> ResponseCompletedEvent(sequenceNumber, response())
            RESPONSE_INCOMPLETE -> ResponseIncompleteEvent(sequenceNumber, response())
            RESPONSE_FAILED -> ResponseFailedEvent(sequenceNumber, response())
            OUTPUT_ITEM_ADDED -> OutputItemAddedEvent(sequenceNumber, outputIndex!!, item())
            OUTPUT_ITEM_DONE -> OutputItemDoneEvent(sequenceNumber, outputIndex!!, item())
            CONTENT_PART_ADDED ->
                ContentPartAddedEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    contentIndex!!,
                    part(),
                )
            CONTENT_PART_DONE ->
                ContentPartDoneEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    contentIndex!!,
                    part(),
                )
            OUTPUT_TEXT_DELTA ->
                OutputTextDeltaEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    contentIndex!!,
                    element.string("delta")!!,
                )
            REASONING_TEXT_DELTA ->
                ReasoningTextDeltaEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    contentIndex!!,
                    element.string("delta")!!,
                )
            OUTPUT_TEXT_DONE ->
                OutputTextDoneEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    contentIndex!!,
                    element.string("text")!!,
                )
            REASONING_TEXT_DONE ->
                ReasoningTextDoneEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    contentIndex!!,
                    element.string("text")!!,
                )
            FUNCTION_CALL_ARGUMENTS_DELTA ->
                FunctionCallArgumentsDeltaEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    element.string("delta")!!,
                )
            FUNCTION_CALL_ARGUMENTS_DONE ->
                FunctionCallArgumentsDoneEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    element.string("arguments")!!,
                )
            CUSTOM_TOOL_CALL_INPUT_DELTA ->
                CustomToolCallInputDeltaEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    element.string("delta")!!,
                )
            CUSTOM_TOOL_CALL_INPUT_DONE ->
                CustomToolCallInputDoneEvent(
                    sequenceNumber,
                    itemId!!,
                    outputIndex!!,
                    element.string("input")!!,
                )
            WEB_SEARCH_CALL_IN_PROGRESS ->
                WebSearchCallInProgressEvent(sequenceNumber, itemId!!, outputIndex!!)
            WEB_SEARCH_CALL_SEARCHING ->
                WebSearchCallSearchingEvent(sequenceNumber, itemId!!, outputIndex!!)
            WEB_SEARCH_CALL_COMPLETED ->
                WebSearchCallCompletedEvent(sequenceNumber, itemId!!, outputIndex!!)
            else -> null
        }
    }
}

private fun JsonObject.string(key: String): String? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull

private fun JsonObject.int(key: String): Int? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull
