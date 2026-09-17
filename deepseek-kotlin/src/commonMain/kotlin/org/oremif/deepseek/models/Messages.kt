@file:OptIn(ExperimentalSerializationApi::class)

package org.oremif.deepseek.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * One turn in a chat conversation.
 *
 * Concrete subtypes — [SystemMessage], [UserMessage], [AssistantMessage], [ToolMessage] — are
 * distinguished on the wire by the `role` discriminator.
 *
 * The `MessageBuilder` DSL (available via `client.chat { ... }`) offers shorter syntax for building
 * a list of [ChatMessage] than instantiating these classes directly.
 *
 * The interface itself declares no `content`: the four subtypes disagree on its shape — required
 * text for [SystemMessage] and [ToolMessage], optional text for [AssistantMessage], and text or
 * multimodal parts for [UserMessage] — so each declares its own. Match on the subtype to read it.
 */
@Serializable @JsonClassDiscriminator("role") public sealed interface ChatMessage

/**
 * System-role message — sets the assistant's behavior for the rest of the conversation.
 *
 * Example:
 * ```kotlin
 * val messages = listOf(
 *     SystemMessage("You are a concise Kotlin expert."),
 *     UserMessage("Explain extension functions."),
 * )
 * ```
 *
 * @property content Instructions for the assistant.
 * @property name Optional participant name forwarded to the model.
 */
@Serializable
@SerialName("system")
public class SystemMessage(public val content: String, public val name: String? = null) :
    ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SystemMessage) return false
        return content == other.content && name == other.name
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "SystemMessage(content='$content', name=$name)"
}

/**
 * User-role message — the end-user's input to the conversation.
 *
 * Carries either plain text or a list of multimodal [ContentPart]s; [UserContent] says which.
 *
 * ```kotlin
 * val text = UserMessage("What is Kotlin?")
 * val multimodal = UserMessage(listOf(TextPart("What is in this image?"), ImageUrlPart(url)))
 *
 * when (val content = multimodal.content) {
 *     is UserContent.Text -> println(content.text)
 *     is UserContent.Parts -> println(content.parts)
 *     null -> println("no content")
 * }
 * ```
 *
 * The `user { ... }` DSL builds the multimodal form more compactly — see
 * [ChatCompletionRequest.UserContentBuilder].
 *
 * @property content User's message text or multimodal parts; `null` when the message carries no new
 *   content at all.
 * @property name Optional participant name forwarded to the model.
 */
@Serializable
@SerialName("user")
public class UserMessage(
    public val content: UserContent?,
    public val name: String? = null,
) : ChatMessage {

    /**
     * Creates a plain-text user message.
     *
     * @param content User's text input
     * @param name Optional participant name forwarded to the model
     */
    public constructor(
        content: String?,
        name: String? = null,
    ) : this(content?.let(UserContent::Text), name)

    /**
     * Creates a multimodal user message.
     *
     * @param parts Content parts, in the order the model should see them
     * @param name Optional participant name forwarded to the model
     */
    public constructor(
        parts: List<ContentPart>,
        name: String? = null,
    ) : this(UserContent.Parts(parts), name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserMessage) return false
        return content == other.content && name == other.name
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "UserMessage(content=$content, name=$name)"
}

/**
 * The two shapes the `content` of a [UserMessage] takes on the wire: a bare string, or an array of
 * [ContentPart]s.
 *
 * The [UserMessage] constructors wrap a `String` or a `List<ContentPart>` for you, so this type
 * mostly shows up when reading a message back.
 */
@Serializable(with = UserContentSerializer::class)
public sealed interface UserContent {

    /**
     * Plain text, sent and received as a bare JSON string.
     *
     * @property text The text itself.
     */
    public class Text(public val text: String) : UserContent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Text) return false
            return text == other.text
        }

        override fun hashCode(): Int = text.hashCode()

        override fun toString(): String = "UserContent.Text(text='$text')"
    }

    /**
     * Multimodal content, sent and received as a JSON array of parts.
     *
     * @property parts The parts, in the order the model sees them.
     */
    public class Parts(public val parts: List<ContentPart>) : UserContent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Parts) return false
            return parts == other.parts
        }

        override fun hashCode(): Int = parts.hashCode()

        override fun toString(): String = "UserContent.Parts(parts=$parts)"
    }
}

