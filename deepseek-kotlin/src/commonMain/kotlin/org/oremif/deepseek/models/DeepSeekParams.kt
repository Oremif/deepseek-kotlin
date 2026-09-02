package org.oremif.deepseek.models

/** Deprecation message shared by the `frequency_penalty` / `presence_penalty` declarations. */
internal const val DEPRECATED_PENALTY: String =
    "The DeepSeek API ignores this parameter: it is still sent, but has no effect on the response."

/**
 * Shared sampling parameters for the DeepSeek chat and FIM endpoints.
 *
 * This class carries the fields common to both endpoints and acts as the base of
 * [ChatCompletionParams] and [FIMCompletionParams]. It also exposes shortcut factory
 * methods ([chat], [chatStream], [fim], [fimStream]) so any existing param instance can
 * be used as an entry point to produce a differently-typed one.
 *
 * @property frequencyPenalty Sent as `frequency_penalty`, ignored by the API.
 * @property maxTokens Maximum number of tokens to generate in the response. At least `1`,
 * and otherwise bounded by the model's context length.
 * @property presencePenalty Sent as `presence_penalty`, ignored by the API.
 * @property stop Custom stop sequences that cause the model to stop generating further tokens.
 * @property temperature Controls randomness in output generation (higher = more random).
 * Expected range: `0.0..2.0`.
 * @property topP Controls diversity via nucleus sampling (consider only tokens with top_p probability mass).
 * Expected range: `0.0..1.0`.
 */
public open class DeepSeekParams internal constructor(
    @Deprecated(DEPRECATED_PENALTY)
    public val frequencyPenalty: Double? = null,
    public val maxTokens: Int? = null,
    @Deprecated(DEPRECATED_PENALTY)
    public val presencePenalty: Double? = null,
    public val stop: StopReason? = null,
    public val temperature: Double? = null,
    public val topP: Double? = null,
) {

    /**
     * Shortcut to [chatCompletionParams], available on any existing [DeepSeekParams].
     *
     * Example:
     * ```kotlin
     * val params = existingParams.chat {
     *     model = ChatModel.DEEPSEEK_V4_FLASH
     *     temperature = 0.8
     *     maxTokens = 2000
     * }
     * ```
     *
     * @param block Configuration block for building chat parameters
     * @return Configured [ChatCompletionParams] for use with chat endpoints
     */
    public fun chat(block: ChatCompletionParams.Builder.() -> Unit): ChatCompletionParams =
        chatCompletionParams(block)

    /**
     * Shortcut to [chatCompletionStreamParams], available on any existing [DeepSeekParams].
     *
     * Example:
     * ```kotlin
     * val streamParams = existingParams.chatStream {
     *     model = ChatModel.DEEPSEEK_V4_PRO
     *     temperature = 0.8
     * }
     * ```
     *
     * @param block Configuration block for building streaming chat parameters
     * @return Configured [ChatCompletionParams] with `stream = true`
     */
    public fun chatStream(block: ChatCompletionParams.StreamBuilder.() -> Unit): ChatCompletionParams =
        chatCompletionStreamParams(block)

    /**
     * Shortcut to [fimCompletionParams], available on any existing [DeepSeekParams].
     *
     * Example:
     * ```kotlin
     * val fimParams = existingParams.fim {
     *     suffix = "    return result"
     *     temperature = 0.5
     * }
     * ```
     *
     * @param block Configuration block for building FIM parameters
     * @return Configured [FIMCompletionParams] for use with FIM endpoints
     */
    public fun fim(block: FIMCompletionParams.Builder.() -> Unit): FIMCompletionParams = fimCompletionParams(block)

    /**
     * Shortcut to [fimCompletionStreamParams], available on any existing [DeepSeekParams].
     *
     * Example:
     * ```kotlin
     * val streamFimParams = existingParams.fimStream {
     *     suffix = "    return result"
     *     temperature = 0.5
     * }
     * ```
     *
     * @param block Configuration block for building streaming FIM parameters
     * @return Configured [FIMCompletionParams] with `stream = true`
     */
    public fun fimStream(block: FIMCompletionParams.StreamBuilder.() -> Unit): FIMCompletionParams =
        fimCompletionStreamParams(block)
}