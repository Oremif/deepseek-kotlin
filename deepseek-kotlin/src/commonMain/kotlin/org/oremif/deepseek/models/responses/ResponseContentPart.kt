package org.oremif.deepseek.models.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.oremif.deepseek.models.ImageDetail

/**
 * One element of the content array of a [ResponseItem].
 *
 * The concrete subtypes are distinguished on the wire by the `type` discriminator: [InputTextPart]
 * and [InputImagePart] describe what is sent to the model, [OutputTextPart] and [ReasoningTextPart]
 * what comes back. A part the SDK does not know is preserved as an [UnknownResponseContentPart]
 * rather than failing the whole response.
 *
 * Prefer the `input { }` DSL over instantiating parts directly:
 * ```kotlin
 * client.createResponse {
 *     params { model = ChatModel.DEEPSEEK_FLASH }
 *     input {
 *         user {
 *             text("What is in this image?")
 *             image("https://example.com/cat.jpg", ImageDetail.LOW)
 *         }
 *     }
 * }
 * ```
 *
 * @property type Raw `type` discriminator of the part.
 * @see <a href="https://api-docs.deepseek.com/api/create-response">Create Response</a>
 */
@Serializable(with = ResponseContentPartSerializer::class)
public sealed interface ResponseContentPart {
    public val type: String
}

/**
 * Base of the three content parts that carry nothing but text, so they can be matched at once:
 * ```kotlin
 * val text = part.let { if (it is TextContentPart) it.text else null }
 * ```
 *
 * @property text The text the part carries.
 */
public sealed class TextContentPart : ResponseContentPart {
    public abstract val text: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextContentPart) return false
        return type == other.type && text == other.text
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }

    override fun toString(): String = "${this::class.simpleName}(text='$text')"
}

/**
 * Text sent to the model, as `input_text`.
 *
 * @property text The text itself.
 */
@Serializable
public class InputTextPart(override val text: String) : TextContentPart() {
    override val type: String
        get() = INPUT_TEXT
}

/**
 * Text produced by the model, as `output_text`.
 *
 * Also accepted on input: replaying a previous assistant message keeps the parts it came back with.
 *
 * @property text The text itself.
 */
@Serializable
public class OutputTextPart(override val text: String) : TextContentPart() {
    override val type: String
        get() = OUTPUT_TEXT
}

/**
 * Chain-of-thought produced by the model in thinking mode, as `reasoning_text`.
 *
 * Appears in the content of a [ReasoningItem], and is accepted back on input so the reasoning of an
 * earlier turn can be replayed.
 *
 * @property text The chain-of-thought text.
 */
@Serializable
public class ReasoningTextPart(override val text: String) : TextContentPart() {
    override val type: String
        get() = REASONING_TEXT
}

/**
 * Image sent to the model, as `input_image`.
 *
 * Exactly one of [imageUrl] and [fileId] must be set — the API answers 400 to a part carrying
 * neither or both. Both models the API serves read images; a model that cannot replaces the part
 * with a placeholder text. Images are accepted in `user` and `developer` messages and in the output
 * of a tool call; in a `system` or `assistant` message they are rejected with 400.
 *
 * @property imageUrl Either an `http(s)` link the API fetches (at most 8192 characters), or a
 *   base64 data URL that inlines the image (`data:image/jpeg;base64,...`). JPEG, PNG, GIF and WebP
 *   are supported. Mutually exclusive with [fileId].
 * @property fileId Identifier of an image uploaded through the Files API, of the form
 *   `file-api-...`. Mutually exclusive with [imageUrl].
 * @property detail How much of the image's resolution reaches the model; ignored when [fileId] is
 *   set.
 */
@Serializable
public class InputImagePart(
    public val imageUrl: String? = null,
    public val fileId: String? = null,
    public val detail: ImageDetail? = null,
) : ResponseContentPart {
    init {
        require((imageUrl == null) != (fileId == null)) {
            "an input image part must carry exactly one of imageUrl and fileId"
        }
    }

    override val type: String
        get() = INPUT_IMAGE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InputImagePart) return false
        return imageUrl == other.imageUrl && fileId == other.fileId && detail == other.detail
    }

    override fun hashCode(): Int {
        var result = imageUrl?.hashCode() ?: 0
        result = 31 * result + (fileId?.hashCode() ?: 0)
        result = 31 * result + (detail?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "InputImagePart(imageUrl=$imageUrl, fileId=$fileId, detail=$detail)"
}

/**
 * A content part whose `type` this version of the SDK does not model.
 *
 * Keeps the payload verbatim so a response stays readable — and round-trips unchanged when replayed
 * as input — after the API grows a part type.
 *
 * @property type Raw `type` discriminator the API sent.
 * @property raw The part exactly as it arrived, discriminator included.
 */
public class UnknownResponseContentPart(
    override val type: String,
    public val raw: JsonObject,
) : ResponseContentPart {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnknownResponseContentPart) return false
        return type == other.type && raw == other.raw
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + raw.hashCode()
        return result
    }

    override fun toString(): String = "UnknownResponseContentPart(type='$type', raw=$raw)"
}

