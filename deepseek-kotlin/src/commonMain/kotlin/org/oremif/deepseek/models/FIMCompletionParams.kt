@file:Suppress("DEPRECATION")

package org.oremif.deepseek.models

/**
 * Creates FIM completion parameters using a builder pattern.
 *
 * This function provides a convenient way to configure non-streaming FIM completion
 * parameters using Kotlin's DSL-style builder syntax.
 *
 * Example:
 * ```kotlin
 * val params = fimCompletionParams {
 *     temperature = 0.8
 *     maxTokens = 500
 *     suffix = "END OF DOCUMENT"
 * }
 * ```
 *
 * @param block Configuration block for setting parameter values
 * @return Configured [FIMCompletionParams] instance
 */
public fun fimCompletionParams(block: FIMCompletionParams.Builder.() -> Unit): FIMCompletionParams {
    return FIMCompletionParams.Builder().apply(block).build()
}

/**
 * Creates streaming FIM completion parameters using a builder pattern.
 *
 * This function provides a convenient way to configure streaming FIM completion
 * parameters using Kotlin's DSL-style builder syntax. The resulting parameters
 * will have `stream` set to `true` automatically.
 *
 * Streaming is particularly useful for long completions, allowing you to process
 * and display results incrementally as they arrive.
 *
 * Example:
 * ```kotlin
 * val streamParams = fimCompletionStreamParams {
 *     temperature = 0.7
 *     streamOptions = StreamOptions(includeUsage = true)
 *     suffix = "}"
 * }
 *
 * streamClient.fim(streamParams, "function calculate() {").collect { chunk ->
 *     print(chunk.choices.firstOrNull()?.text ?: "")
 * }
 * ```
 *
 * @param block Configuration block for setting parameter values
 * @return Configured [FIMCompletionParams] instance with streaming enabled
 */
public fun fimCompletionStreamParams(block: FIMCompletionParams.StreamBuilder.() -> Unit): FIMCompletionParams {
    return FIMCompletionParams.StreamBuilder().apply(block).build()
}

/** Rejects [ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP]; any other slug is left to the server. */
internal fun requireFimModel(model: ChatModel) {
    require(model != ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP) {
        "FIM completion is not supported by ${ChatModel.DEEPSEEK_V4_FLASH_VISION_EXP}, use ChatModel.DEEPSEEK_V4_PRO"
    }
}

/**
 * Parameters for configuring Fill-in-the-Middle (FIM) completion requests to DeepSeek models.
 *
 * FIM allows the model to complete text where you provide both a prefix and a suffix,
 * making it useful for code completion, text infilling, and template-based generation.
 *
 * Example:
 * ```kotlin
 * val params = fimCompletionParams {
 *     temperature = 0.7
 *     maxTokens = 200
 *     suffix = "}"  // The code should end with a closing bracket
 * }
 *
 * val completion = client.fim(params, "function calculateArea(radius) {")
 * println(completion.choices.first().text)
 * ```
 *
 * @property model The DeepSeek model to use; the FIM endpoint only accepts
 * [ChatModel.DEEPSEEK_V4_PRO], which is the default
 * @property echo Whether to include the prompt in the returned completion
 * @property frequencyPenalty Sent as `frequency_penalty`, ignored by the API
 * @property logprobs Maximum number of log probabilities to return (up to 20)
 * @property maxTokens Maximum number of tokens to generate; at least 1 and otherwise
 * bounded by the model's context length
 * @property presencePenalty Sent as `presence_penalty`, ignored by the API
 * @property stop Custom stop sequences that will cause the model to stop generating further tokens
 * @property stream Whether to stream the response back piece by piece
 * @property streamOptions Configuration options for streaming responses
 * @property suffix Text that the model should complete towards (the ending part in FIM)
 * @property temperature Controls randomness in responses, between 0.0 and 2.0 (lower is more deterministic)
 * @property topP Controls diversity by limiting to top-p probability mass in token selection
 */
