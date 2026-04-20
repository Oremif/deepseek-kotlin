@file:OptIn(ExperimentalSerializationApi::class)

package org.oremif.deepseek.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*


/**
 * One turn in a chat conversation.
 *
 * Concrete subtypes — [SystemMessage], [UserMessage], [AssistantMessage], [ToolMessage] —
 * are distinguished on the wire by the `role` discriminator.
 *
 * The `MessageBuilder` DSL (available via `client.chat { ... }`) offers shorter syntax
 * for building a list of [ChatMessage] than instantiating these classes directly.
 *
 * @property content Message text; `null` is allowed for assistant/user messages that
 * only carry tool calls.
 */
@Serializable
@JsonClassDiscriminator("role")
public sealed interface ChatMessage {
    public val content: String?
}

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
public class SystemMessage(override val content: String, public val name: String? = null) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SystemMessage) return false
        return content == other.content && name == other.name
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "SystemMessage(content='$content', name=$name)"
}

/**
 * User-role message — the end-user's input to the conversation.
 *
 * @property content User's message text. May be `null` only in unusual cases such as a
 * follow-up turn that carries no new text.
 * @property name Optional participant name forwarded to the model.
 */
@Serializable
@SerialName("user")
public class UserMessage(override val content: String?, public val name: String? = null) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserMessage) return false
        return content == other.content && name == other.name
    }

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "UserMessage(content='$content', name=$name)"
}

/**
 * Assistant-role message — either a prior model response replayed as context, or a
 * prefix-completion seed for the `deepseek-chat` prefix mode.
 *
 * Model responses returned by the API are represented by the specialised
 * [ChatCompletionMessage] subclass, which additionally carries [ToolCall]s.
 *
 * @property content Assistant message text; may be `null` when the message only carries
 * tool calls.
 * @property name Optional participant name forwarded to the model.
 * @property prefix When `true`, marks this message as a partial assistant response that
 * the model should continue generating from. See the DeepSeek API docs for prefix mode.
 * @property reasoningContent For the `deepseek-reasoner` model: the reasoning trace
 * associated with the response.
 */
@Serializable
@SerialName("assistant")
public open class AssistantMessage(
    override val content: String?,
    public val name: String? = null,
    public val prefix: Boolean? = null,
    public val reasoningContent: String? = null,
) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssistantMessage) return false
        return content == other.content &&
                name == other.name &&
                prefix == other.prefix &&
                reasoningContent == other.reasoningContent
    }

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (prefix?.hashCode() ?: 0)
        result = 31 * result + (reasoningContent?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "AssistantMessage(content=$content, name=$name, prefix=$prefix, reasoningContent=$reasoningContent)"
}

/**
 * Assistant-role message as returned by the DeepSeek API inside a [ChatChoice].
 *
 * Uses a custom serializer so that the `role` field is always present and `tool_calls`
 * is emitted only when non-null.
 *
 * @property toolCalls Tool calls the model emitted on this turn, or `null` if none.
 */
@Serializable(with = ChatCompletionMessageSerializer::class)
@SerialName("assistant")
public class ChatCompletionMessage(
    content: String?,
    reasoningContent: String? = null,
    public val toolCalls: List<ToolCall>? = null,
) : AssistantMessage(content = content, reasoningContent = reasoningContent) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatCompletionMessage) return false
        return content == other.content &&
                reasoningContent == other.reasoningContent &&
                toolCalls == other.toolCalls
    }

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + (reasoningContent?.hashCode() ?: 0)
        result = 31 * result + (toolCalls?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChatCompletionMessage(content=$content, reasoningContent=$reasoningContent, toolCalls=$toolCalls)"
}

/**
 * Tool-role message — the result of executing a [ToolCall] produced by the model.
 *
 * Echo [toolCallId] from the matching [ToolCall] so the model can correlate the result
 * with its original call.
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
public class ToolMessage(override val content: String, public val toolCallId: String) : ChatMessage {
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

    override fun toString(): String =
        "ToolMessage(content='$content', toolCallId='$toolCallId')"
}

/**
 * Custom serializer for [ChatCompletionMessage] that enforces the assistant role on the
 * wire and omits `null` `tool_calls` fields from the output.
 *
 * Exposed publicly because it is referenced from the `@Serializable(with = ...)`
 * annotation on [ChatCompletionMessage]; callers do not normally invoke it directly.
 */
public object ChatCompletionMessageSerializer : KSerializer<ChatCompletionMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChatCompletionMessage") {
        element<String?>("content")
        element<String?>("reasoning_content")
        element<List<ToolCall>?>("tool_calls")
        element("role", String.serializer().descriptor)
    }


    override fun deserialize(decoder: Decoder): ChatCompletionMessage {
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val json = jsonInput.decodeJsonElement().jsonObject
        return ChatCompletionMessage(
            content = json["content"]?.jsonPrimitive?.content,
            reasoningContent = json["reasoning_content"]?.jsonPrimitive?.content,
            toolCalls = json["tool_calls"]?.takeIf { it !is JsonNull }?.let {
                jsonInput.json.decodeFromJsonElement(
                    ListSerializer(ToolCall.serializer()),
                    it
                )
            }
        )
    }

    override fun serialize(encoder: Encoder, value: ChatCompletionMessage) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeNullableSerializableElement(
            descriptor, 0, String.serializer(), value.content
        )
        value.reasoningContent?.let {
            composite.encodeNullableSerializableElement(descriptor, 1, String.serializer(), it)
        }
        value.toolCalls?.let {
            composite.encodeNullableSerializableElement(
                descriptor, 2, ListSerializer(ToolCall.serializer()), it
            )
        }
        composite.encodeStringElement(descriptor, 3, "assistant")
        composite.endStructure(descriptor)
    }
}