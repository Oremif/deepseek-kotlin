package org.oremif.deepseek.models

/**
 * Creates chat completion parameters using a builder pattern.
 *
 * This function provides a convenient way to configure non-streaming chat completion
 * parameters using Kotlin's DSL-style builder syntax.
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionParams {
 *     model = ChatModel.DEEPSEEK_CHAT
 *     temperature = 0.8
 *     maxTokens = 500
 *     presencePenalty = 0.3
 *     responseFormat = ResponseFormat.JsonObject
 * }
 * ```
 *
 * @param block Configuration block for setting parameter values
 * @return Configured [ChatCompletionParams] instance
 */
public fun chatCompletionParams(block: ChatCompletionParams.Builder.() -> Unit): ChatCompletionParams {
    return ChatCompletionParams.Builder().apply(block).build()
}

/**
 * Creates streaming chat completion parameters using a builder pattern.
 *
 * This function provides a convenient way to configure streaming chat completion
 * parameters using Kotlin's DSL-style builder syntax. The resulting parameters
 * will have `stream` set to `true` automatically.
 *
 * Example:
 * ```kotlin
 * val streamParams = chatCompletionStreamParams {
 *     model = ChatModel.DEEPSEEK_CHAT
 *     temperature = 0.7
 *     streamOptions = StreamOptions(chunkSize = 10)
 * }
 *
 * client.chatStream(streamParams, messages).collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.delta?.content ?: "")
 * }
 * ```
 *
 * @param block Configuration block for setting parameter values
 * @return Configured [ChatCompletionParams] instance with streaming enabled
 */
public fun chatCompletionStreamParams(block: ChatCompletionParams.StreamBuilder.() -> Unit): ChatCompletionParams {
    return ChatCompletionParams.StreamBuilder().apply(block).build()
}

/**
 * Parameters for configuring chat completion requests to DeepSeek models.
 *
 * This class encapsulates all the options available when sending chat completion
 * requests, allowing fine-grained control over the model's behavior and output.
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionParams {
 *     model = ChatModel.DEEPSEEK_CHAT
 *     temperature = 0.7
 *     maxTokens = 1000
 *     presencePenalty = 0.5
 * }
 *
 * client.chat(params, messages)
 * ```
 *
 * @property model The DeepSeek model to use for chat completion
 * @property frequencyPenalty Number between -2.0 and 2.0 that penalizes tokens based on their frequency in the text
 * @property maxTokens Maximum number of tokens to generate, between 1 and 8192
 * @property presencePenalty Number between -2.0 and 2.0 that penalizes tokens based on their presence in the text
 * @property responseFormat Format specification for the model's output
 * @property stop Custom stop sequences that will cause the model to stop generating further tokens
 * @property stream Whether to stream the response back piece by piece
 * @property streamOptions Configuration options for streaming responses
 * @property temperature Controls randomness in responses, between 0.0 and 2.0 (lower is more deterministic)
 * @property topP Controls diversity by limiting to top-p probability mass in token selection
 * @property tools List of tools that the model may use during chat completion
 * @property toolChoice Controls how the model selects tools to use
 * @property logprobs Whether to return log probabilities of output tokens
 * @property topLogprobs How many most likely tokens to return at each position (max 20)
 */
