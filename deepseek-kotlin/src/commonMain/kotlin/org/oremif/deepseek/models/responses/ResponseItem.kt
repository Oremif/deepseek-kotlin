package org.oremif.deepseek.models.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.jvm.JvmInline

/**
 * One entry of the conversation handed to — or produced by — the Responses API.
 *
 * The Responses API is **stateless**: the server keeps nothing between calls, so every turn resends
 * the whole history. The same item types travel in both directions, which makes continuing a
 * conversation a plain list append:
 * ```kotlin
 * val history = mutableListOf<ResponseItem>(MessageItem(ResponseRole.USER, "What is 6 * 7?"))
 * val answer = client.createResponse(params, history)
 * history += answer.output
 * history += MessageItem(ResponseRole.USER, "And 6 * 8?")
 * ```
 *
 * An item whose `type` this version of the SDK does not model is preserved as an [UnknownItem], so
 * a response stays readable — and replays unchanged — after the API grows an item type.
 *
 * @property type Raw `type` discriminator of the item.
 * @see <a href="https://api-docs.deepseek.com/api/create-response">Create Response</a>
 */
@Serializable(with = ResponseItemSerializer::class)
public sealed interface ResponseItem {
    public val type: String
}

/**
 * Author of a [MessageItem].
 *
 * Serialized as the raw value the API expects, so a role the SDK declares no constant for is still
 * usable: `ResponseRole("moderator")`.
 *
 * @property value Raw role sent in — and returned by — the `role` field. Must not be blank.
 */
@Serializable
@JvmInline
public value class ResponseRole(public val value: String) {
    init {
        require(value.isNotBlank()) { "role must not be blank" }
    }

    override fun toString(): String = value

    public companion object {
        /** `user` — the end user's turn. */
        public val USER: ResponseRole = ResponseRole("user")

        /** `assistant` — a turn produced by the model. The only role output items carry. */
        public val ASSISTANT: ResponseRole = ResponseRole("assistant")

        /** `system` — instructions steering the conversation. Rejects image parts. */
        public val SYSTEM: ResponseRole = ResponseRole("system")

        /** `developer` — treated by the API exactly like [USER]. */
        public val DEVELOPER: ResponseRole = ResponseRole("developer")
    }
}

/**
 * A message turn: text, or multimodal parts, attributed to a [ResponseRole].
 *
 * Carries either plain text or a list of [ResponseContentPart]s, never both — the two constructors
 * pick the form, and the unused accessor reads back as `null`.
 *
 * ```kotlin
 * val plain = MessageItem(ResponseRole.USER, "What is Kotlin?")
 * val multimodal = MessageItem(
 *     ResponseRole.USER,
 *     listOf(InputTextPart("What is in this image?"), InputImagePart(imageUrl = url)),
 * )
 *
 * when (val content = plain.content) {
 *     is ResponseContent.Text -> println(content.text)
 *     is ResponseContent.Parts -> println(content.parts)
 *     null -> println("no content")
 * }
 * ```
 *
 * @property role Author of the message.
 * @property content Message text or multimodal parts; `null` only on an item the API sent without
 *   any content, such as the empty shell of a `response.output_item.added` event.
 * @property id Identifier the API assigned to the item; `null` on an item the caller built.
 * @property status Generation state of the item; `null` on an item the caller built.
 */
@Serializable
public class MessageItem(
    public val role: ResponseRole,
    public val content: ResponseContent?,
    public val id: String? = null,
    public val status: ResponseStatus? = null,
) : ResponseItem {

    /**
     * Creates a plain-text message item.
     *
     * @param role Author of the message
     * @param content Message text
     * @param id Identifier of an item echoed back from a previous response
     * @param status Generation state of an item echoed back from a previous response
     */
    public constructor(
        role: ResponseRole,
        content: String,
        id: String? = null,
        status: ResponseStatus? = null,
    ) : this(role, ResponseContent.Text(content), id, status)

    /**
     * Creates a multimodal message item.
     *
     * @param role Author of the message
     * @param parts Content parts, in the order the model should see them
     * @param id Identifier of an item echoed back from a previous response
     * @param status Generation state of an item echoed back from a previous response
     */
    public constructor(
        role: ResponseRole,
        parts: List<ResponseContentPart>,
        id: String? = null,
        status: ResponseStatus? = null,
    ) : this(role, ResponseContent.Parts(parts), id, status)

    override val type: String
        get() = MESSAGE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageItem) return false
        return role == other.role &&
            content == other.content &&
            id == other.id &&
            status == other.status
    }

    override fun hashCode(): Int {
        var result = role.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String =
        "MessageItem(role=$role, content=$content, id=$id, status=$status)"
}

