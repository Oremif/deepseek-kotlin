package org.oremif.deepseek.models.responses

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ImageDetail

/**
 * A request to the Responses API.
 *
 * The conversation travels in [input], either as one user message ([ResponseInput.Text]) or as a
 * list of items ([ResponseInput.Items]) — the API is stateless, so the list carries the whole
 * history every turn. At least one of [input] and [instructions] must be present.
 *
 * Build one through [ResponseParams.createRequest] or the `createResponse { }` DSL rather than by
 * hand when more than the input varies.
 *
 * @property model ID of the model to use.
 *
 * **Possible values: {`deepseek-flash`, `deepseek-v4-pro`, `deepseek-flash`}.**
 *
 * @property input The conversation, as one user message or as a list of items; `null` when the
 *   request is driven by [instructions] alone.
 * @property instructions System-level instruction, inserted as the first system message of the
 *   model's context.
 * @property reasoning Thinking-mode configuration; see [ReasoningConfig].
 * @property maxOutputTokens Ceiling on the tokens generated for the response, counting both the
 *   visible answer and the reasoning.
 * @property stream If set, the response is streamed as semantic server-sent events, ending with
 *   `response.completed` / `response.incomplete` / `response.failed` — there is no `[DONE]` marker.
 * @property temperature Sampling temperature between `0` and `2`; has no effect in thinking mode.
 *
 * **Default value: `1`.**
 *
 * @property topP Nucleus sampling mass, at most `1`; has no effect in thinking mode.
 *
 * **Default value: `1`.**
 *
 * @property text Shape of the answer, including JSON-Schema-constrained output; see [TextConfig].
 * @property tools Tools the model may use — functions, and the server-side web search.
 * @property toolChoice Which tool, if any, the model must call.
 * @property topLogprobs How many most likely tokens to report at each position.
 *
 * **Possible values: `<= 20`.**
 *
 * @property user Custom identifier of the end user behind the request, used for content safety
 *   review, KVCache isolation, scheduling isolation and privacy management. Must not carry personal
 *   data.
 *
 * **Possible values: 1 to 512 characters from `[a-zA-Z0-9\-_]`.**
 *
 * @see <a href="https://api-docs.deepseek.com/api/create-response">Create Response</a>
 */
