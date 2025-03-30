package org.oremif.deepseek.client

import kotlinx.serialization.json.Json
import org.oremif.deepseek.models.DeepSeekParams

/**
 * Configuration settings for DeepSeek API clients.
 *
 * This class encapsulates configuration options used by both standard and streaming
 * DeepSeek clients, including model parameters, serialization settings, and request timeouts.
 *
 * @property params Optional model parameters that define default behavior for API requests
 * @property jsonConfig JSON serialization configuration for API requests and responses
 * @property chatCompletionTimeout Timeout in milliseconds for chat completion requests
 * @property fimCompletionTimeout Timeout in milliseconds for file-in-the-middle completion requests
 */
public class DeepSeekClientConfig(
    public val params: DeepSeekParams? = null,
    public val jsonConfig: Json = Json,
    public val chatCompletionTimeout: Long = 45_000,
    public val fimCompletionTimeout: Long = 60_000,
)