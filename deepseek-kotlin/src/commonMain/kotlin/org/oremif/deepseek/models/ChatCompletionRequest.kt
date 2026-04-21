package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Represents the Chat Completion request
 *
 * @property messages A list of messages comprising the conversation so far.
 * @property model ID of the model to use. You can use deepseek-chat.
 *
 * **Possible values: {`deepseek-chat`, `deepseek-reasoner`}.**
 * @property frequencyPenalty  Number between -2.0 and 2.0.
 * Positive values penalize new tokens based on their existing frequency in the text so far,
 * decreasing the model's likelihood to repeat the same line verbatim.
 *
 * **Possible values: `>= -2` and `<= 2`.**
 *
 * **Default value: `0`.**
 * @property maxTokens Integer between 1 and 8192.
 * The maximum number of tokens that can be generated in the chat completion.
 *
 * The total length of input tokens and generated tokens is limited by the model's context length.
 *
 * If `max_tokens` is not specified, the default value 4096 is used.
 *
 * **Possible values: `> 1`.**
 * @property presencePenalty Number between -2.0 and 2.0.
 * Positive values penalize new tokens based on whether they appear in the text so far,
 * increasing the model's likelihood to talk about new topics.
 *
 * **Possible values: `>= -2` and `<= 2`.**
 *
 * **Default value: `0`.**
 * @property responseFormat An object specifying the format that the model must output.
 * Setting to { "type": "json_object" } enables JSON Output,
 * which guarantees the message the model generates is valid JSON.
 * @property stop Up to 16 sequences where the API will stop generating further tokens.
 * @property stream If set, partial message deltas will be sent.
 * Tokens will be sent as data-only server-sent events (SSE) as they become available,
 * with the stream terminated by a `data: [DONE]`
 * @property streamOptions Options for streaming response. Only set this when you set `stream: true`.
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
 * @property tools A list of tools the model may call.
 * Currently, only functions are supported as a tool.
 * Use this to provide a list of functions the model may generate JSON inputs for.
 * A max of 128 functions are supported.
 * @property toolChoice Controls which (if any) tool is called by the model.
 *
 * - `none` means the model will not call any tool and instead generates a message.
 *
 * - `auto` means the model can pick between generating a message or calling one or more tools.
 *
 * - `required` means the model must call one or more tools.
 *
 * Specifying a particular tool via
 * `{"type": "function", "function": {"name": "my_function"}}` forces the model to call that tool.
 *
 * `none` is the default when no tools are present. `auto` is the default if tools are present.
 * @property logprobs Whether to return log probabilities of the output tokens or not.
 * If true, returns the log probabilities of each output token returned in the `content` of `message`.
 * @property topLogprobs An integer between 0 and 20 specifying the number of most likely tokens to return at each token
 * position, each with an associated log probability. `logprobs` must be set to `true` if this parameter is used.
 *
 * **Possible values: `<= 20`.**
 * @property thinking Toggles the reasoning pass of the `deepseek-reasoner` model. See
 * [Thinking] for the server-side behaviour, in particular the model-slug rewrite that
 * happens when [ThinkingType.DISABLED] is sent with `model = deepseek-reasoner`.
 */
