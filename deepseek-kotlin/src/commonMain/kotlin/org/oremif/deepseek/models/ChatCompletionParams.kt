package org.oremif.deepseek.models

public fun chatCompletionParams(block: ChatCompletionParams.Builder.() -> Unit): ChatCompletionParams {
    return ChatCompletionParams.Builder().apply(block).build()
}

public fun chatCompletionStreamParams(block: ChatCompletionParams.StreamBuilder.() -> Unit): ChatCompletionParams {
    return ChatCompletionParams.StreamBuilder().apply(block).build()
}

public class ChatCompletionParams internal constructor(
    public val model: ChatModel,
    public val frequencyPenalty: Double? = null,
    public val maxTokens: Int? = null,
    public val presencePenalty: Double? = null,
    public val responseFormat: ResponseFormat? = null,
    public val stop: StopReason? = null,
    public val stream: Boolean? = null,
    public val streamOptions: StreamOptions? = null,
    public val temperature: Double? = null,
    public val topP: Double? = null,
    public val tools: List<Tool>? = null,
    public val toolChoice: ToolChoice? = null,
    public val logprobs: Boolean? = null,
    public val topLogprobs: Int? = null,
) {
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