@Serializable
public class ResponseRequest(
    public val model: ChatModel,
    public val input: ResponseInput? = null,
    public val instructions: String? = null,
    public val reasoning: ReasoningConfig? = null,
    public val maxOutputTokens: Int? = null,
    public val stream: Boolean? = null,
    public val temperature: Double? = null,
    public val topP: Double? = null,
    public val text: TextConfig? = null,
    public val tools: List<ResponseTool>? = null,
    public val toolChoice: ResponseToolChoice? = null,
    public val topLogprobs: Int? = null,
    public val user: String? = null,
) {
    init {
        require(input != null || instructions != null) {
            "a response request needs at least one of input and instructions"
        }
    }

    /**
     * Creates a request whose input is a single user message.
     *
     * @param model ID of the model to use
     * @param input Text of the user's message
     */
    public constructor(
        model: ChatModel,
        input: String,
        instructions: String? = null,
        reasoning: ReasoningConfig? = null,
        maxOutputTokens: Int? = null,
        stream: Boolean? = null,
        temperature: Double? = null,
        topP: Double? = null,
        text: TextConfig? = null,
        tools: List<ResponseTool>? = null,
        toolChoice: ResponseToolChoice? = null,
        topLogprobs: Int? = null,
        user: String? = null,
    ) : this(
        model,
        ResponseInput.Text(input),
        instructions,
        reasoning,
        maxOutputTokens,
        stream,
        temperature,
        topP,
        text,
        tools,
        toolChoice,
        topLogprobs,
        user,
    )

    /**
     * Creates a request whose input is a list of conversation items.
     *
     * @param model ID of the model to use
     * @param input Conversation so far, in the order the model should see it
     */
    public constructor(
        model: ChatModel,
        input: List<ResponseItem>,
        instructions: String? = null,
        reasoning: ReasoningConfig? = null,
        maxOutputTokens: Int? = null,
        stream: Boolean? = null,
        temperature: Double? = null,
        topP: Double? = null,
        text: TextConfig? = null,
        tools: List<ResponseTool>? = null,
        toolChoice: ResponseToolChoice? = null,
        topLogprobs: Int? = null,
        user: String? = null,
    ) : this(
        model,
        ResponseInput.Items(input),
        instructions,
        reasoning,
        maxOutputTokens,
        stream,
        temperature,
        topP,
        text,
        tools,
        toolChoice,
        topLogprobs,
        user,
    )

    /**
     * Builder for non-streaming Responses API requests.
     *
     * Used by `DeepSeekClient.createResponse { ... }`; compose [params] and [input] inside the
     * block.
     */
    public class Builder {
        private var params: ResponseParams = ResponseParams()
        private var input: ResponseInput? = null

        /**
         * Configures request parameters (model, instructions, reasoning, tools, etc.).
         *
         * Replaces any previously-configured parameters.
         *
         * @param block Builder block applied to a fresh [ResponseParams.Builder]
         */
        public fun params(block: ResponseParams.Builder.() -> Unit) {
            params = ResponseParams.Builder().apply(block).build()
        }

        /**
         * Sends a single user message as the whole input.
         *
         * @param text Text of the user's message
         */
        public fun input(text: String) {
            input = ResponseInput.Text(text)
        }

        /**
         * Sends a ready-made conversation as the input.
         *
         * @param items Conversation so far, in the order the model should see it
         */
        public fun input(items: List<ResponseItem>) {
            input = ResponseInput.Items(items)
        }

        /**
         * Builds the conversation via an [InputBuilder] DSL.
         *
         * @param block Builder block that appends items using [InputBuilder.user],
         *   [InputBuilder.assistant], etc.
         */
        public fun input(block: InputBuilder.() -> Unit) {
            input = ResponseInput.Items(InputBuilder().apply(block).build())
        }

        internal fun build(): ResponseRequest = params.createRequest(input)
    }

    /**
     * Builder for streaming Responses API requests.
     *
     * Used by `DeepSeekClientStream.createResponse { ... }`; the resulting request has `stream =
     * true`.
     */
    public class StreamBuilder {
        private var params: ResponseParams = ResponseParams(stream = true)
        private var input: ResponseInput? = null

        /**
         * Configures streaming request parameters.
         *
         * Replaces any previously-configured parameters.
         *
         * @param block Builder block applied to a fresh [ResponseParams.StreamBuilder]
         */
        public fun params(block: ResponseParams.StreamBuilder.() -> Unit) {
            params = ResponseParams.StreamBuilder().apply(block).build()
        }

        /**
         * Sends a single user message as the whole input.
         *
         * @param text Text of the user's message
         */
        public fun input(text: String) {
            input = ResponseInput.Text(text)
        }

        /**
         * Sends a ready-made conversation as the input.
         *
         * @param items Conversation so far, in the order the model should see it
         */
        public fun input(items: List<ResponseItem>) {
            input = ResponseInput.Items(items)
        }

        /**
         * Builds the conversation via an [InputBuilder] DSL.
         *
         * @param block Builder block that appends items using [InputBuilder.user],
         *   [InputBuilder.assistant], etc.
         */
        public fun input(block: InputBuilder.() -> Unit) {
            input = ResponseInput.Items(InputBuilder().apply(block).build())
        }

        internal fun build(): ResponseRequest = params.createRequest(input)
    }

    /**
     * DSL for building the input of a Responses API call.
     *
     * Appends items in call order. Because the API is stateless, a previous response's output is
     * appended verbatim with [items] to continue the conversation.
     *
     * Example:
     * ```kotlin
     * client.createResponse {
     *     input {
     *         user("What is the weather in Tokyo?")
     *         items(previous.output)
     *         functionCallOutput(callId, """{"temp_c": 21}""")
     *     }
     * }
     * ```
     */
    public class InputBuilder {
        private val items = mutableListOf<ResponseItem>()

        /**
         * Appends a user message.
         *
         * @param text User's text input
         */
        public fun user(text: String) {
            items.add(MessageItem(ResponseRole.USER, text))
        }

        /**
         * Appends a multimodal user message assembled by a [ContentBuilder] DSL.
         *
         * @param block Builder block that appends the message's content parts
         */
        public fun user(block: ContentBuilder.() -> Unit) {
            items.add(MessageItem(ResponseRole.USER, ContentBuilder().apply(block).build()))
        }

        /**
         * Appends a system message.
         *
         * Prefer `instructions` in the parameters for the leading system prompt; use this to place
         * one mid-conversation. Image parts are rejected in this role.
         *
         * @param text Instructions for the model
         */
        public fun system(text: String) {
            items.add(MessageItem(ResponseRole.SYSTEM, text))
        }

        /**
         * Appends a developer message, which the API treats exactly like a user message.
         *
         * @param text Developer's text input
         */
        public fun developer(text: String) {
            items.add(MessageItem(ResponseRole.DEVELOPER, text))
        }

        /**
         * Appends an assistant message, replaying an earlier model turn as context.
         *
         * @param text Assistant text to replay
         */
        public fun assistant(text: String) {
            items.add(MessageItem(ResponseRole.ASSISTANT, text))
        }

        /**
         * Appends a message with an arbitrary role.
         *
         * @param role Author of the message
         * @param text Message text
         */
        public fun message(role: ResponseRole, text: String) {
            items.add(MessageItem(role, text))
        }

        /**
         * Appends a chain-of-thought item, replaying the reasoning of an earlier turn.
         *
         * @param text Reasoning text
         */
        public fun reasoning(text: String) {
            items.add(ReasoningItem(text))
        }

        /**
         * Appends a function call, replaying one the model emitted earlier.
         *
         * @param callId Identifier pairing the call with its output
         * @param name Name of the function
         * @param arguments Arguments as a raw JSON string
         */
        public fun functionCall(callId: String, name: String, arguments: String) {
            items.add(FunctionCallItem(callId, name, arguments))
        }

        /**
         * Appends the result of running a function the model called.
         *
         * @param callId Identifier of the call this output answers
         * @param output Tool result, typically JSON-encoded
         */
        public fun functionCallOutput(callId: String, output: String) {
            items.add(FunctionCallOutputItem(callId, output))
        }

        /**
         * Appends the result of running a function, as content parts — which lets a tool hand the
         * model an image it produced.
         *
         * @param callId Identifier of the call this output answers
         * @param block Builder block that appends the result's content parts
         */
        public fun functionCallOutput(callId: String, block: ContentBuilder.() -> Unit) {
            items.add(FunctionCallOutputItem(callId, ContentBuilder().apply(block).build()))
        }

        /**
         * Appends a ready-made item.
         *
         * @param item Item to append
         */
        public fun item(item: ResponseItem) {
            items.add(item)
        }

        /**
         * Appends ready-made items, typically the `output` of a previous response.
         *
         * @param items Items to append, in order
         */
        public fun items(items: List<ResponseItem>) {
            this.items.addAll(items)
        }

        internal fun build(): List<ResponseItem> {
            require(items.isNotEmpty()) { "input built with the DSL must have at least one item" }
            return items.toList()
        }
    }

    /**
     * DSL for building the multimodal content of a single message or tool output.
     *
     * Appends [ResponseContentPart]s in call order; at least one part is required. Images are only
     * read by both models the API serves.
     *
     * Example:
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
     */
    public class ContentBuilder {
        private val parts = mutableListOf<ResponseContentPart>()

        /**
         * Appends an [InputTextPart].
         *
         * @param text Text fragment shown to the model
         */
        public fun text(text: String) {
            parts.add(InputTextPart(text))
        }

        /**
         * Appends an [OutputTextPart], the shape an assistant turn comes back in.
         *
         * @param text Text fragment produced by the model
         */
        public fun outputText(text: String) {
            parts.add(OutputTextPart(text))
        }

        /**
         * Appends an [InputImagePart] referencing the image by URL.
         *
         * @param url `http(s)` link to the image, or a `data:` URL carrying it inline
         * @param detail How much of the image's resolution the model sees
         */
        public fun image(url: String, detail: ImageDetail? = null) {
            parts.add(InputImagePart(imageUrl = url, detail = detail))
        }

        /**
         * Appends an [InputImagePart] referencing an image already uploaded to the Files API.
         *
         * @param fileId Identifier of the uploaded file, of the form `file-api-...`
         */
        public fun imageFile(fileId: String) {
            parts.add(InputImagePart(fileId = fileId))
        }

        /**
         * Appends a ready-made content part.
         *
         * @param part Part to append
         */
        public fun part(part: ResponseContentPart) {
            parts.add(part)
        }

        internal fun build(): List<ResponseContentPart> {
            require(parts.isNotEmpty()) {
                "content built with the parts DSL must have at least one part"
            }
            return parts.toList()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseRequest) return false
        return model == other.model &&
            input == other.input &&
            instructions == other.instructions &&
            reasoning == other.reasoning &&
            maxOutputTokens == other.maxOutputTokens &&
            stream == other.stream &&
            temperature == other.temperature &&
            topP == other.topP &&
            text == other.text &&
            tools == other.tools &&
            toolChoice == other.toolChoice &&
            topLogprobs == other.topLogprobs &&
            user == other.user
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        result = 31 * result + input.hashCode()
        result = 31 * result + (instructions?.hashCode() ?: 0)
        result = 31 * result + (reasoning?.hashCode() ?: 0)
        result = 31 * result + (maxOutputTokens ?: 0)
        result = 31 * result + (stream?.hashCode() ?: 0)
        result = 31 * result + (temperature?.hashCode() ?: 0)
        result = 31 * result + (topP?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (tools?.hashCode() ?: 0)
        result = 31 * result + (toolChoice?.hashCode() ?: 0)
        result = 31 * result + (topLogprobs ?: 0)
        result = 31 * result + (user?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ResponseRequest(model=$model, input=$input, " +
            "instructions=$instructions, reasoning=$reasoning, maxOutputTokens=$maxOutputTokens, " +
            "stream=$stream, temperature=$temperature, topP=$topP, text=$text, tools=$tools, " +
            "toolChoice=$toolChoice, topLogprobs=$topLogprobs, user=$user)"
}

/**
 * The two shapes the `input` of a [ResponseRequest] takes on the wire: a bare string, treated by
 * the API as a single user message, or an array of [ResponseItem]s.
 *
 * The request constructors and [ResponseParams.createRequest] wrap a `String` or a
 * `List<ResponseItem>` for you, so this type mostly shows up when reading a request back:
 * ```kotlin
 * when (val input = request.input) {
 *     is ResponseInput.Text -> println(input.text)
 *     is ResponseInput.Items -> println("${'$'}{input.items.size} items")
 *     null -> println("instructions only")
 * }
 * ```
 */
@Serializable(with = ResponseInputSerializer::class)
public sealed interface ResponseInput {

    /**
     * A single user message, sent as a bare JSON string.
     *
     * @property text The message text.
     */
    public class Text(public val text: String) : ResponseInput {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Text) return false
            return text == other.text
        }

        override fun hashCode(): Int = text.hashCode()

        override fun toString(): String = "ResponseInput.Text(text='${'$'}text')"
    }

    /**
     * A conversation, sent as a JSON array of items.
     *
     * @property items The items, in the order the model sees them.
     */
    public class Items(public val items: List<ResponseItem>) : ResponseInput {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Items) return false
            return items == other.items
        }

        override fun hashCode(): Int = items.hashCode()

        override fun toString(): String = "ResponseInput.Items(items=${'$'}items)"
    }
}

/** Encodes [ResponseInput.Text] as a JSON string and [ResponseInput.Items] as a JSON array. */
internal object ResponseInputSerializer : KSerializer<ResponseInput> {
    private val itemsSerializer = ListSerializer(ResponseItemSerializer)

    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "org.oremif.deepseek.models.responses.ResponseInput",
            JsonElement.serializer().descriptor,
        )

    override fun serialize(encoder: Encoder, value: ResponseInput) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        val element =
            when (value) {
                is ResponseInput.Text -> JsonPrimitive(value.text)
                is ResponseInput.Items ->
                    output.json.encodeToJsonElement(itemsSerializer, value.items)
            }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): ResponseInput {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonArray ->
                ResponseInput.Items(input.json.decodeFromJsonElement(itemsSerializer, element))
            else -> ResponseInput.Text(element.jsonPrimitive.content)
        }
    }
}