/**
 * The chain-of-thought a thinking model produced before its answer.
 *
 * Comes back ahead of the [MessageItem] in [ModelResponse.output], and is accepted on input so an
 * earlier turn's reasoning can be replayed.
 *
 * @property content Parts carrying the reasoning, in practice [ReasoningTextPart]s.
 * @property id Identifier the API assigned to the item; `null` on an item the caller built.
 * @property status Generation state of the item; `null` on an item the caller built.
 */
@Serializable
public class ReasoningItem(
    public val content: List<ResponseContentPart> = emptyList(),
    public val id: String? = null,
    public val status: ResponseStatus? = null,
) : ResponseItem {

    /**
     * Creates a reasoning item from plain chain-of-thought text.
     *
     * @param text The reasoning text, wrapped in a single [ReasoningTextPart]
     * @param id Identifier of an item echoed back from a previous response
     * @param status Generation state of an item echoed back from a previous response
     */
    public constructor(
        text: String,
        id: String? = null,
        status: ResponseStatus? = null,
    ) : this(listOf(ReasoningTextPart(text)), id, status)

    override val type: String
        get() = REASONING

    /** The reasoning text of every [TextContentPart] in [content], concatenated in order. */
    public val text: String
        get() = content.filterIsInstance<TextContentPart>().joinToString(separator = "") { it.text }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReasoningItem) return false
        return content == other.content && id == other.id && status == other.status
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String = "ReasoningItem(content=$content, id=$id, status=$status)"
}

/**
 * A function the model asked to call.
 *
 * Answer it with a [FunctionCallOutputItem] carrying the same [callId]; every call in the history
 * must have its matching output, or the API answers 400.
 *
 * @property callId Identifier pairing the call with its output. Must be non-empty and unique.
 * @property name Name of the function to call.
 * @property arguments Arguments the model generated, as a raw JSON string. Not guaranteed to be
 *   valid JSON, nor to match the declared schema; [argumentsAsJsonOrNull] parses it defensively.
 * @property id Identifier the API assigned to the item; `null` on an item the caller built.
 * @property status Generation state of the item; `null` on an item the caller built.
 */
@Serializable
public class FunctionCallItem(
    public val callId: String,
    public val name: String,
    public val arguments: String,
    public val id: String? = null,
    public val status: ResponseStatus? = null,
) : ResponseItem {
    override val type: String
        get() = FUNCTION_CALL

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionCallItem) return false
        return callId == other.callId &&
            name == other.name &&
            arguments == other.arguments &&
            id == other.id &&
            status == other.status
    }

    override fun hashCode(): Int {
        var result = callId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String =
        "FunctionCallItem(callId='$callId', name='$name', arguments=$arguments, id=$id, status=$status)"
}

/**
 * Parses [FunctionCallItem.arguments] into a [JsonObject], or returns `null` when the model
 * produced malformed JSON or a JSON value that is not an object.
 *
 * Example:
 * ```kotlin
 * val call = response.functionCalls.firstOrNull()
 * val city = call?.argumentsAsJsonOrNull()?.get("city")?.jsonPrimitive?.content
 * ```
 *
 * @param json [Json] instance used for parsing; the default is sufficient for plain argument
 *   objects.
 * @return The parsed arguments, or `null` if they are not a valid JSON object.
 */
public fun FunctionCallItem.argumentsAsJsonOrNull(json: Json = Json): JsonObject? = runCatching {
    json.parseToJsonElement(arguments) as? JsonObject
}
    .getOrNull()