@Serializable
public class ChatCompletionRequest(
    public val messages: List<ChatMessage>,
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
    public val thinking: Thinking? = null,
) {

    /**
     * Builder for non-streaming chat completion requests.
     *
     * Used by `DeepSeekClient.chatCompletion { ... }`; compose [messages] and [params]
     * inside the block.
     */
    public class Builder {
        private var messages = mutableListOf<ChatMessage>()
        private var params: ChatCompletionParams = ChatCompletionParams(
            model = ChatModel.DEEPSEEK_CHAT,
        )

        /**
         * Builds the conversation history via a [MessageBuilder] DSL.
         *
         * @param block Builder block that appends messages using [MessageBuilder.system],
         * [MessageBuilder.user], etc.
         */
        public fun messages(block: MessageBuilder.() -> Unit) {
            messages.addAll(MessageBuilder().apply(block).build())
        }

        /**
         * Configures request parameters (model, temperature, tools, etc.).
         *
         * Replaces any previously-configured parameters.
         *
         * @param block Builder block applied to a fresh [ChatCompletionParams.Builder]
         */
        public fun params(block: ChatCompletionParams.Builder.() -> Unit) {
            params = ChatCompletionParams.Builder().apply(block).build()
        }

        internal fun build(): ChatCompletionRequest =
            params.createRequest(messages)
    }

    /**
     * Builder for streaming chat completion requests.
     *
     * Used by `DeepSeekClientStream.chatCompletion { ... }`; compose [messages] and
     * [params] inside the block. The resulting request has `stream = true`.
     */
    public class StreamBuilder {
        private var messages = mutableListOf<ChatMessage>()
        private var params: ChatCompletionParams = ChatCompletionParams(
            model = ChatModel.DEEPSEEK_CHAT,
        )

        /**
         * Builds the conversation history via a [MessageBuilder] DSL.
         *
         * @param block Builder block that appends messages using [MessageBuilder.system],
         * [MessageBuilder.user], etc.
         */
        public fun messages(block: MessageBuilder.() -> Unit) {
            messages.addAll(MessageBuilder().apply(block).build())
        }

        /**
         * Configures streaming request parameters.
         *
         * Replaces any previously-configured parameters.
         *
         * @param block Builder block applied to a fresh [ChatCompletionParams.StreamBuilder]
         */
        public fun params(block: ChatCompletionParams.StreamBuilder.() -> Unit) {
            params = ChatCompletionParams.StreamBuilder().apply(block).build()
        }

        internal fun build(): ChatCompletionRequest =
            params.createRequest(messages)
    }

    /**
     * DSL for building a conversation history.
     *
     * Appends messages to an internal list in call order. Each helper adds the matching
     * concrete [ChatMessage] subtype.
     *
     * Example:
     * ```kotlin
     * client.chat {
     *     system("You are a Kotlin expert")
     *     user("What are coroutines?")
     * }
     * ```
     */
    public class MessageBuilder {
        private val messages = mutableListOf<ChatMessage>()

        /**
         * Appends a [SystemMessage] with the given [content].
         *
         * @param content System instructions for the assistant
         */
        public fun system(content: String) {
            messages.add(SystemMessage(content))
        }

        /**
         * Appends a [UserMessage] with the given [content].
         *
         * @param content User's text input
         */
        public fun user(content: String) {
            messages.add(UserMessage(content))
        }

        /**
         * Appends an [AssistantMessage] with the given [content].
         *
         * @param content Assistant text to replay as context
         */
        public fun assistant(content: String) {
            messages.add(AssistantMessage(content))
        }

        /**
         * Appends a [ToolMessage] carrying the result of executing a prior [ToolCall].
         *
         * @param content Tool execution result (typically JSON-encoded)
         * @param toolCallId Identifier of the [ToolCall] this message responds to
         */
        public fun tool(content: String, toolCallId: String) {
            messages.add(ToolMessage(content, toolCallId))
        }

        internal fun build(): List<ChatMessage> = messages.toList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatCompletionRequest) return false

        return messages == other.messages &&
                model == other.model &&
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
                topLogprobs == other.topLogprobs &&
                thinking == other.thinking
    }

    override fun hashCode(): Int {
        var result = messages.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + (frequencyPenalty?.hashCode() ?: 0)
        result = 31 * result + (maxTokens?.hashCode() ?: 0)
        result = 31 * result + (presencePenalty?.hashCode() ?: 0)
        result = 31 * result + (responseFormat?.hashCode() ?: 0)
        result = 31 * result + (stop?.hashCode() ?: 0)
        result = 31 * result + (stream?.hashCode() ?: 0)
        result = 31 * result + (streamOptions?.hashCode() ?: 0)
        result = 31 * result + (temperature?.hashCode() ?: 0)
        result = 31 * result + (topP?.hashCode() ?: 0)
        result = 31 * result + (tools?.hashCode() ?: 0)
        result = 31 * result + (toolChoice?.hashCode() ?: 0)
        result = 31 * result + (logprobs?.hashCode() ?: 0)
        result = 31 * result + (topLogprobs?.hashCode() ?: 0)
        result = 31 * result + (thinking?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChatCompletionRequest(messages=$messages, model=$model, frequencyPenalty=$frequencyPenalty, maxTokens=$maxTokens, presencePenalty=$presencePenalty, responseFormat=$responseFormat, stop=$stop, stream=$stream, streamOptions=$streamOptions, temperature=$temperature, topP=$topP, tools=$tools, toolChoice=$toolChoice, logprobs=$logprobs, topLogprobs=$topLogprobs, thinking=$thinking)"
}