internal const val INPUT_TEXT: String = "input_text"
internal const val OUTPUT_TEXT: String = "output_text"
internal const val REASONING_TEXT: String = "reasoning_text"
internal const val INPUT_IMAGE: String = "input_image"

/**
 * Writes the `type` discriminator alongside the fields of the concrete part, and reads it back,
 * falling through to [UnknownResponseContentPart] for a type this SDK does not model.
 */
internal object ResponseContentPartSerializer : KSerializer<ResponseContentPart> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.responses.ResponseContentPart",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: ResponseContentPart) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val element =
            when (value) {
                is InputTextPart ->
                    withType(value.type, output.json.encode(InputTextPart.serializer(), value))
                is OutputTextPart ->
                    withType(value.type, output.json.encode(OutputTextPart.serializer(), value))
                is ReasoningTextPart ->
                    withType(value.type, output.json.encode(ReasoningTextPart.serializer(), value))
                is InputImagePart ->
                    withType(value.type, output.json.encode(InputImagePart.serializer(), value))
                // Kept verbatim: re-adding a discriminator we never parsed would change the
                // payload.
                is UnknownResponseContentPart -> value.raw
            }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ResponseContentPart {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val element = input.decodeJsonElement().jsonObject
        val type = element.discriminator()
        val body = JsonObject(element - "type")
        return when (type) {
            INPUT_TEXT -> input.json.decodeFromJsonElement(InputTextPart.serializer(), body)
            OUTPUT_TEXT -> input.json.decodeFromJsonElement(OutputTextPart.serializer(), body)
            REASONING_TEXT -> input.json.decodeFromJsonElement(ReasoningTextPart.serializer(), body)
            INPUT_IMAGE -> input.json.decodeFromJsonElement(InputImagePart.serializer(), body)
            else -> UnknownResponseContentPart(type, element)
        }
    }
}

/** Prepends the `type` discriminator to an encoded body, without duplicating an existing one. */
internal fun withType(type: String, body: JsonObject): JsonObject =
    if ("type" in body) body else JsonObject(mapOf("type" to JsonPrimitive(type)) + body)

/** Encodes [value] with [serializer] and narrows the result to the object it is known to be. */
internal fun <T> Json.encode(serializer: SerializationStrategy<T>, value: T): JsonObject =
    encodeToJsonElement(serializer, value).jsonObject

/** Reads the `type` discriminator of a wire object, defaulting to an empty string when absent. */
internal fun JsonObject.discriminator(): String =
    this["type"]?.jsonPrimitive?.contentOrNull.orEmpty()

/**
 * The two shapes a `content` or `output` field takes on the wire: a bare string, or an array of
 * [ResponseContentPart]s.
 *
 * Reading one is a `when` over the two cases:
 * ```kotlin
 * when (val content = message.content) {
 *     is ResponseContent.Text -> println(content.text)
 *     is ResponseContent.Parts -> content.parts.forEach(::println)
 *     null -> println("no content")
 * }
 * ```
 *
 * Writing one is usually implicit: the item constructors take a `String` or a
 * `List<ResponseContentPart>` and wrap it for you.
 */
@Serializable(with = ResponseContentSerializer::class)
public sealed interface ResponseContent {

    /**
     * Plain text, sent and received as a bare JSON string.
     *
     * @property text The text itself.
     */
    public class Text(public val text: String) : ResponseContent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Text) return false
            return text == other.text
        }

        override fun hashCode(): Int = text.hashCode()

        override fun toString(): String = "ResponseContent.Text(text='$text')"
    }

    /**
     * Multimodal content, sent and received as a JSON array of parts.
     *
     * @property parts The parts, in the order the model sees them.
     */
    public class Parts(public val parts: List<ResponseContentPart>) : ResponseContent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Parts) return false
            return parts == other.parts
        }

        override fun hashCode(): Int = parts.hashCode()

        override fun toString(): String = "ResponseContent.Parts(parts=$parts)"
    }
}

/** Encodes [ResponseContent.Text] as a JSON string and [ResponseContent.Parts] as a JSON array. */
internal object ResponseContentSerializer : KSerializer<ResponseContent> {
    private val partsSerializer = ListSerializer(ResponseContentPartSerializer)

    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.responses.ResponseContent",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: ResponseContent) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val element =
            when (value) {
                is ResponseContent.Text -> JsonPrimitive(value.text)
                is ResponseContent.Parts ->
                    output.json.encodeToJsonElement(partsSerializer, value.parts)
            }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ResponseContent {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonArray ->
                ResponseContent.Parts(input.json.decodeFromJsonElement(partsSerializer, element))
            else -> ResponseContent.Text(element.jsonPrimitive.content)
        }
    }
}
