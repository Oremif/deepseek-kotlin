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
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


@Serializable
@JsonClassDiscriminator("role")
public sealed interface ChatMessage {
    public val content: String?
}

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

@Serializable
@SerialName("user")
public class UserMessage(override val content: String?, public val name: String? = null) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserMessage) return false
        return content == other.content && name == other.name
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "UserMessage(content='$content', name=$name)"
}

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

public object ChatCompletionMessageSerializer : KSerializer<ChatCompletionMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChatCompletionMessage") {
        element<String?>("content")
        element<String?>("reasoning_content")
        element("tool_calls", ListSerializer(ToolCall.serializer()).descriptor)
        element("role", String.serializer().descriptor)
    }


    override fun deserialize(decoder: Decoder): ChatCompletionMessage {
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val json = jsonInput.decodeJsonElement().jsonObject
        return ChatCompletionMessage(
            content = json["content"]?.jsonPrimitive?.content,
            reasoningContent = json["reasoning_content"]?.jsonPrimitive?.content,
            toolCalls = json["tool_calls"]?.let {
                jsonInput.json.decodeFromJsonElement(
                    ListSerializer(ToolCall.serializer()),
                    it
                )
            } ?: listOf()
        )
    }

    override fun serialize(encoder: Encoder, value: ChatCompletionMessage) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeNullableSerializableElement(
            descriptor, 0, String.serializer(), value.content
        )
        value.reasoningContent?.let {
            composite.encodeSerializableElement(descriptor, 1, String.serializer(), it)
        }
        value.toolCalls?.let {
            composite.encodeSerializableElement(
                descriptor, 2, ListSerializer(ToolCall.serializer()), it
            )
        }
        composite.encodeStringElement(descriptor, 3, "assistant")
        composite.endStructure(descriptor)
    }
}