public class ChatCompletionParams internal constructor(
    public val model: ChatModel,
    frequencyPenalty: Double? = null,
    maxTokens: Int? = null,
    presencePenalty: Double? = null,
    public val responseFormat: ResponseFormat? = null,
    stop: StopReason? = null,
    public val stream: Boolean? = null,
    public val streamOptions: StreamOptions? = null,
    temperature: Double? = null,
    topP: Double? = null,
    public val tools: List<Tool>? = null,
    public val toolChoice: ToolChoice? = null,
    public val logprobs: Boolean? = null,
    public val topLogprobs: Int? = null,
) : DeepSeekParams(frequencyPenalty, maxTokens, presencePenalty, stop, temperature, topP) {

    /**
     * Builder for creating [ChatCompletionParams] with standard (non-streaming) configuration.
     */
    public class Builder {
        public var model: ChatModel = ChatModel.DEEPSEEK_CHAT
        public var frequencyPenalty: Double? = null
        public var maxTokens: Int? = null
        public var presencePenalty: Double? = null
        public var responseFormat: ResponseFormat? = null
        public var stop: StopReason? = null
        public var temperature: Double? = null
        public var topP: Double? = null
        public var tools: List<Tool>? = null
        public var toolChoice: ToolChoice? = null
        public var logprobs: Boolean? = null
        public var topLogprobs: Int? = null

        internal fun build(): ChatCompletionParams {
            frequencyPenalty?.let { require(it in -2.0..2.0) { "frequencyPenalty must be between -2.0 and 2.0" } }
            maxTokens?.let { require(it in 1..8192) { "maxTokens must be between 1 and 8192" } }
            presencePenalty?.let { require(it in -2.0..2.0) { "presencePenalty must be between -2.0 and 2.0" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            topLogprobs?.let { require(it <= 20) { "topLogprobs must be <= 20" } }

            return ChatCompletionParams(
                model = model,
                frequencyPenalty = frequencyPenalty,
                maxTokens = maxTokens,
                presencePenalty = presencePenalty,
                responseFormat = responseFormat,
                stop = stop,
                temperature = temperature,
                topP = topP,
                tools = tools,
                toolChoice = toolChoice,
                logprobs = logprobs,
                topLogprobs = topLogprobs,
            )
        }
    }

    /**
     * Builder for creating [ChatCompletionParams] specifically configured for streaming responses.
     */
    public class StreamBuilder {
        public var model: ChatModel = ChatModel.DEEPSEEK_CHAT
        public var frequencyPenalty: Double? = null
        public var maxTokens: Int? = null
        public var presencePenalty: Double? = null
        public var responseFormat: ResponseFormat? = null
        public var stop: StopReason? = null
        public var streamOptions: StreamOptions? = null
        public var temperature: Double? = null
        public var topP: Double? = null
        public var tools: List<Tool>? = null
        public var toolChoice: ToolChoice? = null
        public var logprobs: Boolean? = null
        public var topLogprobs: Int? = null


        internal fun build(): ChatCompletionParams {
            frequencyPenalty?.let { require(it in -2.0..2.0) { "frequencyPenalty must be between -2.0 and 2.0" } }
            maxTokens?.let { require(it in 1..8192) { "maxTokens must be between 1 and 8192" } }
            presencePenalty?.let { require(it in -2.0..2.0) { "presencePenalty must be between -2.0 and 2.0" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            topLogprobs?.let { require(it <= 20) { "topLogprobs must be <= 20" } }

            return ChatCompletionParams(
                model = model,
                frequencyPenalty = frequencyPenalty,
                maxTokens = maxTokens,
                presencePenalty = presencePenalty,
                responseFormat = responseFormat,
                stop = stop,
                stream = true,
                streamOptions = streamOptions,
                temperature = temperature,
                topP = topP,
                tools = tools,
                toolChoice = toolChoice,
                logprobs = logprobs,
                topLogprobs = topLogprobs,
            )
        }
    }

    /**
     * Creates a [ChatCompletionRequest] from these parameters and the provided messages.
     *
     * @param messages List of chat messages to include in the request
     * @return A fully configured [ChatCompletionRequest]
     */
    public fun createRequest(messages: List<ChatMessage>): ChatCompletionRequest =
        ChatCompletionRequest(
            messages = messages,
            model = model,
            frequencyPenalty = frequencyPenalty,
            maxTokens = maxTokens,
            presencePenalty = presencePenalty,
            responseFormat = responseFormat,
            stop = stop,
            stream = stream,
            streamOptions = streamOptions,
            temperature = temperature,
            topP = topP,
            tools = tools,
            toolChoice = toolChoice,
            logprobs = logprobs,
            topLogprobs = topLogprobs,
        )

    /**
     * Creates a copy of these parameters with optional changes to specific properties.
     *
     * Example:
     * ```kotlin
     * // Create a streaming version of existing parameters
     * val streamParams = regularParams.copy(stream = true)
     * ```
     *
     * @param model New chat model to use, or existing value if not specified
     * @param frequencyPenalty New frequency penalty value, or existing value if not specified
     * @param maxTokens New maximum token count, or existing value if not specified
     * @param presencePenalty New presence penalty value, or existing value if not specified
     * @param responseFormat New response format, or existing value if not specified
     * @param stop New stop reason, or existing value if not specified
     * @param stream New streaming setting, or existing value if not specified
     * @param streamOptions New stream options, or existing value if not specified
     * @param temperature New temperature value, or existing value if not specified
     * @param topP New top-p value, or existing value if not specified
     * @param tools New tools list, or existing value if not specified
     * @param toolChoice New tool choice, or existing value if not specified
     * @param logprobs New log probabilities setting, or existing value if not specified
     * @param topLogprobs New top log probabilities count, or existing value if not specified
     * @return A new [ChatCompletionParams] instance with the specified changes
     */
    public fun copy(
        model: ChatModel = this.model,
        frequencyPenalty: Double? = this.frequencyPenalty,
        maxTokens: Int? = this.maxTokens,
        presencePenalty: Double? = this.presencePenalty,
        responseFormat: ResponseFormat? = this.responseFormat,
        stop: StopReason? = this.stop,
        stream: Boolean? = this.stream,
        streamOptions: StreamOptions? = this.streamOptions,
        temperature: Double? = this.temperature,
        topP: Double? = this.topP,
        tools: List<Tool>? = this.tools,
        toolChoice: ToolChoice? = this.toolChoice,
        logprobs: Boolean? = this.logprobs,
        topLogprobs: Int? = this.topLogprobs,
    ): ChatCompletionParams {
        return ChatCompletionParams(
            model = model,
            frequencyPenalty = frequencyPenalty,
            maxTokens = maxTokens,
            presencePenalty = presencePenalty,
            responseFormat = responseFormat,
            stop = stop,
            stream = stream,
            streamOptions = streamOptions,
            temperature = temperature,
            topP = topP,
            tools = tools,
            toolChoice = toolChoice,
            logprobs = logprobs,
            topLogprobs = topLogprobs,
        )
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatCompletionParams) return false

        return model == other.model &&
                frequencyPenalty == other.frequencyPenalty &&
                maxTokens == other.maxTokens &&
                presencePenalty == other.presencePenalty &&
                responseFormat == other.responseFormat &&
                stop == other.stop &&
                stream == other.stream &&
                streamOptions == other.streamOptions &&
                temperature == other.temperature &&
                topP == other.topP &&
                tools == other.tools &&
                toolChoice == other.toolChoice &&
                logprobs == other.logprobs &&
                topLogprobs == other.topLogprobs
    }

    override fun hashCode(): Int {
        var result = frequencyPenalty?.hashCode() ?: 0
        result = 31 * result + (maxTokens ?: 0)
        result = 31 * result + (presencePenalty?.hashCode() ?: 0)
        result = 31 * result + (stream?.hashCode() ?: 0)
        result = 31 * result + (temperature?.hashCode() ?: 0)
        result = 31 * result + (topP?.hashCode() ?: 0)
        result = 31 * result + (logprobs?.hashCode() ?: 0)
        result = 31 * result + (topLogprobs ?: 0)
        result = 31 * result + model.hashCode()
        result = 31 * result + (responseFormat?.hashCode() ?: 0)
        result = 31 * result + (stop?.hashCode() ?: 0)
        result = 31 * result + (streamOptions?.hashCode() ?: 0)
        result = 31 * result + (tools?.hashCode() ?: 0)
        result = 31 * result + (toolChoice?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ChatCompletionParams(model=$model, frequencyPenalty=$frequencyPenalty, maxTokens=$maxTokens, presencePenalty=$presencePenalty, responseFormat=$responseFormat, stop=$stop, stream=$stream, streamOptions=$streamOptions, temperature=$temperature, topP=$topP, tools=$tools, toolChoice=$toolChoice, logprobs=$logprobs, topLogprobs=$topLogprobs)"
    }
}