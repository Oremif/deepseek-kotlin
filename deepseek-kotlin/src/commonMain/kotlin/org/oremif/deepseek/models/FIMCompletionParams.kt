package org.oremif.deepseek.models

public fun fimCompletionParams(block: FIMCompletionParams.Builder.() -> Unit): FIMCompletionParams {
    return FIMCompletionParams.Builder().apply(block).build()
}

public fun fimCompletionStreamParams(block: FIMCompletionParams.StreamBuilder.() -> Unit): FIMCompletionParams {
    return FIMCompletionParams.StreamBuilder().apply(block).build()
}

public class FIMCompletionParams internal constructor(
    public val echo: Boolean? = null,
    public val frequencyPenalty: Double? = null,
    public val logprobs: Int? = null,
    public val maxTokens: Int? = null,
    public val presencePenalty: Double? = null,
    public val stop: StopReason? = null,
    public val stream: Boolean? = null,
    public val streamOptions: StreamOptions? = null,
    public val suffix: String? = null,
    public val temperature: Double? = null,
    public val topP: Double? = null,
) {
    public class Builder {
        public var echo: Boolean? = null
        public var frequencyPenalty: Double? = null
        public var logprobs: Int? = null
        public var maxTokens: Int? = null
        public var presencePenalty: Double? = null
        public var stop: StopReason? = null
        public var suffix: String? = null
        public var temperature: Double? = null
        public var topP: Double? = null

        internal fun build(): FIMCompletionParams {
            frequencyPenalty?.let { require(it in -2.0..2.0) { "frequencyPenalty must be between -2.0 and 2.0" } }
            maxTokens?.let { require(it in 1..8192) { "maxTokens must be between 1 and 8192" } }
            presencePenalty?.let { require(it in -2.0..2.0) { "presencePenalty must be between -2.0 and 2.0" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            logprobs?.let { require(it <= 20) { "logprobs must be <= 20" } }

            return FIMCompletionParams(
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

    public class StreamBuilder {
        public var echo: Boolean? = null
        public var frequencyPenalty: Double? = null
        public var logprobs: Int? = null
        public var maxTokens: Int? = null
        public var presencePenalty: Double? = null
        public var stop: StopReason? = null
        public var streamOptions: StreamOptions? = null
        public var suffix: String? = null
        public var temperature: Double? = null
        public var topP: Double? = null

        internal fun build(): FIMCompletionParams {
            frequencyPenalty?.let { require(it in -2.0..2.0) { "frequencyPenalty must be between -2.0 and 2.0" } }
            maxTokens?.let { require(it in 1..8192) { "maxTokens must be between 1 and 8192" } }
            presencePenalty?.let { require(it in -2.0..2.0) { "presencePenalty must be between -2.0 and 2.0" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            logprobs?.let { require(it <= 20) { "logprobs must be <= 20" } }

            return FIMCompletionParams(
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

    public fun createRequest(prompt: String): FIMCompletionRequest =
        FIMCompletionRequest(
            model = ChatModel.DEEPSEEK_CHAT,
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


    public fun copy(
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

        return echo == other.echo &&
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
        var result = echo?.hashCode() ?: 0
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
        "FIMCompletionParams(echo=$echo, frequencyPenalty=$frequencyPenalty, logprobs=$logprobs, maxTokens=$maxTokens, presencePenalty=$presencePenalty, stop=$stop, stream=$stream, streamOptions=$streamOptions, suffix=$suffix, temperature=$temperature, topP=$topP)"
}