public class FIMCompletionParams internal constructor(
    public val model: ChatModel = ChatModel.DEEPSEEK_V4_PRO,
    public val echo: Boolean? = null,
    frequencyPenalty: Double? = null,
    public val logprobs: Int? = null,
    maxTokens: Int? = null,
    presencePenalty: Double? = null,
    stop: StopReason? = null,
    public val stream: Boolean? = null,
    public val streamOptions: StreamOptions? = null,
    public val suffix: String? = null,
    temperature: Double? = null,
    topP: Double? = null,
) : DeepSeekParams(frequencyPenalty, maxTokens, presencePenalty, stop, temperature, topP) {
    /**
     * Builder for creating [FIMCompletionParams] with standard (non-streaming) configuration.
     */
    public class Builder {
        public var model: ChatModel = ChatModel.DEEPSEEK_V4_PRO
        public var echo: Boolean? = null

        @Deprecated(DEPRECATED_PENALTY)
        public var frequencyPenalty: Double? = null
        public var logprobs: Int? = null
        public var maxTokens: Int? = null

        @Deprecated(DEPRECATED_PENALTY)
        public var presencePenalty: Double? = null
        public var stop: StopReason? = null
        public var suffix: String? = null
        public var temperature: Double? = null
        public var topP: Double? = null

        internal fun build(): FIMCompletionParams {
            requireFimModel(model)
            maxTokens?.let { require(it >= 1) { "maxTokens must be >= 1" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            topP?.let { require(it in 0.0..1.0) { "topP must be between 0.0 and 1.0" } }
            logprobs?.let { require(it <= 20) { "logprobs must be <= 20" } }

            return FIMCompletionParams(
                model = model,
                echo = echo,
                frequencyPenalty = frequencyPenalty,
                logprobs = logprobs,
                maxTokens = maxTokens,
                presencePenalty = presencePenalty,
                stop = stop,
                suffix = suffix,
                temperature = temperature,
                topP = topP,
            )
        }
    }

    /**
     * Builder for creating [FIMCompletionParams] specifically configured for streaming responses.
     */
    public class StreamBuilder {
        public var model: ChatModel = ChatModel.DEEPSEEK_V4_PRO
        public var echo: Boolean? = null

        @Deprecated(DEPRECATED_PENALTY)
        public var frequencyPenalty: Double? = null
        public var logprobs: Int? = null
        public var maxTokens: Int? = null

        @Deprecated(DEPRECATED_PENALTY)
        public var presencePenalty: Double? = null
        public var stop: StopReason? = null
        public var streamOptions: StreamOptions? = null
        public var suffix: String? = null
        public var temperature: Double? = null
        public var topP: Double? = null

        internal fun build(): FIMCompletionParams {
            requireFimModel(model)
            maxTokens?.let { require(it >= 1) { "maxTokens must be >= 1" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            topP?.let { require(it in 0.0..1.0) { "topP must be between 0.0 and 1.0" } }
            logprobs?.let { require(it <= 20) { "logprobs must be <= 20" } }

            return FIMCompletionParams(
                model = model,
                echo = echo,
                frequencyPenalty = frequencyPenalty,
                logprobs = logprobs,
                maxTokens = maxTokens,
                presencePenalty = presencePenalty,
                stop = stop,
                stream = true,
                streamOptions = streamOptions,
                suffix = suffix,
                temperature = temperature,
                topP = topP,
            )
        }
    }

    /**
     * Creates a [FIMCompletionRequest] from these parameters and the provided prompt.
     *
     * @param prompt The text to start the completion from (the beginning part in FIM)
     * @return A fully configured [FIMCompletionRequest]
     */
    public fun createRequest(prompt: String): FIMCompletionRequest =
        FIMCompletionRequest(
            model = model,
            prompt = prompt,
            echo = echo,
            frequencyPenalty = frequencyPenalty,
            logprobs = logprobs,
            maxTokens = maxTokens,
            presencePenalty = presencePenalty,
            stop = stop,
            stream = stream,
            streamOptions = streamOptions,
            suffix = suffix,
            temperature = temperature,
            topP = topP,
        )


    /**
     * Creates a copy of these parameters with optional changes to specific properties.
     *
     * This is useful when you want to reuse a base configuration with slight modifications,
     * such as changing from standard to streaming mode.
     *
     * Example:
     * ```kotlin
     * // Create a streaming version of existing parameters
     * val streamParams = regularParams.copy(stream = true)
     * ```
     *
     * @param model New model to use, or existing value if not specified
     * @param echo New echo setting, or existing value if not specified
     * @param frequencyPenalty New frequency penalty value, or existing value if not specified
     * @param logprobs New log probabilities count, or existing value if not specified
     * @param maxTokens New maximum token count, or existing value if not specified
     * @param presencePenalty New presence penalty value, or existing value if not specified
     * @param stop New stop reason, or existing value if not specified
     * @param stream New streaming setting, or existing value if not specified
     * @param streamOptions New stream options, or existing value if not specified
     * @param suffix New suffix text, or existing value if not specified
     * @param temperature New temperature value, or existing value if not specified
     * @param topP New top-p value, or existing value if not specified
     * @return A new [FIMCompletionParams] instance with the specified changes
     */
    public fun copy(
        model: ChatModel = this.model,
        echo: Boolean? = this.echo,
        frequencyPenalty: Double? = this.frequencyPenalty,
        logprobs: Int? = this.logprobs,
        maxTokens: Int? = this.maxTokens,
        presencePenalty: Double? = this.presencePenalty,
        stop: StopReason? = this.stop,
        stream: Boolean? = this.stream,
        streamOptions: StreamOptions? = this.streamOptions,
        suffix: String? = this.suffix,
        temperature: Double? = this.temperature,
        topP: Double? = this.topP,
    ): FIMCompletionParams {
        return FIMCompletionParams(
            model = model,
            echo = echo,
            frequencyPenalty = frequencyPenalty,
            logprobs = logprobs,
            maxTokens = maxTokens,
            presencePenalty = presencePenalty,
            stop = stop,
            stream = stream,
            streamOptions = streamOptions,
            suffix = suffix,
            temperature = temperature,
            topP = topP,
        )
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FIMCompletionParams) return false

        return model == other.model &&
                echo == other.echo &&
                frequencyPenalty == other.frequencyPenalty &&
                logprobs == other.logprobs &&
                maxTokens == other.maxTokens &&
                presencePenalty == other.presencePenalty &&
                stop == other.stop &&
                stream == other.stream &&
                streamOptions == other.streamOptions &&
                suffix == other.suffix &&
                temperature == other.temperature &&
                topP == other.topP
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        result = 31 * result + (echo?.hashCode() ?: 0)
        result = 31 * result + (frequencyPenalty?.hashCode() ?: 0)
        result = 31 * result + (logprobs?.hashCode() ?: 0)
        result = 31 * result + (maxTokens?.hashCode() ?: 0)
        result = 31 * result + (presencePenalty?.hashCode() ?: 0)
        result = 31 * result + (stop?.hashCode() ?: 0)
        result = 31 * result + (stream?.hashCode() ?: 0)
        result = 31 * result + (streamOptions?.hashCode() ?: 0)
        result = 31 * result + (suffix?.hashCode() ?: 0)
        result = 31 * result + (temperature?.hashCode() ?: 0)
        result = 31 * result + (topP?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FIMCompletionParams(model=$model, echo=$echo, frequencyPenalty=$frequencyPenalty, logprobs=$logprobs, maxTokens=$maxTokens, presencePenalty=$presencePenalty, stop=$stop, stream=$stream, streamOptions=$streamOptions, suffix=$suffix, temperature=$temperature, topP=$topP)"
}