/**
 * Encodes [UserContent.Text] as a JSON string and [UserContent.Parts] as a JSON array, and reads
 * either shape back.
 */
internal object UserContentSerializer : KSerializer<UserContent> {
    private val partsSerializer = ListSerializer(ContentPart.serializer())

    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.UserContent",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: UserContent) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val element =
            when (value) {
                is UserContent.Text -> JsonPrimitive(value.text)
                is UserContent.Parts ->
                    output.json.encodeToJsonElement(partsSerializer, value.parts)
            }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): UserContent {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonArray ->
                UserContent.Parts(input.json.decodeFromJsonElement(partsSerializer, element))
            else -> UserContent.Text(element.jsonPrimitive.content)
        }
    }
}

/**
 * Assistant-role message — a model response, whether produced by the API or replayed as context.
 *
 * The same class travels in both directions, which is what makes the tool-calling loop a plain list
 * append: take the message off a [ChatChoice] and put it straight back into the next request.
 *
 * ```kotlin
 * val first = client.chat(params, messages)
 * val call = first.choices.single().message.toolCalls?.single()
 * val next = messages +
 *     first.choices.single().message +
 *     ToolMessage(runTool(call), call.id)
 * ```
 *
 * @property content Assistant message text; `null` when the message only carries [toolCalls].
 * @property name Optional participant name forwarded to the model.
 * @property prefix When `true`, marks this message as a partial assistant response that the model
 *   should continue generating from. Prefix completion is only served from the beta base path: a
 *   request ending in such a message is routed to `beta/chat/completions` automatically, so the
 *   client needs no extra configuration.
 * @property reasoningContent Chain-of-thought that precedes [content] in thinking mode. On a
 *   response it is what the model thought; on a request it seeds the continuation, and so takes
 *   effect only alongside `prefix = true`.
 * @property toolCalls Tool calls the model emitted on this turn, or `null` if none. Answer each one
 *   with a [ToolMessage] carrying the matching [ToolCall.id].
 */
@Serializable
@SerialName("assistant")
@JsonIgnoreUnknownKeys
public class AssistantMessage(
    public val content: String?,
    public val name: String? = null,
    public val prefix: Boolean? = null,
    public val reasoningContent: String? = null,
    public val toolCalls: List<ToolCall>? = null,
) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssistantMessage) return false
        return content == other.content &&
            name == other.name &&
            prefix == other.prefix &&
            reasoningContent == other.reasoningContent &&
            toolCalls == other.toolCalls
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + prefix.hashCode()
        result = 31 * result + reasoningContent.hashCode()
        result = 31 * result + toolCalls.hashCode()
        return result
    }

    override fun toString(): String =
        "AssistantMessage(content=$content, name=$name, prefix=$prefix, " +
            "reasoningContent=$reasoningContent, toolCalls=$toolCalls)"
}

/**
 * Tool-role message — the result of executing a [ToolCall] produced by the model.
 *
 * Echo [toolCallId] from the matching [ToolCall] so the model can correlate the result with its
 * original call.
 *
 * Example:
 * ```kotlin
 * val toolResult = ToolMessage(
 *     content = Json.encodeToString(weatherResponse),
 *     toolCallId = toolCall.id,
 * )
 * ```
 *
 * @property content Result of the tool execution, typically JSON-encoded.
 * @property toolCallId Identifier of the [ToolCall] this message responds to.
 */
@Serializable
@SerialName("tool")
public class ToolMessage(public val content: String, public val toolCallId: String) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ToolMessage) return false
        return content == other.content && toolCallId == other.toolCallId
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + toolCallId.hashCode()
        return result
    }

    override fun toString(): String = "ToolMessage(content='$content', toolCallId='$toolCallId')"
}