/**
 * The result of running a function the model called.
 *
 * Carries either plain text or a list of [ResponseContentPart]s — an image part lets a tool hand
 * the model a screenshot it produced.
 *
 * @property callId Identifier of the [FunctionCallItem] this output answers.
 * @property output Tool result, as text or as content parts.
 */
@Serializable
public class FunctionCallOutputItem(
    public val callId: String,
    public val output: ResponseContent,
) : ResponseItem {

    /**
     * Creates a textual tool result.
     *
     * @param callId Identifier of the [FunctionCallItem] this output answers
     * @param output Tool result, typically JSON-encoded
     */
    public constructor(callId: String, output: String) : this(callId, ResponseContent.Text(output))

    /**
     * Creates a tool result made of content parts.
     *
     * @param callId Identifier of the [FunctionCallItem] this output answers
     * @param output Tool result parts, in the order the model should see them
     */
    public constructor(
        callId: String,
        output: List<ResponseContentPart>,
    ) : this(callId, ResponseContent.Parts(output))

    override val type: String
        get() = FUNCTION_CALL_OUTPUT

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionCallOutputItem) return false
        return callId == other.callId && output == other.output
    }

    override fun hashCode(): Int {
        var result = callId.hashCode()
        result = 31 * result + output.hashCode()
        return result
    }

    override fun toString(): String = "FunctionCallOutputItem(callId='$callId', output=$output)"
}

/**
 * A call to the `apply_patch` custom tool, which takes free-form input instead of JSON arguments.
 *
 * @property callId Identifier pairing the call with its [CustomToolCallOutputItem].
 * @property name Name of the custom tool; the API only serves `apply_patch`.
 * @property input Raw input the model generated for the tool.
 * @property id Identifier the API assigned to the item; `null` on an item the caller built.
 * @property status Generation state of the item; `null` on an item the caller built.
 */
@Serializable
public class CustomToolCallItem(
    public val callId: String,
    public val name: String,
    public val input: String,
    public val id: String? = null,
    public val status: ResponseStatus? = null,
) : ResponseItem {
    override val type: String
        get() = CUSTOM_TOOL_CALL

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomToolCallItem) return false
        return callId == other.callId &&
            name == other.name &&
            input == other.input &&
            id == other.id &&
            status == other.status
    }

    override fun hashCode(): Int {
        var result = callId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + input.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String =
        "CustomToolCallItem(callId='$callId', name='$name', input='$input', id=$id, status=$status)"
}

/**
 * The result of running a [CustomToolCallItem].
 *
 * @property callId Identifier of the [CustomToolCallItem] this output answers.
 * @property output Tool result, as text or as content parts.
 */
@Serializable
public class CustomToolCallOutputItem(
    public val callId: String,
    public val output: ResponseContent,
) : ResponseItem {

    /**
     * Creates a textual custom tool result.
     *
     * @param callId Identifier of the [CustomToolCallItem] this output answers
     * @param output Tool result
     */
    public constructor(callId: String, output: String) : this(callId, ResponseContent.Text(output))

    /**
     * Creates a custom tool result made of content parts.
     *
     * @param callId Identifier of the [CustomToolCallItem] this output answers
     * @param output Tool result parts, in the order the model should see them
     */
    public constructor(
        callId: String,
        output: List<ResponseContentPart>,
    ) : this(callId, ResponseContent.Parts(output))

    override val type: String
        get() = CUSTOM_TOOL_CALL_OUTPUT

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomToolCallOutputItem) return false
        return callId == other.callId && output == other.output
    }

    override fun hashCode(): Int {
        var result = callId.hashCode()
        result = 31 * result + output.hashCode()
        return result
    }

    override fun toString(): String = "CustomToolCallOutputItem(callId='$callId', output=$output)"
}

/**
 * A web search the API ran on its own, server side.
 *
 * Appears in [ModelResponse.output] once the `web_search` tool fires. Pass it back as-is in the
 * next turn's input — the server restores the search results behind it.
 *
 * @property action Description of the action performed (`search`, `open_page` or `find_in_page`),
 *   kept as raw JSON since the API documents no fixed shape for it.
 * @property id Identifier the API assigned to the item.
 * @property status Generation state of the item.
 */
