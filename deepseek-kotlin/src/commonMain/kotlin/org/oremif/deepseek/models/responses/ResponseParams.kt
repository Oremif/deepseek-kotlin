package org.oremif.deepseek.models.responses

import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.requireValidUserId

/**
 * Creates Responses API parameters using a builder.
 *
 * Example:
 * ```kotlin
 * val params = responseParams {
 *     model = ChatModel.DEEPSEEK_V4_PRO
 *     instructions = "You are a helpful assistant."
 *     reasoning = ReasoningConfig(ReasoningEffort.LOW)
 *     maxOutputTokens = 2000
 * }
 * ```
 *
 * @param block Configuration block for setting parameter values
 * @return Configured [ResponseParams] instance
 */
public fun responseParams(block: ResponseParams.Builder.() -> Unit): ResponseParams =
    ResponseParams.Builder().apply(block).build()

/**
 * Creates streaming Responses API parameters using a builder.
 *
 * The resulting parameters have `stream` set to `true`.
 *
 * Example:
 * ```kotlin
 * val params = responseStreamParams {
 *     model = ChatModel.DEEPSEEK_FLASH
 *     instructions = "You are a helpful assistant."
 * }
 * ```
 *
 * @param block Configuration block for setting parameter values
 * @return Configured [ResponseParams] instance with streaming enabled
 */
public fun responseStreamParams(block: ResponseParams.StreamBuilder.() -> Unit): ResponseParams =
    ResponseParams.StreamBuilder().apply(block).build()

/**
 * Everything about a Responses API call except the conversation itself.
 *
 * The Responses API is stateless, so the same parameters are reused turn after turn while only the
 * input grows:
 * ```kotlin
 * val params = responseParams { instructions = "You are a helpful assistant." }
 * val history = mutableListOf<ResponseItem>(MessageItem(ResponseRole.USER, "Hi"))
 * val first = client.createResponse(params, history)
 * history += first.output
 * ```
 *
 * Every constraint the API documents is checked on construction, so an invalid combination fails
 * here rather than as a 400 from the server.
 *
 * @property model The DeepSeek model to use; defaults to [ChatModel.DEEPSEEK_FLASH].
 * @property instructions System-level instruction, inserted as the first system message of the
 *   model's context. Either this or an input is required.
 * @property reasoning Thinking-mode configuration; see [ReasoningConfig].
 * @property maxOutputTokens Ceiling on the tokens generated for the response, counting both the
 *   visible answer and the reasoning. At least `1`.
 * @property stream Whether the response is streamed as semantic server-sent events.
 * @property temperature Sampling temperature between `0.0` and `2.0`; has no effect in thinking
 *   mode.
 * @property topP Nucleus sampling mass between `0.0` and `1.0`; has no effect in thinking mode.
 * @property text Shape of the answer, including JSON-Schema-constrained output; see [TextConfig].
 * @property tools Tools the model may use — functions, and the server-side web search.
 * @property toolChoice Which tool, if any, the model must call.
 * @property topLogprobs How many most likely tokens to report at each position; `0` to `20`.
 * @property user Custom identifier of the end user behind the request; 1 to 512 characters from
 *   `[a-zA-Z0-9\-_]`. Must not carry personal data. The chat endpoint spells the same field
 *   `user_id`.
 * @see <a href="https://api-docs.deepseek.com/api/create-response">Create Response</a>
 */
