package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Represents the FIM (Fill-In-the-Middle) Completion request
 *
 * @property model ID of the model to use.
 *
 * **Possible values: {`deepseek-chat`}**
 * @property prompt The prompt to generate completions for.
 *
 * **Default value: `Once upon a time, `**
 * @property echo Echo back the prompt in addition to the completion
 * @property frequencyPenalty  Number between -2.0 and 2.0.
 * Positive values penalize new tokens based on their existing frequency in the text so far,
 * decreasing the model's likelihood to repeat the same line verbatim.
 *
 * **Possible values: `>= -2` and `<= 2`.**
 *
 * **Default value: `0`.**
 * @property logprobs Include the log probabilities on the `logprobs` most likely output tokens,
 * as well the chosen tokens.
 * For example, if `logprobs` is 20, the API will return a list of the 20 most likely tokens.
 * The API will always return the `logprob` of the sampled token,
 * so there may be up to `logprobs+1` elements in the response.
 *
 * The maximum value for `logprobs` is 20.
 *
 * **Possible values: `<= 20`.**
 * @property maxTokens The maximum number of tokens that can be generated in the completion.
 * @property presencePenalty Number between -2.0 and 2.0.
 * Positive values penalize new tokens based on whether they appear in the text so far,
 * increasing the model's likelihood to talk about new topics.
 *
 * **Possible values: `>= -2` and `<= 2`.**
 *
 * **Default value: `0`.**
 * @property stop Up to 16 sequences where the API will stop generating further tokens.
 * @property stream Whether to stream back partial progress.
 * If set, tokens will be sent as data-only server-sent events as they become available,
 * with the stream terminated by a data: [DONE] message.
 * @property streamOptions Options for streaming response. Only set this when you set `stream: true`.
 * @property suffix The suffix that comes after a completion of inserted text.
 * @property temperature What sampling temperature to use, between 0 and 2.
 * Higher values like 0.8 will make the output more random,
 * while lower values like 0.2 will make it more focused and deterministic.
 *
 * We generally recommend altering this or `top_p` but not both.
 *
 * **Possible values: `<= 2`.**
 *
 * **Default value: `1`.**
 * @property topP An alternative to sampling with temperature,
 * called nucleus sampling, where the model considers the results of the tokens with top_p probability mass.
 * So 0.1 means only the tokens comprising the top 10% probability mass are considered.
 *
 * We generally recommend altering this or `temperature` but not both.
 * **Possible values: `<= 1`.**
 *
 * **Default value: `1`.**
 */
@Serializable
public class FIMCompletionRequest(
    public val model: ChatModel,
    public val prompt: String,
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
        private var prompt: String? = null
        private var params: FIMCompletionParams = FIMCompletionParams()

        public fun prompt(str: String) {
            prompt = str
        }

        public fun params(block: FIMCompletionParams.Builder.() -> Unit) {
            params = FIMCompletionParams.Builder().apply(block).build()
        }

        public fun build(): FIMCompletionRequest =
            params.createRequest(prompt ?: "Once upon a time, ")
    }

    public class StreamBuilder {
        private var prompt: String? = null
        private var params: FIMCompletionParams = FIMCompletionParams()

        public fun prompt(str: String) {
            prompt = str
        }

        public fun params(block: FIMCompletionParams.StreamBuilder.() -> Unit) {
            params = FIMCompletionParams.StreamBuilder().apply(block).build()
        }

        public fun build(): FIMCompletionRequest =
            params.createRequest(prompt ?: "Once upon a time, ")
    }

    public class MessageBuilder {
        private val messages = mutableListOf<ChatMessage>()

        public fun system(content: String) {
            messages.add(SystemMessage(content))
        }

        public fun user(content: String) {
            messages.add(UserMessage(content))
        }

        public fun assistant(content: String) {
            messages.add(AssistantMessage(content))
        }

        public fun tool(content: String, toolCallId: String) {
            messages.add(ToolMessage(content, toolCallId))
        }

        public fun build(): List<ChatMessage> = messages.toList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FIMCompletionRequest) return false

        return model == other.model &&
                prompt == other.prompt &&
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
        result = 31 * result + prompt.hashCode()
        result = 31 * result + (echo?.hashCode() ?: 0)
        result = 31 * result + (frequencyPenalty?.hashCode() ?: 0)
        result = 31 * result + (logprobs ?: 0)
        result = 31 * result + (maxTokens ?: 0)
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
        "FIMCompletionRequest(model=$model, prompt='$prompt', echo=$echo, frequencyPenalty=$frequencyPenalty, logprobs=$logprobs, maxTokens=$maxTokens, presencePenalty=$presencePenalty, stop=$stop, stream=$stream, streamOptions=$streamOptions, suffix=$suffix, temperature=$temperature, topP=$topP)"
}