@Serializable
public class WebSearchCallItem(
    public val action: JsonObject? = null,
    public val id: String? = null,
    public val status: ResponseStatus? = null,
) : ResponseItem {
    override val type: String
        get() = WEB_SEARCH_CALL

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSearchCallItem) return false
        return action == other.action && id == other.id && status == other.status
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }

    override fun toString(): String = "WebSearchCallItem(action=$action, id=$id, status=$status)"
}

/**
 * An item whose `type` this version of the SDK does not model.
 *
 * Keeps the payload verbatim, so a response carrying a newly-introduced item type still parses and
 * replays unchanged as input.
 *
 * @property type Raw `type` discriminator the API sent.
 * @property raw The item exactly as it arrived, discriminator included.
 */
public class UnknownItem(
    override val type: String,
    public val raw: JsonObject,
) : ResponseItem {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnknownItem) return false
        return type == other.type && raw == other.raw
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + raw.hashCode()
        return result
    }

    override fun toString(): String = "UnknownItem(type='$type', raw=$raw)"
}

internal const val MESSAGE: String = "message"
internal const val REASONING: String = "reasoning"
internal const val FUNCTION_CALL: String = "function_call"
internal const val FUNCTION_CALL_OUTPUT: String = "function_call_output"
internal const val CUSTOM_TOOL_CALL: String = "custom_tool_call"
internal const val CUSTOM_TOOL_CALL_OUTPUT: String = "custom_tool_call_output"
internal const val WEB_SEARCH_CALL: String = "web_search_call"

/**
 * Writes the `type` discriminator alongside the fields of the concrete item, and reads it back.
 *
 * A message item may legally arrive without `type` as long as it carries `role`; both shapes are
 * accepted on the way in, and `type` is always written on the way out. Anything else unrecognised
 * becomes an [UnknownItem].
 */
internal object ResponseItemSerializer : KSerializer<ResponseItem> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.responses.ResponseItem",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: ResponseItem) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val element =
            when (value) {
                is MessageItem ->
                    withType(value.type, output.json.encode(MessageItem.serializer(), value))
                is ReasoningItem ->
                    withType(value.type, output.json.encode(ReasoningItem.serializer(), value))
                is FunctionCallItem ->
                    withType(value.type, output.json.encode(FunctionCallItem.serializer(), value))
                is FunctionCallOutputItem ->
                    withType(
                        value.type,
                        output.json.encode(FunctionCallOutputItem.serializer(), value),
                    )
                is CustomToolCallItem ->
                    withType(value.type, output.json.encode(CustomToolCallItem.serializer(), value))
                is CustomToolCallOutputItem ->
                    withType(
                        value.type,
                        output.json.encode(CustomToolCallOutputItem.serializer(), value),
                    )
                is WebSearchCallItem ->
                    withType(value.type, output.json.encode(WebSearchCallItem.serializer(), value))
                // Kept verbatim: re-adding a discriminator we never parsed would change the
                // payload.
                is UnknownItem -> value.raw
            }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ResponseItem {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val element = input.decodeJsonElement().jsonObject
        val type = element.discriminator().ifEmpty { if ("role" in element) MESSAGE else "" }
        val body = JsonObject(element - "type")
        return when (type) {
            MESSAGE -> input.json.decodeFromJsonElement(MessageItem.serializer(), body)
            REASONING -> input.json.decodeFromJsonElement(ReasoningItem.serializer(), body)
            FUNCTION_CALL -> input.json.decodeFromJsonElement(FunctionCallItem.serializer(), body)
            FUNCTION_CALL_OUTPUT ->
                input.json.decodeFromJsonElement(FunctionCallOutputItem.serializer(), body)
            CUSTOM_TOOL_CALL ->
                input.json.decodeFromJsonElement(CustomToolCallItem.serializer(), body)
            CUSTOM_TOOL_CALL_OUTPUT ->
                input.json.decodeFromJsonElement(CustomToolCallOutputItem.serializer(), body)
            WEB_SEARCH_CALL ->
                input.json.decodeFromJsonElement(WebSearchCallItem.serializer(), body)
            else -> UnknownItem(type, element)
        }
    }
}
