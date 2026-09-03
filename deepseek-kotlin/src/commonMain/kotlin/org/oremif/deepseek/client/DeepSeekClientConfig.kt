package org.oremif.deepseek.client

import kotlinx.serialization.json.Json

/**
 * Configuration settings for DeepSeek API clients.
 *
 * This class encapsulates configuration options used by both standard and streaming
 * DeepSeek clients, including serialization settings and request timeouts.
 *
 * @property jsonConfig JSON serialization configuration for API requests and responses
 * @property chatCompletionTimeout Timeout in milliseconds for chat completion requests;
 * defaults to 5 minutes, since a thinking model can spend minutes on a single
 * non-streaming call
 * @property fimCompletionTimeout Timeout in milliseconds for file-in-the-middle completion requests
 * @property uploadTimeout Timeout in milliseconds for Files API uploads; defaults to
 * 5 minutes, since the API accepts files of up to 64 MiB and a slow uplink needs far
 * longer than a chat call to push one
 */
public class DeepSeekClientConfig(
    public val jsonConfig: Json = Json,
    public val chatCompletionTimeout: Long = 300_000,
    public val fimCompletionTimeout: Long = 60_000,
    public val uploadTimeout: Long = 300_000,
)
