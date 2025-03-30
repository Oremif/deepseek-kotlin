package org.oremif.deepseek.models

/**
 * Base class for DeepSeek API parameters.
 *
 * This class provides common parameters used across different DeepSeek API endpoints,
 * serving as a foundation for more specific parameter classes like [ChatCompletionParams]
 * and [FIMCompletionParams].
 *
 *
 * @property frequencyPenalty Reduces repetition by penalizing tokens based on their frequency in the text
 * @property maxTokens Maximum number of tokens to generate in the response
 * @property presencePenalty Reduces repetition by penalizing tokens that have appeared in the text
 * @property stop Custom stop sequence that causes the model to stop generating further tokens
 * @property temperature Controls randomness in output generation (higher = more random)
 * @property topP Controls diversity via nucleus sampling (consider only tokens with top_p probability mass)
 */
public open class DeepSeekParams internal constructor(
    public val frequencyPenalty: Double? = null,
    public val maxTokens: Int? = null,
    public val presencePenalty: Double? = null,
    public val stop: StopReason? = null,
    public val temperature: Double? = null,
    public val topP: Double? = null,
) {

    /**
     * Creates chat completion parameters with custom settings.
     *
     * This function provides a convenient way to create parameters specifically
     * for chat completion requests using a builder pattern.
     *
     * Example:
     * ```kotlin
     * params {
     *     chat {
     *         model = ChatModel.DEEPSEEK_CHAT
     *         temperature = 0.8
     *         maxTokens = 2000
     *    }
     * }
     * ```
     *
     * @param block Configuration block for building chat parameters
     * @return Configured [ChatCompletionParams] for use with chat endpoints
     */
    public fun chat(block: ChatCompletionParams.Builder.() -> Unit): ChatCompletionParams =
        chatCompletionParams(block)

    /**
     * Creates chat completion stream parameters with custom settings.
     *
     * This function provides a convenient way to create parameters specifically
     * for chat completion requests using a builder pattern.
     *
     * Example:
     * ```kotlin
     * params {
     *     chat {
     *         model = ChatModel.DEEPSEEK_CHAT
     *         temperature = 0.8
     *         maxTokens = 2000
     *    }
     * }
     * ```
     *
     * @param block Configuration block for building chat parameters
     * @return Configured [ChatCompletionParams] for use with chat endpoints
     */
    public fun chatStream(block: ChatCompletionParams.StreamBuilder.() -> Unit): ChatCompletionParams =
        chatCompletionStreamParams(block)

    /**
     * Creates file-in-the-middle (FIM) completion parameters with custom settings.
     *
     * This function provides a convenient way to create parameters specifically
     * for FIM completion requests using a builder pattern.
     *
     * Example:
     * ```kotlin
     * params {
     *     fim {
     *         prefix = "def calculate_total("
     *         suffix = "    return result"
     *         temperature = 0.5
     *     }
     * }
     * ```
     *
     * @param block Configuration block for building FIM parameters
     * @return Configured [FIMCompletionParams] for use with FIM endpoints
     */
    public fun fim(block: FIMCompletionParams.Builder.() -> Unit): FIMCompletionParams = fimCompletionParams(block)

    /**
     * Creates file-in-the-middle (FIM) completion stream parameters with custom settings.
     *
     * This function provides a convenient way to create parameters specifically
     * for FIM completion requests using a builder pattern.
     *
     * Example:
     * ```kotlin
     * params {
     *     fim {
     *         prefix = "def calculate_total("
     *         suffix = "    return result"
     *         temperature = 0.5
     *     }
     * }
     * ```
     *
     * @param block Configuration block for building FIM parameters
     * @return Configured [FIMCompletionParams] for use with FIM endpoints
     */
    public fun fimStream(block: FIMCompletionParams.StreamBuilder.() -> Unit): FIMCompletionParams =
        fimCompletionStreamParams(block)
}