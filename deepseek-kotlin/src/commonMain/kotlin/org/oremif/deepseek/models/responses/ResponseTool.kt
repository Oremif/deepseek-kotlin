package org.oremif.deepseek.models.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * A tool the model may use while producing a response.
 *
 * Unlike `/chat/completions`, which only takes functions, the Responses API also serves the
 * server-side [WebSearchTool] — the model searches the web itself and the results never pass
 * through the caller.
 *
 * @property type Raw `type` discriminator of the tool.
 * @see <a href="https://api-docs.deepseek.com/api/create-response">Create Response</a>
 */
@Serializable(with = ResponseToolSerializer::class)
public sealed interface ResponseTool {
    public val type: String
}

/** Character set and length the API accepts in a function tool name. */
private val FUNCTION_NAME_REGEX = Regex("^[a-zA-Z0-9_-]+$")

/**
 * A function the model may call, described by its JSON Schema.
 *
 * The model answers with a [FunctionCallItem]; run the function and feed the result back as a
 * [FunctionCallOutputItem] carrying the same `call_id`.
 *
 * Example:
 * ```kotlin
 * val weather = FunctionTool(
 *     name = "get_weather",
 *     description = "Get the current weather for a city",
 *     parameters = buildJsonObject {
 *         put("type", "object")
 *         putJsonObject("properties") { putJsonObject("city") { put("type", "string") } }
 *         putJsonArray("required") { add("city") }
 *     },
 * )
 * ```
 *
 * @property name Function name: 1 to 128 characters from `[a-zA-Z0-9_-]`, unique across the tool
 *   list.
 * @property description What the function does; helps the model decide when to call it.
 * @property parameters JSON-Schema description of the arguments the function accepts.
 */
@Serializable
public class FunctionTool(
    public val name: String,
    public val description: String? = null,
    public val parameters: JsonObject? = null,
) : ResponseTool {
    init {
        require(name.length <= 128) {
            "function name must be at most 128 characters long, was ${name.length}"
        }
        require(FUNCTION_NAME_REGEX.matches(name)) {
            "function name must be non-empty and match [a-zA-Z0-9_-]"
        }
    }

    override val type: String
        get() = FUNCTION

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionTool) return false
        return name == other.name &&
            description == other.description &&
            parameters == other.parameters
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

    override fun toString(): String =
        "FunctionTool(name='$name', description=$description, parameters=$parameters)"
}

/**
 * The built-in web search, executed on the API's side.
 *
 * The model decides when to search, the server runs the query and continues generating — up to ten
 * rounds — and each search surfaces as a [WebSearchCallItem] in the output. `search_context_size`
 * and `user_location` are accepted by the API but ignored.
 *
 * ```kotlin
 * val params = responseParams { tools = listOf(WebSearchTool.webSearch) }
 * ```
 *
 * @property type Raw tool type, either `web_search` or a dated variant of it.
 */
public class WebSearchTool private constructor(override val type: String) : ResponseTool {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSearchTool) return false
        return type == other.type
    }

    override fun hashCode(): Int = type.hashCode()

    override fun toString(): String = "WebSearchTool(type='$type')"

    public companion object {
        /** `web_search` — the current web search tool. */
        public val webSearch: WebSearchTool = WebSearchTool(WEB_SEARCH)

        /** `web_search_2025_08_26` — the dated alias of [webSearch]. */
        public val webSearch20250826: WebSearchTool = WebSearchTool(WEB_SEARCH_DATED)

        /**
         * Wraps a web search tool type the SDK declares no constant for.
         *
         * @param type Raw tool type to send
         */
        public fun of(type: String): WebSearchTool = WebSearchTool(type)
    }
}

/**
 * A free-form tool that takes raw text instead of JSON arguments.
 *
 * The API only serves `apply_patch`, the tool Codex uses to edit files; any other name is rejected
 * with 400. The model answers with a [CustomToolCallItem].
 *
 * @property name Name of the custom tool; in practice `apply_patch`.
 */
@Serializable
public class CustomTool(public val name: String) : ResponseTool {
    override val type: String
        get() = CUSTOM

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomTool) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "CustomTool(name='$name')"

    public companion object {
        /** `apply_patch` — the only custom tool the API serves. */
        public val applyPatch: CustomTool = CustomTool("apply_patch")
    }
}

/**
 * A tool whose `type` this version of the SDK does not model, kept verbatim so it round-trips.
 *
 * @property type Raw `type` discriminator.
 * @property raw The tool exactly as it was read, discriminator included.
 */
