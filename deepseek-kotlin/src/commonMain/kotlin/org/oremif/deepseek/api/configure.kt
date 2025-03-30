package org.oremif.deepseek.api

import org.oremif.deepseek.client.DeepSeekClient
import org.oremif.deepseek.client.DeepSeekClientConfig
import org.oremif.deepseek.models.DeepSeekParams

/**
 * Creates a new client instance with updated parameters.
 *
 * This function allows you to create a new client with modified parameters while
 * preserving all other configuration settings from the original client.
 *
 * Example:
 * ```kotlin
 * // Original client
 * val client = DeepSeekClient("your-api-token")
 *
 * // Create a new client with modified parameters
 * val customParams = ChatCompletionParams(ChatModel.DEEPSEEK_CHAT)
 * val configuredClient = client.configure(customParams)
 * ```
 *
 * @param params The new parameters to use for this client instance
 * @return A new [DeepSeekClient] with updated parameters
 */
public fun DeepSeekClient.configure(params: DeepSeekParams): DeepSeekClient =
    DeepSeekClient(
        client = this.client,
        config = DeepSeekClientConfig(
            params = params,
            jsonConfig = this.config.jsonConfig,
            chatCompletionTimeout = this.config.chatCompletionTimeout,
            fimCompletionTimeout = this.config.fimCompletionTimeout,
        )
    )

/**
 * Creates a new client instance with parameters modified via a builder block.
 *
 * This function provides a convenient way to create a new client with customized
 * parameters using a Kotlin DSL builder pattern.
 *
 * Example:
 * ```kotlin
 * // Create a new client with custom parameters
 * val configuredClient = client.configure {
 *     chat {
 *         temperature = 0.8
 *         maxTokens = 2000
 *         topP = 0.95
 *     }
 * }
 * ```
 *
 * @param block A builder block for modifying parameters
 * @return A new [DeepSeekClient] with the updated parameters
 */
public fun DeepSeekClient.configure(block: DeepSeekParams.() -> DeepSeekParams): DeepSeekClient =
    this.configure(DeepSeekParams().block())