public class ResponseParams
internal constructor(
    public val model: ChatModel = ChatModel.DEEPSEEK_FLASH,
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
        maxOutputTokens?.let { require(it >= 1) { "maxOutputTokens must be >= 1" } }
        temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
        topP?.let { require(it in 0.0..1.0) { "topP must be between 0.0 and 1.0" } }
        topLogprobs?.let { require(it in 0..20) { "topLogprobs must be between 0 and 20" } }
        user?.let(::requireValidUserId)
        tools?.let(::requireUniqueToolNames)
    }

    /** Builder for creating [ResponseParams] with standard (non-streaming) configuration. */
    public class Builder {
        public var model: ChatModel = ChatModel.DEEPSEEK_FLASH
        public var instructions: String? = null
        public var reasoning: ReasoningConfig? = null
        public var maxOutputTokens: Int? = null
        public var temperature: Double? = null
        public var topP: Double? = null
        public var text: TextConfig? = null
        public var tools: List<ResponseTool>? = null
        public var toolChoice: ResponseToolChoice? = null
        public var topLogprobs: Int? = null
        public var user: String? = null

        internal fun build(): ResponseParams =
            ResponseParams(
                model = model,
                instructions = instructions,
                reasoning = reasoning,
                maxOutputTokens = maxOutputTokens,
                temperature = temperature,
                topP = topP,
                text = text,
                tools = tools,
                toolChoice = toolChoice,
                topLogprobs = topLogprobs,
                user = user,
            )
    }

    /** Builder for creating [ResponseParams] configured for streaming responses. */
    public class StreamBuilder {
        public var model: ChatModel = ChatModel.DEEPSEEK_FLASH
        public var instructions: String? = null
        public var reasoning: ReasoningConfig? = null
        public var maxOutputTokens: Int? = null
        public var temperature: Double? = null
        public var topP: Double? = null
        public var text: TextConfig? = null
        public var tools: List<ResponseTool>? = null
        public var toolChoice: ResponseToolChoice? = null
        public var topLogprobs: Int? = null
        public var user: String? = null

        internal fun build(): ResponseParams =
            ResponseParams(
                model = model,
                instructions = instructions,
                reasoning = reasoning,
                maxOutputTokens = maxOutputTokens,
                stream = true,
                temperature = temperature,
                topP = topP,
                text = text,
                tools = tools,
                toolChoice = toolChoice,
                topLogprobs = topLogprobs,
                user = user,
            )
    }

    /**
     * Creates a [ResponseRequest] carrying a conversation.
     *
     * @param input Conversation so far, in the order the model should see it
     * @return A fully configured [ResponseRequest]
     */
    public fun createRequest(input: List<ResponseItem>): ResponseRequest =
        createRequest(ResponseInput.Items(input))

    /**
     * Creates a [ResponseRequest] carrying a single user message.
     *
     * @param input Text of the user's message
     * @return A fully configured [ResponseRequest]
     */
    public fun createRequest(input: String): ResponseRequest =
        createRequest(ResponseInput.Text(input))

    /**
     * Creates a [ResponseRequest] with no input, driven by [instructions] alone.
     *
     * @return A fully configured [ResponseRequest]
     * @throws IllegalArgumentException if [instructions] is `null`, since the API requires at least
     *   one of the two
     */
    public fun createRequest(): ResponseRequest = createRequest(input = null)

    /**
     * Creates a [ResponseRequest] from an already-shaped input.
     *
     * @param input The conversation, or `null` to rely on [instructions] alone
     * @return A fully configured [ResponseRequest]
     */
    public fun createRequest(input: ResponseInput?): ResponseRequest =
        ResponseRequest(
            model = model,
            input = input,
            instructions = instructions,
            reasoning = reasoning,
            maxOutputTokens = maxOutputTokens,
            stream = stream,
            temperature = temperature,
            topP = topP,
            text = text,
            tools = tools,
            toolChoice = toolChoice,
            topLogprobs = topLogprobs,
            user = user,
        )

    /**
     * Creates a copy of these parameters with optional changes to specific properties.
     *
     * Unlike the chat and FIM parameters, the copy is validated exactly like a freshly built one.
     *
     * Example:
     * ```kotlin
     * val streaming = params.copy(stream = true)
     * ```
     *
     * @param model New model, or the existing one if not specified
     * @param instructions New system-level instruction, or the existing one if not specified
     * @param reasoning New thinking-mode configuration, or the existing one if not specified
     * @param maxOutputTokens New output ceiling, or the existing one if not specified
     * @param stream New streaming setting, or the existing one if not specified
     * @param temperature New temperature, or the existing one if not specified
     * @param topP New top-p value, or the existing one if not specified
     * @param text New output shape, or the existing one if not specified
     * @param tools New tool list, or the existing one if not specified
     * @param toolChoice New tool choice, or the existing one if not specified
     * @param topLogprobs New log-probability count, or the existing one if not specified
     * @param user New end-user identifier, or the existing one if not specified
     * @return A new [ResponseParams] instance with the specified changes
     */
    public fun copy(
        model: ChatModel = this.model,
        instructions: String? = this.instructions,
        reasoning: ReasoningConfig? = this.reasoning,
        maxOutputTokens: Int? = this.maxOutputTokens,
        stream: Boolean? = this.stream,
        temperature: Double? = this.temperature,
        topP: Double? = this.topP,
        text: TextConfig? = this.text,
        tools: List<ResponseTool>? = this.tools,
        toolChoice: ResponseToolChoice? = this.toolChoice,
        topLogprobs: Int? = this.topLogprobs,
        user: String? = this.user,
    ): ResponseParams =
        ResponseParams(
            model = model,
            instructions = instructions,
            reasoning = reasoning,
            maxOutputTokens = maxOutputTokens,
            stream = stream,
            temperature = temperature,
            topP = topP,
            text = text,
            tools = tools,
            toolChoice = toolChoice,
            topLogprobs = topLogprobs,
            user = user,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseParams) return false
        return model == other.model &&
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
        result = 31 * result + instructions.hashCode()
        result = 31 * result + reasoning.hashCode()
        result = 31 * result + (maxOutputTokens ?: 0)
        result = 31 * result + stream.hashCode()
        result = 31 * result + temperature.hashCode()
        result = 31 * result + topP.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + tools.hashCode()
        result = 31 * result + toolChoice.hashCode()
        result = 31 * result + (topLogprobs ?: 0)
        result = 31 * result + user.hashCode()
        return result
    }

    override fun toString(): String =
        "ResponseParams(model=$model, instructions=$instructions, reasoning=$reasoning, " +
            "maxOutputTokens=$maxOutputTokens, stream=$stream, temperature=$temperature, topP=$topP, " +
            "text=$text, tools=$tools, toolChoice=$toolChoice, topLogprobs=$topLogprobs, user=$user)"
}

/**
 * Fails fast on a tool list the API would reject: it requires function names to be unique across
 * the whole list.
 */
internal fun requireUniqueToolNames(tools: List<ResponseTool>) {
    val names = tools.filterIsInstance<FunctionTool>().map { it.name }
    require(names.size == names.toSet().size) { "function tool names must be unique, got $names" }
}