public class UnknownTool(
    override val type: String,
    public val raw: JsonObject,
) : ResponseTool {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnknownTool) return false
        return type == other.type && raw == other.raw
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + raw.hashCode()
        return result
    }

    override fun toString(): String = "UnknownTool(type='$type', raw=$raw)"
}

internal const val FUNCTION: String = "function"
internal const val WEB_SEARCH: String = "web_search"
internal const val WEB_SEARCH_DATED: String = "web_search_2025_08_26"
internal const val CUSTOM: String = "custom"

/** Writes the flat `{"type": ..., ...}` shape the API expects for a tool, and reads it back. */
internal object ResponseToolSerializer : KSerializer<ResponseTool> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.responses.ResponseTool",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: ResponseTool) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val element =
            when (value) {
                is FunctionTool ->
                    withType(value.type, output.json.encode(FunctionTool.serializer(), value))
                is CustomTool ->
                    withType(value.type, output.json.encode(CustomTool.serializer(), value))
                is WebSearchTool -> withType(value.type, JsonObject(emptyMap()))
                // Kept verbatim: re-adding a discriminator we never parsed would change the
                // payload.
                is UnknownTool -> value.raw
            }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ResponseTool {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val element = input.decodeJsonElement().jsonObject
        val type = element.discriminator()
        val body = JsonObject(element - "type")
        return when (type) {
            FUNCTION -> input.json.decodeFromJsonElement(FunctionTool.serializer(), body)
            CUSTOM -> input.json.decodeFromJsonElement(CustomTool.serializer(), body)
            WEB_SEARCH,
            WEB_SEARCH_DATED -> WebSearchTool.of(type)
            else -> UnknownTool(type, element)
        }
    }
}

/**
 * Controls which tool, if any, the model calls.
 *
 * Implementations:
 * - [ResponseToolChoiceMode] — coarse strategies (`none`, `auto`, `required`).
 * - [NamedResponseToolChoice] — pin the model to one tool.
 *
 * When omitted the API defaults to `auto`. Naming a tool that is absent from the request's tool
 * list is answered with 400.
 */
@Serializable(with = ResponseToolChoiceSerializer::class) public sealed interface ResponseToolChoice

/** Coarse tool-selection strategy. */
@Serializable
public enum class ResponseToolChoiceMode : ResponseToolChoice {
    /** The model must not call any tool and must instead produce a message. */
    @SerialName("none") NONE,

    /** The model decides whether to answer or call tools. The API's default. */
    @SerialName("auto") AUTO,

    /** The model must call at least one of the provided tools. */
    @SerialName("required") REQUIRED,
}

/**
 * Forces the model to call one specific tool.
 *
 * ```kotlin
 * val params = responseParams {
 *     tools = listOf(FunctionTool("get_weather"))
 *     toolChoice = NamedResponseToolChoice.function("get_weather")
 * }
 * ```
 *
 * @property type Type of the tool that must be called.
 * @property name Name of the function to call; set only when [type] is `function`.
 */
@Serializable
public class NamedResponseToolChoice
private constructor(
    public val type: String,
    public val name: String? = null,
) : ResponseToolChoice {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NamedResponseToolChoice) return false
        return type == other.type && name == other.name
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = "NamedResponseToolChoice(type='$type', name=$name)"

    public companion object {
        /**
         * Forces a call to the named function.
         *
         * @param name Name of a [FunctionTool] present in the request's tool list
         */
        public fun function(name: String): NamedResponseToolChoice =
            NamedResponseToolChoice(FUNCTION, name)

        /** Forces the model to run a web search. Requires a [WebSearchTool] in the tool list. */
        public val webSearch: NamedResponseToolChoice = NamedResponseToolChoice(WEB_SEARCH)
    }
}

/** Reads the bare-string form as a [ResponseToolChoiceMode] and the object form as a named one. */
internal object ResponseToolChoiceSerializer : KSerializer<ResponseToolChoice> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.responses.ResponseToolChoice",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: ResponseToolChoice) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val element =
            when (value) {
                is ResponseToolChoiceMode ->
                    output.json.encodeToJsonElement(ResponseToolChoiceMode.serializer(), value)
                is NamedResponseToolChoice ->
                    output.json.encodeToJsonElement(NamedResponseToolChoice.serializer(), value)
            }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ResponseToolChoice {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonObject ->
                input.json.decodeFromJsonElement(NamedResponseToolChoice.serializer(), element)
            else -> input.json.decodeFromJsonElement(ResponseToolChoiceMode.serializer(), element)
        }
    }
}
